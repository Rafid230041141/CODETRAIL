package application;

import application.client.dsa.judge.CodeSyntaxHighlighter;
import application.client.dsa.judge.Difficulty;
import application.client.dsa.judge.DsaProblem;
import application.client.dsa.judge.DsaProblemArenaWindow;
import application.client.dsa.judge.DsaProblemRepository;
import application.client.dsa.judge.ProgrammingLanguage;
import org.fxmisc.richtext.model.StyleSpans;
import java.util.Collection;
import java.util.List;

public class DsaRoutingVerificationTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  STARTING DSA TOPIC & LANGUAGE ROUTING TESTS    ");
        System.out.println("=================================================");

        testAllCanonicalTopicsHaveExercises();
        testNoLooseSubstringHijacking();
        testUnmappedTopicsReturnEmpty();
        testCanonicalAliases();
        testAuthoritativeLanguageDefaults();
        testAlgorithmicVsPracticalClassification();
        testPracticalStarterTemplates();
        testSyntaxHighlightingAndLanguageVisibility();

        System.out.println("=================================================");
        System.out.println("  ALL 8 TEST SUITES PASSED PERFECTLY!            ");
        System.out.println("=================================================");
    }

    private static void testAllCanonicalTopicsHaveExercises() {
        System.out.println("[TEST 1] Verifying 64 canonical topics and 304 exercises...");
        int totalExpected = 64;
        List<String> canonicalKeys = DsaProblemArenaWindow.CANONICAL_TOPIC_KEYS;

        if (canonicalKeys.size() != totalExpected) {
            throw new AssertionError("Expected 64 canonical topics but found: " + canonicalKeys.size());
        }

        List<String> newCanonicalKeys = List.of(
                "python", "java", "cpp", "c", "typescript", "rust", "golang", "sql");
        if (!canonicalKeys.containsAll(newCanonicalKeys)) {
            throw new AssertionError("Recovered canonical topic list is missing one or more new keys: "
                    + newCanonicalKeys);
        }

        int count = 0;
        int exerciseCount = 0;
        for (String topicKey : canonicalKeys) {
            List<DsaProblem> problems = DsaProblemRepository.getProblemsForTopic(topicKey);
            int expectedProblems = newCanonicalKeys.contains(topicKey) ? 3 : 5;
            if (problems.size() != expectedProblems) {
                throw new AssertionError("Topic '" + topicKey + "' expected " + expectedProblems
                        + " problems, found: " + problems.size());
            }
            if (newCanonicalKeys.contains(topicKey)) {
                var difficulties = problems.stream()
                        .map(DsaProblem::difficulty)
                        .collect(java.util.stream.Collectors.toSet());
                if (difficulties.size() != 3
                        || !difficulties.containsAll(List.of(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD))) {
                    throw new AssertionError("Topic '" + topicKey
                            + "' must have exactly one Easy, Medium, and Tough exercise: " + difficulties);
                }
            }
            DsaProblem first = problems.get(0);
            if (!first.topicKey().equalsIgnoreCase(topicKey)) {
                throw new AssertionError("Topic '" + topicKey + "' problem topic mismatch: " + first.topicKey());
            }
            count++;
            exerciseCount += problems.size();
        }
        if (exerciseCount != 304) {
            throw new AssertionError("Expected 304 total exercises but found: " + exerciseCount);
        }
        System.out.println("  ✓ All 56 legacy topics have 5 exercises and 8 language topics have 3 each (304 total).");
    }

    private static void testNoLooseSubstringHijacking() {
        System.out.println("[TEST 2] Verifying non-DSA topics are not hijacked by substring matching...");

        // NumPy must return NM-101, NOT Two Sum (DS-101)
        List<DsaProblem> numpy = DsaProblemRepository.getProblemsForTopic("numpy");
        if (!numpy.get(0).id().startsWith("NM-")) {
            throw new AssertionError("NumPy hijacked into non-NumPy problem: " + numpy.get(0).id());
        }

        // HTML5 must return WH-101
        List<DsaProblem> html5 = DsaProblemRepository.getProblemsForTopic("html5");
        if (!html5.get(0).id().startsWith("WH-")) {
            throw new AssertionError("HTML5 hijacked: " + html5.get(0).id());
        }

        // Flutter must return FL-101
        List<DsaProblem> flutter = DsaProblemRepository.getProblemsForTopic("flutter");
        if (!flutter.get(0).id().startsWith("FL-")) {
            throw new AssertionError("Flutter hijacked: " + flutter.get(0).id());
        }

        // ML Foundations must return MF-101
        List<DsaProblem> ml = DsaProblemRepository.getProblemsForTopic("ml_foundations");
        if (!ml.get(0).id().startsWith("MF-")) {
            throw new AssertionError("ML Foundations hijacked: " + ml.get(0).id());
        }

        // Pygame must return GM-106 (or PG-)
        List<DsaProblem> pygame = DsaProblemRepository.getProblemsForTopic("pygame");
        if (pygame.isEmpty() || pygame.get(0).id().startsWith("DS-")) {
            throw new AssertionError("Pygame hijacked into classic DSA: " + pygame.get(0).id());
        }

        System.out.println("  ✓ Non-DSA topics route cleanly to their own exercises without hijacking.");
    }

    private static void testUnmappedTopicsReturnEmpty() {
        System.out.println("[TEST 3] Verifying unmapped topic titles return empty list and do NOT fall through...");

        // Lesson titles containing "array" or "stack" must NOT return classic DSA Arrays or Stacks
        List<DsaProblem> unmapped1 = DsaProblemRepository.getProblemsForTopic("N-Dimensional Arrays & Slicing");
        if (!unmapped1.isEmpty()) {
            throw new AssertionError("Lesson title with 'array' incorrectly routed to: " + unmapped1.get(0).id());
        }

        List<DsaProblem> unmapped2 = DsaProblemRepository.getProblemsForTopic("Stack vs Heap Mechanics");
        if (!unmapped2.isEmpty()) {
            throw new AssertionError("Lesson title with 'stack' incorrectly routed to: " + unmapped2.get(0).id());
        }

        List<DsaProblem> unmapped3 = DsaProblemRepository.getProblemsForTopic("unknown_course_xyz");
        if (!unmapped3.isEmpty()) {
            throw new AssertionError("Random key incorrectly routed to: " + unmapped3.get(0).id());
        }

        System.out.println("  ✓ Unmapped titles return empty list (enabling honest 'No exercises' state).");
    }

    private static void testCanonicalAliases() {
        System.out.println("[TEST 4] Verifying canonical aliases...");

        assertEquals("arrays", DsaProblemRepository.getCanonicalTopicKey("array"));
        assertEquals("arrays", DsaProblemRepository.getCanonicalTopicKey("arrays"));
        assertEquals("stacks", DsaProblemRepository.getCanonicalTopicKey("stack"));
        assertEquals("html5", DsaProblemRepository.getCanonicalTopicKey("html5"));
        assertEquals("html5", DsaProblemRepository.getCanonicalTopicKey("semantic web"));
        assertEquals("numpy", DsaProblemRepository.getCanonicalTopicKey("numpy"));
        assertEquals("numpy", DsaProblemRepository.getCanonicalTopicKey("scipy"));
        assertEquals("pandas", DsaProblemRepository.getCanonicalTopicKey("data wrangling"));
        assertEquals("react", DsaProblemRepository.getCanonicalTopicKey("react.js"));
        assertEquals("ml_foundations", DsaProblemRepository.getCanonicalTopicKey("machine learning"));
        assertEquals("pygame", DsaProblemRepository.getCanonicalTopicKey("2d games"));
        assertNull(DsaProblemRepository.getCanonicalTopicKey("totally_fake_key_1234"));

        System.out.println("  ✓ Canonical aliases map accurately.");
    }

    private static void testAuthoritativeLanguageDefaults() {
        System.out.println("[TEST 5] Verifying authoritative topic language defaults...");

        assertEquals(ProgrammingLanguage.PYTHON, DsaProblemArenaWindow.getDefaultLanguageForTopic("numpy"));
        assertEquals(ProgrammingLanguage.PYTHON, DsaProblemArenaWindow.getDefaultLanguageForTopic("pandas"));
        assertEquals(ProgrammingLanguage.PYTHON, DsaProblemArenaWindow.getDefaultLanguageForTopic("eda"));
        assertEquals(ProgrammingLanguage.PYTHON, DsaProblemArenaWindow.getDefaultLanguageForTopic("ml_foundations"));
        assertEquals(ProgrammingLanguage.PYTHON, DsaProblemArenaWindow.getDefaultLanguageForTopic("deep_learning"));
        assertEquals(ProgrammingLanguage.PYTHON, DsaProblemArenaWindow.getDefaultLanguageForTopic("pygame"));

        assertEquals(ProgrammingLanguage.HTML, DsaProblemArenaWindow.getDefaultLanguageForTopic("html5"));
        assertEquals(ProgrammingLanguage.CSS, DsaProblemArenaWindow.getDefaultLanguageForTopic("css3"));
        assertEquals(ProgrammingLanguage.SQL, DsaProblemArenaWindow.getDefaultLanguageForTopic("database"));
        assertEquals(ProgrammingLanguage.SQL, DsaProblemArenaWindow.getDefaultLanguageForTopic("sql_analytics"));
        assertEquals(ProgrammingLanguage.SQL, DsaProblemArenaWindow.getDefaultLanguageForTopic("sqlite"));
        assertEquals(ProgrammingLanguage.JAVASCRIPT, DsaProblemArenaWindow.getDefaultLanguageForTopic("javascript"));
        assertEquals(ProgrammingLanguage.JAVASCRIPT, DsaProblemArenaWindow.getDefaultLanguageForTopic("react"));
        assertEquals(ProgrammingLanguage.JAVASCRIPT, DsaProblemArenaWindow.getDefaultLanguageForTopic("node"));
        assertEquals(ProgrammingLanguage.JAVASCRIPT, DsaProblemArenaWindow.getDefaultLanguageForTopic("reactnative"));

        assertEquals(ProgrammingLanguage.CSHARP, DsaProblemArenaWindow.getDefaultLanguageForTopic("unity_basics"));
        assertEquals(ProgrammingLanguage.CSHARP, DsaProblemArenaWindow.getDefaultLanguageForTopic("unity_3d"));

        assertEquals(ProgrammingLanguage.CPP, DsaProblemArenaWindow.getDefaultLanguageForTopic("arrays"));
        assertEquals(ProgrammingLanguage.CPP, DsaProblemArenaWindow.getDefaultLanguageForTopic("graphs"));
        assertEquals(ProgrammingLanguage.CPP, DsaProblemArenaWindow.getDefaultLanguageForTopic("math_games"));
        assertEquals(ProgrammingLanguage.CPP, DsaProblemArenaWindow.getDefaultLanguageForTopic("unreal"));

        assertEquals(ProgrammingLanguage.PYTHON, DsaProblemArenaWindow.getDefaultLanguageForTopic("python"));
        assertEquals(ProgrammingLanguage.JAVA, DsaProblemArenaWindow.getDefaultLanguageForTopic("java"));
        assertEquals(ProgrammingLanguage.CPP, DsaProblemArenaWindow.getDefaultLanguageForTopic("cpp"));
        assertEquals(ProgrammingLanguage.C, DsaProblemArenaWindow.getDefaultLanguageForTopic("c"));
        assertEquals(ProgrammingLanguage.CSHARP, DsaProblemArenaWindow.getDefaultLanguageForTopic("csharp"));

        // Exercise-level language verification
        DsaProblem wh101 = DsaProblemRepository.getProblemById("WH-101");
        DsaProblem wc101 = DsaProblemRepository.getProblemById("WC-101");
        DsaProblem wdb101 = DsaProblemRepository.getProblemById("WDB-101");
        DsaProblem sa101 = DsaProblemRepository.getProblemById("SA-101");

        assertNotNull(wh101);
        assertNotNull(wc101);
        assertNotNull(wdb101);
        assertNotNull(sa101);

        assertEquals(ProgrammingLanguage.HTML, DsaProblemArenaWindow.getDefaultLanguageForExercise(wh101, "html5"));
        assertEquals(ProgrammingLanguage.CSS, DsaProblemArenaWindow.getDefaultLanguageForExercise(wc101, "css3"));
        assertEquals(ProgrammingLanguage.SQL, DsaProblemArenaWindow.getDefaultLanguageForExercise(wdb101, "database"));
        assertEquals(ProgrammingLanguage.SQL, DsaProblemArenaWindow.getDefaultLanguageForExercise(sa101, "sql_analytics"));

        System.out.println("  ✓ Authoritative topic and exercise languages verified.");
    }

    private static void testAlgorithmicVsPracticalClassification() {
        System.out.println("[TEST 6] Verifying algorithmic vs practical statement classification...");

        // DSA Topics 1-16 must be algorithmic (show time limits, competitive judge)
        List<String> dsaKeys = List.of(
                "arrays", "linked lists", "stacks", "queues", "hash maps", "heaps",
                "trees", "dsu", "trie", "sorting algorithms", "searching", "graphs",
                "range queries", "algorithmic paradigms", "string algorithms", "mathematics"
        );
        for (String k : dsaKeys) {
            if (!DsaProblemArenaWindow.isAlgorithmicTopic(k)) {
                throw new AssertionError("Expected DSA topic '" + k + "' to be algorithmic, but was not.");
            }
        }

        // Practical topics (including the recovered language-practice keys) must NOT be algorithmic
        List<String> nonDsaKeys = List.of(
                "html5", "css3", "javascript", "react", "node", "database", "auth", "deploy",
                "flutter", "reactnative", "kotlin", "swift", "statemgmt", "mobileapi", "sqlite", "publish",
                "ml_foundations", "math_ai", "scikit", "deep_learning", "vision", "nlp", "genai", "mlops",
                "numpy", "pandas", "eda", "statistics", "feature_eng", "bigdata", "sql_analytics", "bi_dashboards",
                "math_games", "pygame", "unity_basics", "unity_3d", "unreal", "game_physics", "audio_vfx", "game_publish",
                "python", "java", "cpp", "c", "typescript", "rust", "golang", "sql"
        );
        for (String k : nonDsaKeys) {
            if (DsaProblemArenaWindow.isAlgorithmicTopic(k)) {
                throw new AssertionError("Expected practical topic '" + k + "' to NOT be algorithmic, but was marked algorithmic.");
            }
        }

        System.out.println("  ✓ All 16 DSA and 48 practical courses classified with 100% precision.");
    }

    private static void testPracticalStarterTemplates() {
        System.out.println("[TEST 7] Verifying domain starter templates for practical exercises...");

        // HTML5 should provide semantic HTML boilerplate, NOT readFileSync(0)
        String htmlCode = DsaProblemArenaWindow.getPracticalStarterTemplate("html5", ProgrammingLanguage.JAVASCRIPT);
        if (!htmlCode.contains("<!DOCTYPE html>")) {
            throw new AssertionError("HTML5 template should contain <!DOCTYPE html>, got: " + htmlCode);
        }
        if (htmlCode.contains("readFileSync(0)")) {
            throw new AssertionError("HTML5 template should not contain readFileSync(0)");
        }

        // CSS3 and SQL templates should be empty per user requirement
        String cssCode = DsaProblemArenaWindow.getPracticalStarterTemplate("css3", ProgrammingLanguage.CSS);
        if (!cssCode.isEmpty()) {
            throw new AssertionError("CSS3 template should be empty, got: " + cssCode);
        }
        assertEquals("", ProgrammingLanguage.CSS.starterTemplate());

        String sqlCode = DsaProblemArenaWindow.getPracticalStarterTemplate("database", ProgrammingLanguage.SQL);
        if (!sqlCode.isEmpty()) {
            throw new AssertionError("SQL template should be empty, got: " + sqlCode);
        }
        assertEquals("", ProgrammingLanguage.SQL.starterTemplate());

        // HTML template is kept intact
        assertEquals(true, ProgrammingLanguage.HTML.starterTemplate().contains("<!DOCTYPE html>"));

        // NumPy should provide numpy/pandas pipeline, NOT sys.stdin.read().split()
        String npCode = DsaProblemArenaWindow.getPracticalStarterTemplate("numpy", ProgrammingLanguage.PYTHON);
        if (!npCode.contains("import numpy as np")) {
            throw new AssertionError("NumPy template should contain import numpy as np, got: " + npCode);
        }
        if (npCode.contains("sys.stdin.read().split()")) {
            throw new AssertionError("NumPy template should not contain competitive stdin reading");
        }

        // Flutter should provide Flutter widget starter
        String flCode = DsaProblemArenaWindow.getPracticalStarterTemplate("flutter", ProgrammingLanguage.JAVA);
        if (!flCode.contains("package:flutter/material.dart")) {
            throw new AssertionError("Flutter template missing flutter/material.dart import");
        }

        // Pygame should provide Pygame event loop starter
        String pgCode = DsaProblemArenaWindow.getPracticalStarterTemplate("pygame", ProgrammingLanguage.PYTHON);
        if (!pgCode.contains("import pygame")) {
            throw new AssertionError("Pygame template missing import pygame");
        }

        System.out.println("  ✓ Domain-specific practical templates verified (CSS/SQL blank, HTML5 preserved).");
    }

    private static void testSyntaxHighlightingAndLanguageVisibility() {
        System.out.println("[TEST 8] Verifying syntax highlighting for C++, HTML, CSS, SQL, and language labels...");

        // 1. C++ #include <iostream>
        String cppCode = "#include <iostream>\nusing namespace std;\nint main() {\n    // comment\n    return 0;\n}";
        StyleSpans<Collection<String>> spans = CodeSyntaxHighlighter.computeHighlighting(cppCode, ProgrammingLanguage.CPP);

        boolean hasPreprocessor = false;
        boolean hasHeader = false;
        boolean hasComment = false;
        for (var span : spans) {
            if (span.getStyle().contains("preprocessor")) hasPreprocessor = true;
            if (span.getStyle().contains("header")) hasHeader = true;
            if (span.getStyle().contains("comment")) hasComment = true;
        }

        if (!hasPreprocessor) {
            throw new AssertionError("Expected #include to produce 'preprocessor' style span, but was not found.");
        }
        if (!hasHeader) {
            throw new AssertionError("Expected <iostream> to produce 'header' style span, but was not found.");
        }
        if (!hasComment) {
            throw new AssertionError("Expected // comment to produce 'comment' style span.");
        }

        // 2. HTML syntax highlighting
        String testHtml = DsaProblemArenaWindow.getPracticalStarterTemplate("html5", ProgrammingLanguage.HTML);
        StyleSpans<Collection<String>> htmlSpans = CodeSyntaxHighlighter.computeHighlighting(testHtml, ProgrammingLanguage.HTML);
        boolean htmlHasKeyword = false;
        boolean htmlHasType = false;
        boolean htmlHasComment = false;
        for (var span : htmlSpans) {
            if (span.getStyle().contains("keyword")) htmlHasKeyword = true;
            if (span.getStyle().contains("type-name")) htmlHasType = true;
            if (span.getStyle().contains("comment")) htmlHasComment = true;
        }
        if (!htmlHasKeyword || !htmlHasType || !htmlHasComment) {
            throw new AssertionError("HTML highlighting missing expected token spans: kw=" + htmlHasKeyword + ", type=" + htmlHasType + ", comment=" + htmlHasComment);
        }

        // 3. CSS syntax highlighting
        String testCss = "/* Comment */\n@media (max-width: 600px) {\n  .card { color: #fff; margin: 10px; }\n}";
        StyleSpans<Collection<String>> cssSpans = CodeSyntaxHighlighter.computeHighlighting(testCss, ProgrammingLanguage.CSS);
        boolean cssHasComment = false;
        boolean cssHasPreproc = false;
        boolean cssHasClass = false;
        for (var span : cssSpans) {
            if (span.getStyle().contains("comment")) cssHasComment = true;
            if (span.getStyle().contains("preprocessor")) cssHasPreproc = true;
            if (span.getStyle().contains("class-name")) cssHasClass = true;
        }
        if (!cssHasComment || !cssHasPreproc || !cssHasClass) {
            throw new AssertionError("CSS highlighting missing expected token spans: comment=" + cssHasComment + ", preproc=" + cssHasPreproc + ", class=" + cssHasClass);
        }

        // 4. SQL syntax highlighting
        String testSql = "-- Query\nSELECT id, name FROM users WHERE score > 80;";
        StyleSpans<Collection<String>> sqlSpans = CodeSyntaxHighlighter.computeHighlighting(testSql, ProgrammingLanguage.SQL);
        boolean sqlHasComment = false;
        boolean sqlHasKeyword = false;
        for (var span : sqlSpans) {
            if (span.getStyle().contains("comment")) sqlHasComment = true;
            if (span.getStyle().contains("keyword")) sqlHasKeyword = true;
        }
        if (!sqlHasComment || !sqlHasKeyword) {
            throw new AssertionError("SQL highlighting missing expected token spans: comment=" + sqlHasComment + ", kw=" + sqlHasKeyword);
        }

        // 5. Programming language display names
        for (ProgrammingLanguage lang : ProgrammingLanguage.values()) {
            if (lang.displayName() == null || lang.displayName().isBlank()) {
                throw new AssertionError("ProgrammingLanguage " + lang + " has empty displayName!");
            }
        }

        System.out.println("  ✓ C++, HTML, CSS, SQL highlighting verified with zero parser errors.");
        System.out.println("  ✓ Language display names verified for high-contrast ComboBox presentation.");
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null && actual == null) return;
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError("Expected [" + expected + "] but got [" + actual + "]");
        }
    }

    private static void assertNull(Object actual) {
        if (actual != null) {
            throw new AssertionError("Expected null but got [" + actual + "]");
        }
    }

    private static void assertNotNull(Object actual) {
        if (actual == null) {
            throw new AssertionError("Expected non-null object");
        }
    }
}
