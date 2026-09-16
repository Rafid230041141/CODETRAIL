package application.client.dsa.judge;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent multi-language syntax highlighter for the CodeTrail code editor.
 * Styles code according to the Night Owl / Programiz Dark theme:
 *  - Keywords (class, public, static, void, return, etc.): #c792ea (Lilac / Purple)
 *  - Class / Struct Names (Main, Solution, etc.): #ffcb8b (Warm Peach / Orange)
 *  - Built-in Types (String, int, vector, etc.): #3cc9b0 (Bright Teal / Cyan)
 *  - Methods / Functions (main, println, etc.): #82aaff (Periwinkle / Sky Blue)
 *  - Strings (\"...\"): #addb67 (Vivid Lime / Yellow-Green)
 *  - Comments (// ..., # ...): #484f5d (Slate Muted Gray)
 *  - Plain text / Punctuation (System, out, ;, {}, ()): #d5deeb (Crisp Off-White)
 */
public final class CodeSyntaxHighlighter {

    private CodeSyntaxHighlighter() {}

    private static final String[] JAVA_KEYWORDS = {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
            "record", "var", "yield", "sealed", "permits", "non-sealed"
    };

    private static final String[] CPP_KEYWORDS = {
            "auto", "break", "case", "char", "const", "continue", "default", "do", "double", "else", "enum",
            "extern", "float", "for", "goto", "if", "inline", "int", "long", "register", "restrict", "return",
            "short", "signed", "sizeof", "static", "struct", "switch", "typedef", "union", "unsigned", "void",
            "volatile", "while", "class", "namespace", "new", "delete", "template", "typename", "public",
            "private", "protected", "virtual", "override", "final", "friend", "operator", "try", "catch", "throw",
            "bool", "true", "false", "nullptr", "NULL", "using", "constexpr", "noexcept"
    };

    private static final String[] PYTHON_KEYWORDS = {
            "def", "class", "if", "elif", "else", "for", "while", "return", "import", "from", "as", "in", "is",
            "not", "and", "or", "True", "False", "None", "pass", "break", "continue", "lambda", "try", "except",
            "finally", "with", "yield", "global", "nonlocal", "async", "await", "assert"
    };

    private static final String[] CSHARP_KEYWORDS = {
            "abstract", "as", "base", "bool", "break", "byte", "case", "catch", "char", "checked", "class",
            "const", "continue", "decimal", "default", "delegate", "do", "double", "else", "enum", "event",
            "explicit", "extern", "false", "finally", "fixed", "float", "for", "foreach", "goto", "if", "implicit",
            "in", "int", "interface", "internal", "is", "lock", "long", "namespace", "new", "null", "object",
            "operator", "out", "override", "params", "private", "protected", "public", "readonly", "ref", "return",
            "sbyte", "sealed", "short", "sizeof", "stackalloc", "static", "string", "struct", "switch", "this",
            "throw", "true", "try", "typeof", "uint", "ulong", "unchecked", "unsafe", "ushort", "using", "virtual",
            "void", "volatile", "while", "var", "async", "await", "record"
    };

    private static final String[] JS_KEYWORDS = {
            "function", "class", "const", "let", "var", "if", "else", "for", "while", "do", "switch", "case",
            "break", "continue", "return", "import", "export", "from", "default", "new", "this", "typeof",
            "instanceof", "void", "delete", "in", "of", "try", "catch", "finally", "throw", "async", "await",
            "yield", "null", "undefined", "true", "false"
    };

    private static final String[] COMMON_TYPES = {
            "String", "Integer", "Double", "Boolean", "Long", "Character", "Float", "Short", "Byte",
            "System", "Scanner", "Math", "List", "Map", "Set", "ArrayList", "HashMap", "HashSet",
            "Queue", "Deque", "ArrayDeque", "Stack", "PriorityQueue", "Arrays", "Collections", "StringBuilder",
            "vector", "string", "pair", "queue", "deque", "stack", "priority_queue", "cin", "cout", "endl",
            "printf", "scanf", "print", "input", "Console", "console", "Array", "Object", "Number", "std"
    };

    private static final Map<ProgrammingLanguage, Pattern> PATTERNS = new EnumMap<>(ProgrammingLanguage.class);

    static {
        for (ProgrammingLanguage lang : ProgrammingLanguage.values()) {
            PATTERNS.put(lang, buildPatternForLanguage(lang));
        }
    }

    private static Pattern buildPatternForLanguage(ProgrammingLanguage lang) {
        String[] keywords = switch (lang) {
            case CPP -> CPP_KEYWORDS;
            case C -> CPP_KEYWORDS;
            case PYTHON -> PYTHON_KEYWORDS;
            case JAVA -> JAVA_KEYWORDS;
            case CSHARP -> CSHARP_KEYWORDS;
            case JAVASCRIPT -> JS_KEYWORDS;
        };

        String kwPattern = "\\b(" + String.join("|", keywords) + ")\\b";
        String typePattern = "\\b(" + String.join("|", COMMON_TYPES) + ")\\b";
        String classPattern = "(?<=\\bclass\\s+)[A-Za-z0-9_]+|(?<=\\bstruct\\s+)[A-Za-z0-9_]+";
        String methodPattern = "\\b([a-zA-Z_][a-zA-Z0-9_]*)(?=\\s*\\()";
        String stringPattern = "\"([^\\\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'";
        String commentPattern = lang == ProgrammingLanguage.PYTHON
                ? "#[^\\n]*"
                : "//[^\\n]*|/\\*(?:.|[\\r\\n])*?\\*/";

        String includePattern = (lang == ProgrammingLanguage.CPP || lang == ProgrammingLanguage.C)
                ? "(?<INCLUDE>#\\s*include\\b(?:\\s*<[^>\\n]*>|\\s*\"[^\"\\n]*\")?)"
                : "(?<INCLUDE>$^)";
        String preprocPattern = (lang == ProgrammingLanguage.CPP || lang == ProgrammingLanguage.C || lang == ProgrammingLanguage.CSHARP)
                ? "(?<PREPROCESSOR>#\\s*(?:define|undef|ifdef|ifndef|if|elif|else|endif|pragma|error|warning|line|region|endregion)\\b)"
                : "(?<PREPROCESSOR>$^)";

        return Pattern.compile(
                "(?<COMMENT>" + commentPattern + ")"
                + "|" + includePattern
                + "|" + preprocPattern
                + "|(?<STRING>" + stringPattern + ")"
                + "|(?<KEYWORD>" + kwPattern + ")"
                + "|(?<CLASS>" + classPattern + ")"
                + "|(?<TYPE>" + typePattern + ")"
                + "|(?<METHOD>" + methodPattern + ")"
        );
    }

    public static StyleSpans<Collection<String>> computeHighlighting(String text, ProgrammingLanguage lang) {
        if (text == null) text = "";
        if (lang == null) lang = ProgrammingLanguage.CPP;

        Pattern pattern = PATTERNS.getOrDefault(lang, PATTERNS.get(ProgrammingLanguage.CPP));
        Matcher matcher = pattern.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        while (matcher.find()) {
            if (matcher.group("INCLUDE") != null) {
                String incText = matcher.group("INCLUDE");
                int incStart = matcher.start();
                int bracketIdx = incText.indexOf('<');
                if (bracketIdx == -1) bracketIdx = incText.indexOf('"');

                if (incStart > lastKwEnd) {
                    spansBuilder.add(Collections.singleton("plain"), incStart - lastKwEnd);
                }

                if (bracketIdx != -1) {
                    if (bracketIdx > 0) {
                        spansBuilder.add(Collections.singleton("preprocessor"), bracketIdx);
                    }
                    int headerLen = incText.length() - bracketIdx;
                    if (headerLen > 0) {
                        spansBuilder.add(Collections.singleton("header"), headerLen);
                    }
                } else {
                    spansBuilder.add(Collections.singleton("preprocessor"), incText.length());
                }
                lastKwEnd = matcher.end();
                continue;
            }

            String styleClass;
            if (matcher.group("COMMENT") != null) {
                styleClass = "comment";
            } else if (matcher.group("PREPROCESSOR") != null) {
                styleClass = "preprocessor";
            } else if (matcher.group("STRING") != null) {
                styleClass = "string";
            } else if (matcher.group("KEYWORD") != null) {
                styleClass = "keyword";
            } else if (matcher.group("CLASS") != null) {
                styleClass = "class-name";
            } else if (matcher.group("TYPE") != null) {
                styleClass = "type-name";
            } else if (matcher.group("METHOD") != null) {
                styleClass = "method-name";
            } else {
                styleClass = "plain";
            }

            if (matcher.start() > lastKwEnd) {
                spansBuilder.add(Collections.singleton("plain"), matcher.start() - lastKwEnd);
            }
            if (matcher.end() > matcher.start()) {
                spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            }
            lastKwEnd = matcher.end();
        }

        if (text.length() > lastKwEnd) {
            spansBuilder.add(Collections.singleton("plain"), text.length() - lastKwEnd);
        } else if (lastKwEnd == 0 && text.isEmpty()) {
            spansBuilder.add(Collections.singleton("plain"), 0);
        }
        return spansBuilder.create();
    }

    private static final ExecutorService HIGHLIGHT_EXECUTOR = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            r -> {
                Thread t = new Thread(r, "CodeSyntaxHighlightWorker");
                t.setDaemon(true);
                return t;
            }
    );

    public static CompletableFuture<StyleSpans<Collection<String>>> computeHighlightingAsync(
            String text, ProgrammingLanguage lang) {
        return CompletableFuture.supplyAsync(() -> computeHighlighting(text, lang), HIGHLIGHT_EXECUTOR);
    }

    public static void attachAsyncHighlighting(
            CodeArea codeArea,
            Supplier<ProgrammingLanguage> languageSupplier,
            long debounceMs) {
        if (codeArea == null) return;
        PauseTransition debounce = new PauseTransition(Duration.millis(debounceMs));
        AtomicLong textVersion = new AtomicLong(0);

        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null) return;
            long ver = textVersion.incrementAndGet();
            debounce.setOnFinished(evt -> {
                String textToHighlight = codeArea.getText();
                ProgrammingLanguage lang = languageSupplier != null ? languageSupplier.get() : ProgrammingLanguage.CPP;
                computeHighlightingAsync(textToHighlight, lang)
                        .thenAcceptAsync(spans -> {
                            if (textVersion.get() == ver && codeArea != null) {
                                codeArea.setStyleSpans(0, spans);
                            }
                        }, Platform::runLater);
            });
            debounce.playFromStart();
        });
    }

    public static void applyHighlightingAsync(
            CodeArea codeArea,
            String text,
            ProgrammingLanguage lang) {
        if (codeArea == null) return;
        final String targetText = text != null ? text : "";
        computeHighlightingAsync(targetText, lang)
                .thenAcceptAsync(spans -> {
                    if (codeArea != null && targetText.equals(codeArea.getText())) {
                        codeArea.setStyleSpans(0, spans);
                    }
                }, Platform::runLater);
    }
}
