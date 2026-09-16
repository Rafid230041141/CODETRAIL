package application.client.dsa.judge;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class OnlineJudgeService {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "DSA-Judge-Worker");
        t.setDaemon(true);
        return t;
    });

    private static final String CLANG_PATH = findBinary("clang", List.of("/usr/bin/clang", "/usr/local/bin/clang"));
    private static final String CLANGPP_PATH = findBinary("clang++", List.of("/usr/bin/clang++", "/usr/local/bin/clang++", "/usr/bin/g++"));
    private static final String PYTHON_PATH = findBinary("python3", List.of("/opt/homebrew/bin/python3", "/usr/local/bin/python3", "/usr/bin/python3"));
    private static final String JAVAC_PATH = findBinary("javac", List.of(
            "/opt/homebrew/Cellar/openjdk@21/21.0.11/libexec/openjdk.jdk/Contents/Home/bin/javac",
            "/opt/homebrew/bin/javac",
            "/usr/bin/javac"
    ));
    private static final String JAVA_PATH = findBinary("java", List.of(
            "/opt/homebrew/Cellar/openjdk@21/21.0.11/libexec/openjdk.jdk/Contents/Home/bin/java",
            "/opt/homebrew/bin/java",
            "/usr/bin/java"
    ));
    private static final String NODE_PATH = findBinary("node", List.of("/usr/local/bin/node", "/opt/homebrew/bin/node", "/usr/bin/node"));
    private static final String DOTNET_PATH = findBinary("dotnet", List.of("/usr/local/share/dotnet/dotnet", "/opt/homebrew/bin/dotnet", "/usr/bin/dotnet"));

    private static String findBinary(String name, List<String> fallbackPaths) {
        for (String path : fallbackPaths) {
            File f = new File(path);
            if (f.exists() && f.canExecute()) {
                return path;
            }
        }
        return name;
    }

    private static void runOnFxThread(Runnable r) {
        try {
            Class<?> platformClass = Class.forName("javafx.application.Platform");
            java.lang.reflect.Method runLater = platformClass.getMethod("runLater", Runnable.class);
            runLater.invoke(null, r);
        } catch (Throwable t) {
            // Not in JavaFX environment or toolkit not running
            try {
                r.run();
            } catch (Throwable ignored) {}
        }
    }

    public static void submitSolutionAsync(
            DsaProblem problem,
            ProgrammingLanguage language,
            String sourceCode,
            Consumer<JudgeResult> onResult,
            Consumer<String> onProgress
    ) {
        EXECUTOR.submit(() -> {
            try {
                if (onProgress != null) {
                    runOnFxThread(() -> onProgress.accept("Preparing sandbox environment..."));
                }
                JudgeResult result = judgeSolution(problem, language, sourceCode, onProgress);
                if (onResult != null) {
                    runOnFxThread(() -> onResult.accept(result));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JudgeResult err = JudgeResult.internalError("Judge execution exception: " + ex.getMessage());
                if (onResult != null) {
                    runOnFxThread(() -> onResult.accept(err));
                }
            }
        });
    }

    public static void runCustomTestAsync(
            ProgrammingLanguage language,
            String sourceCode,
            String customInput,
            int timeLimitMs,
            Consumer<TestCaseResult> onResult
    ) {
        runCustomTestAsync(language, sourceCode, customInput, "", timeLimitMs, onResult);
    }

    public static void runCustomTestAsync(
            ProgrammingLanguage language,
            String sourceCode,
            String customInput,
            String expectedOutput,
            int timeLimitMs,
            Consumer<TestCaseResult> onResult
    ) {
        EXECUTOR.submit(() -> {
            try {
                TestCaseResult result = runSingleTest(language, sourceCode, customInput, expectedOutput != null ? expectedOutput : "", timeLimitMs, 1, true);
                if (onResult != null) {
                    runOnFxThread(() -> onResult.accept(result));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                TestCaseResult err = new TestCaseResult(
                        1,
                        SubmissionVerdict.INTERNAL_ERROR,
                        0,
                        customInput,
                        expectedOutput != null ? expectedOutput : "",
                        "",
                        "Error running test: " + ex.getMessage(),
                        true
                );
                if (onResult != null) {
                    runOnFxThread(() -> onResult.accept(err));
                }
            }
        });
    }

    private static JudgeResult judgeSolution(
            DsaProblem problem,
            ProgrammingLanguage language,
            String sourceCode,
            Consumer<String> onProgress
    ) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("codetrail_judge_");
            File sourceFile = new File(tempDir.toFile(), language.defaultFileName());
            Files.writeString(sourceFile.toPath(), sourceCode, StandardCharsets.UTF_8);

            // 1. Compilation Phase
            if (onProgress != null) {
                runOnFxThread(() -> onProgress.accept("Compiling solution (" + language.displayName() + ")..."));
            }

            CompileResult compileResult = compileSource(language, tempDir.toFile(), sourceFile);
            if (!compileResult.success) {
                return JudgeResult.compilationError(compileResult.errorLog);
            }

            // 2. Execution & Evaluation Phase across all test cases
            List<TestCase> tests = problem.testCases();
            List<TestCaseResult> results = new ArrayList<>();
            long maxTimeMs = 0;
            long totalTimeMs = 0;
            SubmissionVerdict overallVerdict = SubmissionVerdict.ACCEPTED;
            int failedIndex = -1;

            int testIndex = 1;
            for (TestCase testCase : tests) {
                final int currentTestNum = testIndex;
                if (onProgress != null) {
                    runOnFxThread(() -> onProgress.accept(String.format("Running on test %d of %d...", currentTestNum, tests.size())));
                }

                ExecutionResult execResult = executeProcess(
                        language,
                        tempDir.toFile(),
                        testCase.input(),
                        problem.timeLimitMs()
                );

                maxTimeMs = Math.max(maxTimeMs, execResult.timeTakenMs);
                totalTimeMs += execResult.timeTakenMs;

                SubmissionVerdict testVerdict;
                if (execResult.isTimedOut) {
                    testVerdict = SubmissionVerdict.TIME_LIMIT_EXCEEDED;
                } else if (execResult.exitCode != 0) {
                    testVerdict = SubmissionVerdict.RUNTIME_ERROR;
                } else {
                    boolean matches = compareOutputs(execResult.stdout, testCase.expectedOutput());
                    testVerdict = matches ? SubmissionVerdict.ACCEPTED : SubmissionVerdict.WRONG_ANSWER;
                }

                TestCaseResult tcResult = new TestCaseResult(
                        testIndex,
                        testVerdict,
                        execResult.timeTakenMs,
                        testCase.input(),
                        testCase.expectedOutput(),
                        execResult.stdout,
                        execResult.stderr,
                        testCase.isSample()
                );
                results.add(tcResult);

                if (testVerdict != SubmissionVerdict.ACCEPTED) {
                    overallVerdict = testVerdict;
                    failedIndex = testIndex;
                    break; // Stop on first failing test case (Codeforces standard)
                }

                testIndex++;
            }

            int passed = (int) results.stream().filter(TestCaseResult::isPassed).count();
            return new JudgeResult(
                    overallVerdict,
                    passed,
                    tests.size(),
                    maxTimeMs,
                    totalTimeMs,
                    null,
                    results,
                    failedIndex
            );

        } catch (Exception ex) {
            return JudgeResult.internalError("Judge execution error: " + ex.getMessage());
        } finally {
            cleanupDir(tempDir);
        }
    }

    private static TestCaseResult runSingleTest(
            ProgrammingLanguage language,
            String sourceCode,
            String input,
            String expectedOutput,
            int timeLimitMs,
            int index,
            boolean isSample
    ) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("codetrail_custom_");
            File sourceFile = new File(tempDir.toFile(), language.defaultFileName());
            Files.writeString(sourceFile.toPath(), sourceCode, StandardCharsets.UTF_8);

            CompileResult compileResult = compileSource(language, tempDir.toFile(), sourceFile);
            if (!compileResult.success) {
                return new TestCaseResult(
                        index,
                        SubmissionVerdict.COMPILATION_ERROR,
                        0,
                        input,
                        expectedOutput,
                        "",
                        compileResult.errorLog,
                        isSample
                );
            }

            ExecutionResult execResult = executeProcess(language, tempDir.toFile(), input, timeLimitMs);
            SubmissionVerdict verdict;
            if (execResult.isTimedOut) {
                verdict = SubmissionVerdict.TIME_LIMIT_EXCEEDED;
            } else if (execResult.exitCode != 0) {
                verdict = SubmissionVerdict.RUNTIME_ERROR;
            } else if (!expectedOutput.isBlank()) {
                boolean matches = compareOutputs(execResult.stdout, expectedOutput);
                verdict = matches ? SubmissionVerdict.ACCEPTED : SubmissionVerdict.WRONG_ANSWER;
            } else {
                verdict = SubmissionVerdict.ACCEPTED;
            }

            return new TestCaseResult(
                    index,
                    verdict,
                    execResult.timeTakenMs,
                    input,
                    expectedOutput,
                    execResult.stdout,
                    execResult.stderr,
                    isSample
            );
        } catch (Exception ex) {
            return new TestCaseResult(
                    index,
                    SubmissionVerdict.INTERNAL_ERROR,
                    0,
                    input,
                    expectedOutput,
                    "",
                    ex.getMessage(),
                    isSample
            );
        } finally {
            cleanupDir(tempDir);
        }
    }

    private static CompileResult compileSource(ProgrammingLanguage language, File dir, File sourceFile) {
        if (!language.isCompiled()) {
            return new CompileResult(true, "");
        }

        List<String> command = new ArrayList<>();
        switch (language) {
            case CPP -> {
                command.add(CLANGPP_PATH);
                command.add("-std=c++17");
                command.add("-O2");
                command.add(sourceFile.getName());
                command.add("-o");
                command.add("solution");
            }
            case C -> {
                command.add(CLANG_PATH);
                command.add("-O2");
                command.add(sourceFile.getName());
                command.add("-o");
                command.add("solution");
                command.add("-lm");
            }
            case JAVA -> {
                command.add(JAVAC_PATH);
                command.add(sourceFile.getName());
            }
            case CSHARP -> {
                // If dotnet is available, use dotnet build / csc
                File cscFile = new File("/usr/local/bin/csc");
                if (cscFile.exists()) {
                    command.add(cscFile.getAbsolutePath());
                    command.add(sourceFile.getName());
                } else if (new File(DOTNET_PATH).exists() || commandExists("dotnet")) {
                    // Create minimal csproj for dotnet build
                    try {
                        String csproj = """
                                <Project Sdk="Microsoft.NET.Sdk">
                                  <PropertyGroup>
                                    <OutputType>Exe</OutputType>
                                    <TargetFramework>net8.0</TargetFramework>
                                    <ImplicitUsings>enable</ImplicitUsings>
                                    <Nullable>enable</Nullable>
                                  </PropertyGroup>
                                </Project>
                                """;
                        Files.writeString(new File(dir, "Solution.csproj").toPath(), csproj);
                        command.add(DOTNET_PATH);
                        command.add("build");
                        command.add("-c");
                        command.add("Release");
                        command.add("--nologo");
                        command.add("-v");
                        command.add("q");
                    } catch (IOException ignored) {}
                } else {
                    // System doesn't have dotnet installed natively - simulate C# via Java reflection or prompt
                    return new CompileResult(false, "C# compiler (.NET SDK) is not installed on this machine.\nPlease run code in C++, C, Python, Java, or JavaScript, or install .NET SDK (brew install dotnet).");
                }
            }
            default -> {
                return new CompileResult(true, "");
            }
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(dir);
            Process process = pb.start();

            String stderr = readStream(process.getErrorStream());
            String stdout = readStream(process.getInputStream());
            boolean finished = process.waitFor(15, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return new CompileResult(false, "Compilation timed out after 15 seconds.");
            }

            if (process.exitValue() != 0) {
                String error = !stderr.isBlank() ? stderr : stdout;
                return new CompileResult(false, error.isBlank() ? "Unknown compilation error" : error);
            }

            return new CompileResult(true, "");
        } catch (Exception ex) {
            return new CompileResult(false, "Compilation exception: " + ex.getMessage());
        }
    }

    private static ExecutionResult executeProcess(
            ProgrammingLanguage language,
            File dir,
            String input,
            int timeLimitMs
    ) {
        List<String> command = new ArrayList<>();
        switch (language) {
            case CPP, C -> command.add(new File(dir, "solution").getAbsolutePath());
            case PYTHON -> {
                command.add(PYTHON_PATH);
                command.add("solution.py");
            }
            case JAVA -> {
                command.add(JAVA_PATH);
                command.add("-cp");
                command.add(dir.getAbsolutePath());
                command.add("-Xmx256m");
                command.add("Main");
            }
            case JAVASCRIPT -> {
                command.add(NODE_PATH);
                command.add("solution.js");
            }
            case CSHARP -> {
                File exeFile = new File(dir, "Solution.exe");
                if (exeFile.exists()) {
                    command.add(exeFile.getAbsolutePath());
                } else {
                    command.add(DOTNET_PATH);
                    command.add("run");
                    command.add("--no-build");
                    command.add("-c");
                    command.add("Release");
                }
            }
        }

        long startTime = System.nanoTime();
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(dir);
            Process process = pb.start();

            // Write input to stdin
            if (input != null && !input.isEmpty()) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(input.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                } catch (IOException ignored) {}
            } else {
                try {
                    process.getOutputStream().close();
                } catch (IOException ignored) {}
            }

            // Read output asynchronously to prevent process pipe buffer blocking
            Future<String> stdoutFuture = EXECUTOR.submit(() -> readStream(process.getInputStream()));
            Future<String> stderrFuture = EXECUTOR.submit(() -> readStream(process.getErrorStream()));

            boolean finished = process.waitFor(timeLimitMs + 200L, TimeUnit.MILLISECONDS);
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000L;

            if (!finished || elapsedMs > timeLimitMs) {
                process.destroyForcibly();
                return new ExecutionResult("", "Time Limit Exceeded (" + elapsedMs + " ms > " + timeLimitMs + " ms)", -1, elapsedMs, true);
            }

            int exitCode = process.exitValue();
            String stdout = stdoutFuture.get(1, TimeUnit.SECONDS);
            String stderr = stderrFuture.get(1, TimeUnit.SECONDS);

            return new ExecutionResult(stdout, stderr, exitCode, elapsedMs, false);

        } catch (TimeoutException te) {
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000L;
            return new ExecutionResult("", "Process timeout", -1, elapsedMs, true);
        } catch (Exception ex) {
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000L;
            return new ExecutionResult("", "Runtime exception: " + ex.getMessage(), -1, elapsedMs, false);
        }
    }

    private static boolean compareOutputs(String actual, String expected) {
        if (actual == null) actual = "";
        if (expected == null) expected = "";

        String trimmedAct = actual.trim();
        String trimmedExp = expected.trim();

        // 1. Exact match (or both empty)
        if (trimmedAct.equals(trimmedExp)) {
            return true;
        }

        // 2. Token-by-token comparison (Codeforces testlib.h standard)
        // Treats all whitespace (single/multiple spaces, tabs, newlines) as token delimiters
        String[] actTokens = trimmedAct.isEmpty() ? new String[0] : trimmedAct.split("\\s+");
        String[] expTokens = trimmedExp.isEmpty() ? new String[0] : trimmedExp.split("\\s+");

        if (actTokens.length == expTokens.length) {
            boolean allTokensMatch = true;
            for (int i = 0; i < actTokens.length; i++) {
                String a = actTokens[i];
                String e = expTokens[i];

                if (a.equals(e)) {
                    continue;
                }

                // Case-insensitive check for boolean / status tokens (YES/NO, TRUE/FALSE, etc.)
                if (isCaseInsensitiveToken(e) && a.equalsIgnoreCase(e)) {
                    continue;
                }

                // Numeric precision tolerance comparison (within 1e-6)
                if (isNumericMatch(a, e)) {
                    continue;
                }

                allTokensMatch = false;
                break;
            }
            if (allTokensMatch) {
                return true;
            }
        }

        // 3. Fallback: line-by-line normalized string comparison
        String normActual = normalizeString(actual);
        String normExpected = normalizeString(expected);

        return normActual.equals(normExpected);
    }

    private static boolean isCaseInsensitiveToken(String token) {
        if (token == null) return false;
        String upper = token.toUpperCase(Locale.ROOT);
        return upper.equals("YES") || upper.equals("NO") ||
               upper.equals("TRUE") || upper.equals("FALSE") ||
               upper.equals("POSSIBLE") || upper.equals("IMPOSSIBLE") ||
               upper.equals("VALID") || upper.equals("INVALID");
    }

    private static boolean isNumericMatch(String a, String b) {
        try {
            double da = Double.parseDouble(a);
            double db = Double.parseDouble(b);
            double diff = Math.abs(da - db);
            if (diff <= 1e-6) {
                return true;
            }
            double maxVal = Math.max(1.0, Math.abs(db));
            return (diff / maxVal) <= 1e-6;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static String normalizeString(String s) {
        String[] lines = s.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.stripTrailing();
            if (!trimmed.isEmpty()) {
                if (!sb.isEmpty()) {
                    sb.append("\n");
                }
                sb.append(trimmed);
            }
        }
        return sb.toString().trim();
    }

    private static String readStream(InputStream is) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
                if (sb.length() > 500_000) { // Safety cap to avoid memory overload
                    sb.append("... [Output truncated]\n");
                    break;
                }
            }
            return sb.toString();
        } catch (IOException ex) {
            return "";
        }
    }

    private static boolean commandExists(String command) {
        try {
            Process p = new ProcessBuilder("which", command).start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void cleanupDir(Path dir) {
        if (dir == null) return;
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception ignored) {}
    }

    private record CompileResult(boolean success, String errorLog) {}
    private record ExecutionResult(String stdout, String stderr, int exitCode, long timeTakenMs, boolean isTimedOut) {}
}
