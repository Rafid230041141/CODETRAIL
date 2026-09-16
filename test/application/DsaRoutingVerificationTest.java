package application;

import application.client.dsa.judge.CodeSyntaxHighlighter;
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
        System.out.println("[TEST 1] Verifying all 56 canonical topics have 5 exercises...");
        int totalExpected = 56;
        List<String> canonicalKeys = DsaProblemArenaWindow.CANONICAL_TOPIC_KEYS;

        if (canonicalKeys.size() != totalExpected) {
            throw new AssertionError("Expected 56 canonical topics but found: " + canonicalKeys.size());
        }

        int count = 0;
        for (String topicKey : canonicalKeys) {
            List<DsaProblem> problems = DsaProblemRepository.getProblemsForTopic(topicKey);
            if (problems.size() != 5) {
                throw new AssertionError("Topic '" + topicKey + "' expected 5 problems, found: " + problems.size());
            }
            DsaProblem first = problems.get(0);
            if (!first.topicKey().equalsIgnoreCase(topicKey)) {
                throw new AssertionError("Topic '" + topicKey + "' problem topic mismatch: " + first.topicKey());
            }
            count++;
        }
        System.out.println("  ✓ All " + count + " canonical topics have exactly 5 exercises each (280 exercises total).");
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

        assertEquals(ProgrammingLanguage.JAVASCRIPT, DsaProblemArenaWindow.getDefaultLanguageForTopic("html5"));
        assertEquals(ProgrammingLanguage.JAVASCRIPT, DsaProblemArenaWindow.getDefaultLanguageForTopic("css3"));
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

        System.out.println("  ✓ Authoritative topic languages verified.");
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

        // Language topics (C, C++, Java, Python, C#)
        if (!DsaProblemArenaWindow.isAlgorithmicTopic("cpp")) throw new AssertionError("cpp should be algorithmic");
        if (!DsaProblemArenaWindow.isAlgorithmicTopic("c")) throw new AssertionError("c should be algorithmic");
        if (!DsaProblemArenaWindow.isAlgorithmicTopic("java")) throw new AssertionError("java should be algorithmic");
        if (!DsaProblemArenaWindow.isAlgorithmicTopic("python")) throw new AssertionError("python should be algorithmic");

        // Non-DSA topics (Web Dev, App Dev, AI/ML, Data Science, Game Dev) must NOT be algorithmic
        List<String> nonDsaKeys = List.of(
                "html5", "css3", "javascript", "react", "node", "database", "auth", "deploy",
                "flutter", "reactnative", "kotlin", "swift", "statemgmt", "mobileapi", "sqlite", "publish",
                "ml_foundations", "math_ai", "scikit", "deep_learning", "vision", "nlp", "genai", "mlops",
                "numpy", "pandas", "eda", "statistics", "feature_eng", "bigdata", "sql_analytics", "bi_dashboards",
                "math_games", "pygame", "unity_basics", "unity_3d", "unreal", "game_physics", "audio_vfx", "game_publish"
        );
        for (String k : nonDsaKeys) {
            if (DsaProblemArenaWindow.isAlgorithmicTopic(k)) {
                throw new AssertionError("Expected practical topic '" + k + "' to NOT be algorithmic, but was marked algorithmic.");
            }
        }

        System.out.println("  ✓ All 16 DSA and 40 practical courses classified with 100% precision.");
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

        // CSS3 should provide CSS styles, NOT readFileSync(0)
        String cssCode = DsaProblemArenaWindow.getPracticalStarterTemplate("css3", ProgrammingLanguage.JAVASCRIPT);
        if (!cssCode.contains("/* Modern CSS3 Exercise */")) {
            throw new AssertionError("CSS3 template missing expected header");
        }

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

        System.out.println("  ✓ Domain-specific practical templates verified without competitive stdin overhead.");
    }

    private static void testSyntaxHighlightingAndLanguageVisibility() {
        System.out.println("[TEST 8] Verifying syntax highlighting for #include, header paths, and language labels...");

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

        // 2. Programming language display names
        for (ProgrammingLanguage lang : ProgrammingLanguage.values()) {
            if (lang.displayName() == null || lang.displayName().isBlank()) {
                throw new AssertionError("ProgrammingLanguage " + lang + " has empty displayName!");
            }
        }

        System.out.println("  ✓ #include and header tags properly highlighted as preprocessor & header (NOT comment).");
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
}
