package application.client.dsa.judge;

import java.util.*;

public class DsaProblemRepository {

    private static final List<DsaProblem> ALL_PROBLEMS = new ArrayList<>();
    private static final Map<String, List<DsaProblem>> PROBLEMS_BY_TOPIC = new LinkedHashMap<>();
    private static final Map<String, DsaProblem> PROBLEMS_BY_ID = new LinkedHashMap<>();

    static {
        // Phase 1: Data Structures (5 problems per sub-topic = 45 problems total)
        registerProblems(buildArrayProblems());
        registerProblems(buildLinkedListProblems());
        registerProblems(buildStackProblems());
        registerProblems(buildQueueProblems());
        registerProblems(buildHashMapProblems());
        registerProblems(buildHeapProblems());
        registerProblems(buildTreeProblems());
        registerProblems(buildDsuProblems());
        registerProblems(buildTrieProblems());

        // Phases 2 to 8: Algorithms & Mathematics (5 problems each = 35 problems)
        registerProblems(buildSortingProblems());
        registerProblems(buildSearchingProblems());
        registerProblems(buildGraphsProblems());
        registerProblems(buildRangeQueriesProblems());
        registerProblems(buildParadigmsProblems());
        registerProblems(buildStringProblems());
        registerProblems(buildMathProblems());

        // Web Development topics (8 topics x 5 exercises each)
        registerProblems(buildHtml5Problems());
        registerProblems(buildCss3Problems());
        registerProblems(buildJavascriptProblems());
        registerProblems(buildReactProblems());
        registerProblems(buildNodeProblems());
        registerProblems(buildDatabaseProblems());
        registerProblems(buildAuthProblems());
        registerProblems(buildDeployProblems());

        // App Development topics (8 topics x 5 exercises each)
        registerProblems(buildFlutterProblems());
        registerProblems(buildReactNativeProblems());
        registerProblems(buildKotlinProblems());
        registerProblems(buildSwiftProblems());
        registerProblems(buildStateMgmtProblems());
        registerProblems(buildMobileApiProblems());
        registerProblems(buildSqliteProblems());
        registerProblems(buildPublishProblems());

        // AI/ML topics (8 topics x 5 exercises each)
        registerProblems(buildMlFoundationsProblems());
        registerProblems(buildMathAiProblems());
        registerProblems(buildScikitProblems());
        registerProblems(buildDeepLearningProblems());
        registerProblems(buildVisionProblems());
        registerProblems(buildNlpProblems());
        registerProblems(buildGenAiProblems());
        registerProblems(buildMlopsProblems());

        // Data Science topics (8 topics x 5 exercises each)
        registerProblems(buildNumpyProblems());
        registerProblems(buildPandasProblems());
        registerProblems(buildEdaProblems());
        registerProblems(buildStatisticsProblems());
        registerProblems(buildFeatureEngProblems());
        registerProblems(buildBigDataProblems());
        registerProblems(buildSqlAnalyticsProblems());
        registerProblems(buildBiDashboardsProblems());

        // Game Development topics (8 topics x 5 exercises each)
        registerProblems(buildMathGamesProblems());
        registerProblems(buildPygameProblems());
        registerProblems(buildUnityBasicsProblems());
        registerProblems(buildUnity3dProblems());
        registerProblems(buildUnrealProblems());
        registerProblems(buildGamePhysicsProblems());
        registerProblems(buildAudioVfxProblems());
        registerProblems(buildGamePublishProblems());
    }

    private static void registerProblems(List<DsaProblem> problems) {
        for (DsaProblem problem : problems) {
            ALL_PROBLEMS.add(problem);
            PROBLEMS_BY_ID.put(problem.id(), problem);
            PROBLEMS_BY_TOPIC.computeIfAbsent(problem.topicKey().toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(problem);
        }
    }

    public static List<DsaProblem> getAllProblems() {
        return Collections.unmodifiableList(ALL_PROBLEMS);
    }

    private static final Map<String, String> CANONICAL_ALIASES = Map.ofEntries(
            Map.entry("array", "arrays"),
            Map.entry("arrays", "arrays"),
            Map.entry("dsa-arrays", "arrays"),
            Map.entry("linked-list", "linked lists"),
            Map.entry("linked-lists", "linked lists"),
            Map.entry("linked list", "linked lists"),
            Map.entry("linked lists", "linked lists"),
            Map.entry("stack", "stacks"),
            Map.entry("stacks", "stacks"),
            Map.entry("dsa-stacks", "stacks"),
            Map.entry("queue", "queues"),
            Map.entry("queues", "queues"),
            Map.entry("queues & deques", "queues"),
            Map.entry("hash-map", "hash maps"),
            Map.entry("hash-maps", "hash maps"),
            Map.entry("hash map", "hash maps"),
            Map.entry("hash maps", "hash maps"),
            Map.entry("hash maps & sets", "hash maps"),
            Map.entry("heap", "heaps"),
            Map.entry("heaps", "heaps"),
            Map.entry("heaps & priority queues", "heaps"),
            Map.entry("tree", "trees"),
            Map.entry("trees", "trees"),
            Map.entry("trees & bst", "trees"),
            Map.entry("dsu", "dsu"),
            Map.entry("disjoint-set", "dsu"),
            Map.entry("disjoint set union (dsu)", "dsu"),
            Map.entry("trie", "trie"),
            Map.entry("trie (prefix trees)", "trie"),
            Map.entry("sorting", "sorting algorithms"),
            Map.entry("sorting algorithms", "sorting algorithms"),
            Map.entry("searching", "searching"),
            Map.entry("searching algorithms", "searching"),
            Map.entry("graphs", "graphs"),
            Map.entry("graph algorithms", "graphs"),
            Map.entry("range queries", "range queries"),
            Map.entry("range-queries", "range queries"),
            Map.entry("algorithmic paradigms", "algorithmic paradigms"),
            Map.entry("paradigms", "algorithmic paradigms"),
            Map.entry("string algorithms", "string algorithms"),
            Map.entry("strings", "string algorithms"),
            Map.entry("mathematics", "mathematics"),
            Map.entry("math", "mathematics"),
            // Web Dev Aliases
            Map.entry("html", "html5"),
            Map.entry("semantic web", "html5"),
            Map.entry("css", "css3"),
            Map.entry("flexbox", "css3"),
            Map.entry("grid", "css3"),
            Map.entry("js", "javascript"),
            Map.entry("es6", "javascript"),
            Map.entry("reactjs", "react"),
            Map.entry("react.js", "react"),
            Map.entry("nodejs", "node"),
            Map.entry("node.js", "node"),
            Map.entry("express", "node"),
            Map.entry("databases", "database"),
            Map.entry("postgresql", "database"),
            Map.entry("mongodb", "database"),
            Map.entry("sql", "database"),
            Map.entry("authentication", "auth"),
            Map.entry("jwt", "auth"),
            Map.entry("oauth", "auth"),
            Map.entry("devops", "deploy"),
            Map.entry("docker", "deploy"),
            Map.entry("cicd", "deploy"),
            // App Dev Aliases
            Map.entry("dart", "flutter"),
            Map.entry("react-native", "reactnative"),
            Map.entry("expo", "reactnative"),
            Map.entry("android", "kotlin"),
            Map.entry("compose", "kotlin"),
            Map.entry("swiftui", "swift"),
            Map.entry("ios", "swift"),
            Map.entry("state management", "statemgmt"),
            Map.entry("bloc", "statemgmt"),
            Map.entry("riverpod", "statemgmt"),
            Map.entry("mobile api", "mobileapi"),
            Map.entry("firebase", "mobileapi"),
            Map.entry("room", "sqlite"),
            Map.entry("local storage", "sqlite"),
            Map.entry("app store", "publish"),
            Map.entry("play store", "publish"),
            // AI / ML Aliases
            Map.entry("machine learning", "ml_foundations"),
            Map.entry("supervised learning", "ml_foundations"),
            Map.entry("linear algebra", "math_ai"),
            Map.entry("calculus", "math_ai"),
            Map.entry("scikit-learn", "scikit"),
            Map.entry("sklearn", "scikit"),
            Map.entry("neural networks", "deep_learning"),
            Map.entry("pytorch", "deep_learning"),
            Map.entry("computer vision", "vision"),
            Map.entry("opencv", "vision"),
            Map.entry("transformers", "nlp"),
            Map.entry("generative ai", "genai"),
            Map.entry("llm", "genai"),
            Map.entry("llms", "genai"),
            // Data Science Aliases
            Map.entry("scipy", "numpy"),
            Map.entry("numerical computing", "numpy"),
            Map.entry("data wrangling", "pandas"),
            Map.entry("visualization", "eda"),
            Map.entry("applied statistics", "statistics"),
            Map.entry("hypothesis testing", "statistics"),
            Map.entry("feature engineering", "feature_eng"),
            Map.entry("pca", "feature_eng"),
            Map.entry("big data", "bigdata"),
            Map.entry("spark", "bigdata"),
            Map.entry("pyspark", "bigdata"),
            Map.entry("sql analytics", "sql_analytics"),
            Map.entry("data warehouse", "sql_analytics"),
            Map.entry("streamlit", "bi_dashboards"),
            // Game Dev Aliases
            Map.entry("game math", "math_games"),
            Map.entry("2d games", "pygame"),
            Map.entry("unity engine", "unity_basics"),
            Map.entry("unity", "unity_basics"),
            Map.entry("unity 3d", "unity_3d"),
            Map.entry("unreal engine", "unreal"),
            Map.entry("game physics", "game_physics"),
            Map.entry("game audio", "audio_vfx"),
            Map.entry("shaders", "audio_vfx"),
            Map.entry("game optimization", "game_publish")
    );

    public static String getCanonicalTopicKey(String topicKey) {
        if (topicKey == null) return null;
        String key = topicKey.toLowerCase(Locale.ROOT).trim();
        if (PROBLEMS_BY_TOPIC.containsKey(key)) {
            return key;
        }
        if (CANONICAL_ALIASES.containsKey(key)) {
            return CANONICAL_ALIASES.get(key);
        }
        if (key.equals("data structures") || key.equals("data structure") || key.equals("ds")) {
            return "arrays";
        }
        return null;
    }

    public static List<DsaProblem> getProblemsForTopic(String topicKey) {
        if (topicKey == null) return List.of();
        String key = topicKey.toLowerCase(Locale.ROOT).trim();

        // 1. Exact match in registered topics
        if (PROBLEMS_BY_TOPIC.containsKey(key)) {
            return Collections.unmodifiableList(PROBLEMS_BY_TOPIC.get(key));
        }

        // 2. Canonical exact aliases (NO loose substring matching)
        String canonicalKey = CANONICAL_ALIASES.get(key);
        if (canonicalKey != null && PROBLEMS_BY_TOPIC.containsKey(canonicalKey)) {
            return Collections.unmodifiableList(PROBLEMS_BY_TOPIC.get(canonicalKey));
        }

        // 3. Special case: generic "data structures" or "ds" returns all 45 DS problems
        if (key.equals("data structures") || key.equals("data structure") || key.equals("ds")) {
            List<DsaProblem> allDs = new ArrayList<>();
            allDs.addAll(getListSafe("arrays"));
            allDs.addAll(getListSafe("linked lists"));
            allDs.addAll(getListSafe("stacks"));
            allDs.addAll(getListSafe("queues"));
            allDs.addAll(getListSafe("hash maps"));
            allDs.addAll(getListSafe("heaps"));
            allDs.addAll(getListSafe("trees"));
            allDs.addAll(getListSafe("dsu"));
            allDs.addAll(getListSafe("trie"));
            return Collections.unmodifiableList(allDs);
        }

        // Unmapped topic returns empty list - NEVER silently falls through to Arrays!
        return List.of();
    }

    private static List<DsaProblem> getListSafe(String key) {
        return PROBLEMS_BY_TOPIC.getOrDefault(key.toLowerCase(Locale.ROOT), List.of());
    }

    public static DsaProblem getProblemById(String id) {
        if (id == null) return null;
        return PROBLEMS_BY_ID.get(id);
    }

    // =========================================================================
    // DATA STRUCTURES - TOPIC 1: ARRAYS (5 Problems)
    // =========================================================================
    private static List<DsaProblem> buildArrayProblems() {
        String topic = "arrays";
        String topicTitle = "Data Structures: Arrays";

        return List.of(
                new DsaProblem(
                        "ARR-101",
                        topic,
                        topicTitle,
                        "A. Two Sum Target Pair",
                        Difficulty.EASY,
                        1000,
                        256,
                        "You are given an array of n integers and an integer k. Find the 1-based indices of two distinct elements whose sum is exactly k. You may assume that each input has exactly one solution, and you may not use the same element twice. Output the indices in ascending order.",
                        "The first line contains two integers n and k (2 <= n <= 10^5, -10^9 <= k <= 10^9).\nThe second line contains n space-separated integers a_1, a_2, ..., a_n (-10^9 <= a_i <= 10^9).",
                        "Print two space-separated 1-based indices in ascending order (i j with i < j).",
                        "2 <= n <= 10^5\n-10^9 <= k, a_i <= 10^9",
                        List.of(
                                new TestCase("4 9\n2 7 11 15", "1 2", true, "a[1] + a[2] = 2 + 7 = 9. Output indices 1 2."),
                                new TestCase("3 6\n3 2 4", "2 3", true, "a[2] + a[3] = 2 + 4 = 6."),
                                new TestCase("2 10\n5 5", "1 2", false),
                                new TestCase("5 0\n-3 4 3 90 2", "1 3", false)
                        ),
                        "Use a hash table or two pointers on a sorted array with index tracking to achieve O(n) or O(n log n)."
                ),
                new DsaProblem(
                        "ARR-102",
                        topic,
                        topicTitle,
                        "B. Maximum Subarray Sum (Kadane's)",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Given an array of n integers, find the contiguous subarray (containing at least one number) which has the largest sum and return its sum.",
                        "The first line contains a single integer n (1 <= n <= 2 * 10^5).\nThe second line contains n space-separated integers a_1, a_2, ..., a_n (-10^4 <= a_i <= 10^4).",
                        "Print a single integer — the maximum contiguous subarray sum.",
                        "1 <= n <= 2 * 10^5\n-10^4 <= a_i <= 10^4",
                        List.of(
                                new TestCase("9\n-2 1 -3 4 -1 2 1 -5 4", "6", true, "The subarray [4, -1, 2, 1] has the maximum sum of 6."),
                                new TestCase("1\n-5", "-5", false),
                                new TestCase("5\n5 4 -1 7 8", "23", false),
                                new TestCase("4\n-2 -3 -1 -5", "-1", false)
                        ),
                        "Kadane's algorithm keeps a running maxEndingHere and globalMax in O(n) time and O(1) auxiliary space."
                ),
                new DsaProblem(
                        "ARR-103",
                        topic,
                        topicTitle,
                        "C. Rotate Array by K Steps",
                        Difficulty.MEDIUM,
                        1000,
                        256,
                        "Given an array of n integers, rotate the array to the right by k steps, where k is non-negative. For example, with n = 7 and k = 3, [1, 2, 3, 4, 5, 6, 7] becomes [5, 6, 7, 1, 2, 3, 4].",
                        "The first line contains two integers n and k (1 <= n <= 10^5, 0 <= k <= 10^9).\nThe second line contains n space-separated integers a_1, a_2, ..., a_n (-10^9 <= a_i <= 10^9).",
                        "Print the n integers after rotation, separated by a single space.",
                        "1 <= n <= 10^5\n0 <= k <= 10^9\n-10^9 <= a_i <= 10^9",
                        List.of(
                                new TestCase("7 3\n1 2 3 4 5 6 7", "5 6 7 1 2 3 4", true, "Rotating 3 steps to the right shifts elements accordingly."),
                                new TestCase("4 2\n-1 -100 3 99", "3 99 -1 -100", false),
                                new TestCase("5 5\n10 20 30 40 50", "10 20 30 40 50", false),
                                new TestCase("3 4\n1 2 3", "3 1 2", false)
                        ),
                        "Take k = k % n, then reverse the entire array, reverse the first k elements, and reverse the remaining n - k elements."
                ),
                new DsaProblem(
                        "ARR-104",
                        topic,
                        topicTitle,
                        "D. Merge Overlapping Intervals",
                        Difficulty.MEDIUM,
                        1000,
                        256,
                        "Given an array of n closed intervals [start_i, end_i], merge all overlapping intervals, and output the non-overlapping intervals that cover all intervals in the input, sorted in ascending order of start time.",
                        "The first line contains an integer n (1 <= n <= 10^5).\nThe following n lines each contain two integers start_i and end_i (0 <= start_i <= end_i <= 10^9).",
                        "Print each merged interval on a new line: start and end separated by a single space.",
                        "1 <= n <= 10^5\n0 <= start_i <= end_i <= 10^9",
                        List.of(
                                new TestCase("4\n1 3\n2 6\n8 10\n15 18", "1 6\n8 10\n15 18", true, "[1, 3] and [2, 6] overlap and merge into [1, 6]."),
                                new TestCase("2\n1 4\n4 5", "1 5", true, "Intervals [1, 4] and [4, 5] touch at 4, merging into [1, 5]."),
                                new TestCase("3\n1 10\n2 3\n4 8", "1 10", false),
                                new TestCase("1\n5 9", "5 9", false)
                        ),
                        "Sort the intervals by their start time. Iterate through and expand the end boundary if current start <= previous end."
                ),
                new DsaProblem(
                        "ARR-105",
                        topic,
                        topicTitle,
                        "E. Trapping Rain Water",
                        Difficulty.HARD,
                        1500,
                        256,
                        "Given n non-negative integers representing an elevation map where the width of each bar is 1, compute how much water it can trap after raining.",
                        "The first line contains a single integer n (1 <= n <= 10^5).\nThe second line contains n space-separated integers height_1, height_2, ..., height_n (0 <= height_i <= 10^5).",
                        "Print a single integer — total units of water trapped.",
                        "1 <= n <= 10^5\n0 <= height_i <= 10^5",
                        List.of(
                                new TestCase("12\n0 1 0 2 1 0 1 3 2 1 2 1", "6", true, "Water trapped between peaks sums to 6 units."),
                                new TestCase("6\n4 2 0 3 2 5", "9", true, "Total trapped water is 9."),
                                new TestCase("3\n3 0 2", "2", false),
                                new TestCase("4\n1 2 3 4", "0", false)
                        ),
                        "Two-pointer approach: track leftMax and rightMax from both ends in O(n) time and O(1) space."
                )
        );
    }

    // =========================================================================
    // DATA STRUCTURES - TOPIC 2: LINKED LISTS (5 Problems)
    // =========================================================================
    private static List<DsaProblem> buildLinkedListProblems() {
        String topic = "linked lists";
        String topicTitle = "Data Structures: Linked Lists";

        return List.of(
                new DsaProblem(
                        "LL-101",
                        topic,
                        topicTitle,
                        "A. Singly Linked List Reversal",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Given the head of a singly linked list containing n integer nodes, reverse the list pointers in-place and output the sequence of node values in the new order.",
                        "The first line contains an integer n (1 <= n <= 10^5).\nThe second line contains n space-separated integers representing the node values from head to tail.",
                        "Print the n integers in the reversed order separated by a single space.",
                        "1 <= n <= 10^5\n-10^9 <= val_i <= 10^9",
                        List.of(
                                new TestCase("5\n1 2 3 4 5", "5 4 3 2 1", true, "Reversing 1->2->3->4->5 yields 5->4->3->2->1."),
                                new TestCase("2\n1 2", "2 1", false),
                                new TestCase("1\n100", "100", false),
                                new TestCase("4\n-10 0 20 -30", "-30 20 0 -10", false)
                        ),
                        "Iteratively maintain prev, curr, and next pointers to reverse each link in O(n) time and O(1) auxiliary space."
                ),
                new DsaProblem(
                        "LL-102",
                        topic,
                        topicTitle,
                        "B. Middle of the Linked List",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Given the head of a singly linked list with n nodes, find the middle node value using the slow and fast pointer (Tortoise and Hare) technique. If there are two middle nodes (i.e. n is even), return the second middle node.",
                        "The first line contains an integer n (1 <= n <= 10^5).\nThe second line contains n space-separated integers.",
                        "Print a single integer — the value of the middle node.",
                        "1 <= n <= 10^5\n-10^9 <= val_i <= 10^9",
                        List.of(
                                new TestCase("5\n1 2 3 4 5", "3", true, "Odd length list [1, 2, 3, 4, 5]: middle is 3."),
                                new TestCase("6\n1 2 3 4 5 6", "4", true, "Even length list [1, 2, 3, 4, 5, 6]: second middle is 4."),
                                new TestCase("1\n42", "42", false),
                                new TestCase("2\n10 20", "20", false)
                        ),
                        "Advance slow by 1 step and fast by 2 steps until fast reaches the end."
                ),
                new DsaProblem(
                        "LL-103",
                        topic,
                        topicTitle,
                        "C. Merge Two Sorted Lists",
                        Difficulty.MEDIUM,
                        1000,
                        256,
                        "You are given the heads of two sorted linked lists list1 and list2 of sizes n and m. Merge the two lists into one sorted linked list. The list should be made by splicing together the nodes of the first two lists.",
                        "The first line contains two integers n and m (0 <= n, m <= 10^5, n + m >= 1).\nThe second line contains n space-separated sorted integers.\nThe third line contains m space-separated sorted integers.",
                        "Print n + m space-separated integers in non-decreasing order.",
                        "0 <= n, m <= 10^5\n1 <= n + m <= 2 * 10^5\n-10^9 <= val <= 10^9",
                        List.of(
                                new TestCase("3 3\n1 2 4\n1 3 4", "1 1 2 3 4 4", true, "Merged sorted elements: 1 1 2 3 4 4."),
                                new TestCase("1 2\n5\n2 8", "2 5 8", false),
                                new TestCase("3 2\n-5 0 10\n-2 4", "-5 -2 0 4 10", false)
                        ),
                        "Use a dummy head pointer and compare current values of list1 and list2."
                ),
                new DsaProblem(
                        "LL-104",
                        topic,
                        topicTitle,
                        "D. Remove N-th Node From End of List",
                        Difficulty.MEDIUM,
                        1000,
                        256,
                        "Given the head of a linked list of size L, remove the k-th node from the end of the list and output the remaining nodes.",
                        "The first line contains two integers L and k (1 <= k <= L <= 10^5).\nThe second line contains L space-separated integers.",
                        "Print the remaining L - 1 integers separated by space. If the resulting list is empty, print nothing.",
                        "1 <= k <= L <= 10^5\n-10^9 <= val_i <= 10^9",
                        List.of(
                                new TestCase("5 2\n1 2 3 4 5", "1 2 3 5", true, "The 2nd node from the end is 4. Removing it leaves [1, 2, 3, 5]."),
                                new TestCase("1 1\n1", "", false),
                                new TestCase("2 1\n1 2", "1", false),
                                new TestCase("4 4\n10 20 30 40", "20 30 40", false)
                        ),
                        "Use two pointers separated by k steps. When the lead pointer reaches the end, the trailing pointer is right before the target."
                ),
                new DsaProblem(
                        "LL-105",
                        topic,
                        topicTitle,
                        "E. Reverse Nodes in K-Group",
                        Difficulty.HARD,
                        1500,
                        256,
                        "Given the head of a linked list of n nodes, reverse the nodes of the list k at a time, and return the modified list. k is a positive integer and is less than or equal to the length of the linked list. If the number of nodes is not a multiple of k then left-out nodes, in the end, should remain as they are.",
                        "The first line contains two integers n and k (1 <= k <= n <= 10^5).\nThe second line contains n space-separated integers.",
                        "Print n space-separated integers representing the list after k-group reversals.",
                        "1 <= k <= n <= 10^5\n-10^9 <= val_i <= 10^9",
                        List.of(
                                new TestCase("5 2\n1 2 3 4 5", "2 1 4 3 5", true, "Reversing first group of 2 [1, 2] -> [2, 1]. Next [3, 4] -> [4, 3]. Leftover [5] stays."),
                                new TestCase("5 3\n1 2 3 4 5", "3 2 1 4 5", true, "Reversing first 3 [1, 2, 3] -> [3, 2, 1]. Leftover [4, 5] remains."),
                                new TestCase("4 2\n10 20 30 40", "20 10 40 30", false),
                                new TestCase("3 1\n1 2 3", "1 2 3", false)
                        ),
                        "Count k nodes ahead before performing an in-place group reversal, connecting previous group tails seamlessly."
                )
        );
    }

    // =========================================================================
    // DATA STRUCTURES - TOPIC 3: STACKS (5 Problems)
    // =========================================================================
    private static List<DsaProblem> buildStackProblems() {
        String topic = "stacks";
        String topicTitle = "Data Structures: Stacks";

        return List.of(
                new DsaProblem(
                        "STK-101",
                        topic,
                        topicTitle,
                        "A. Balanced Parentheses & Brackets Hierarchy",
                        Difficulty.EASY,
                        1000,
                        256,
                        "A bracket sequence consisting of '(', ')', '[', ']', '{', '}' is called valid if every opening bracket has an exact matching closing bracket of the same type in the correct LIFO order. Given a sequence of brackets, determine if it is valid.",
                        "The first line contains a single string s (1 <= |s| <= 10^5) containing only '(', ')', '[', ']', '{', '}'.",
                        "Print \"YES\" (without quotes) if the sequence is valid, or \"NO\" otherwise.",
                        "1 <= |s| <= 10^5",
                        List.of(
                                new TestCase("{[()]}", "YES", true, "All brackets match in proper nesting order."),
                                new TestCase("{[(])}", "NO", true, "The closing ']' precedes the closing ')'."),
                                new TestCase("(", "NO", false),
                                new TestCase("()[]{}", "YES", false),
                                new TestCase("(((((((((())))))))))", "YES", false),
                                new TestCase("(((((((((()))))))))", "NO", false)
                        ),
                        "Use a LIFO stack to push opening brackets and pop matching ones."
                ),
                new DsaProblem(
                        "STK-102",
                        topic,
                        topicTitle,
                        "B. Next Greater Element",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Given an array of n integers, for each element find the first element to its right that is strictly greater than it. If no such element exists, output -1 for that position.",
                        "The first line contains an integer n (1 <= n <= 10^5).\nThe second line contains n space-separated integers a_1, a_2, ..., a_n (-10^9 <= a_i <= 10^9).",
                        "Print n space-separated integers where the i-th integer is the next greater element for a_i.",
                        "1 <= n <= 10^5\n-10^9 <= a_i <= 10^9",
                        List.of(
                                new TestCase("4\n4 5 2 25", "5 25 25 -1", true, "For 4: 5. For 5: 25. For 2: 25. For 25: -1."),
                                new TestCase("4\n13 7 6 12", "-1 12 12 -1", false),
                                new TestCase("3\n1 2 3", "2 3 -1", false),
                                new TestCase("3\n3 2 1", "-1 -1 -1", false)
                        ),
                        "Traverse from right to left using a monotonic decreasing stack."
                ),
                new DsaProblem(
                        "STK-103",
                        topic,
                        topicTitle,
                        "C. Min Stack Design Simulation",
                        Difficulty.MEDIUM,
                        1000,
                        256,
                        "Design a stack that supports push, pop, top, and retrieving the minimum element in constant O(1) time.\nProcess q operations:\n- 1 x: Push element x onto the stack.\n- 2: Pop the top element from the stack.\n- 3: Print the top element.\n- 4: Print the minimum element currently in the stack.",
                        "The first line contains an integer q (1 <= q <= 10^5).\nThe next q lines describe the operations (it is guaranteed operations 2, 3, 4 are only called on a non-empty stack).",
                        "For each operation of type 3 and 4, print the answer on a new line.",
                        "1 <= q <= 10^5\n-10^9 <= x <= 10^9",
                        List.of(
                                new TestCase("7\n1 -2\n1 0\n1 -3\n4\n2\n3\n4", "-3\n0\n-2", true, "Pushed -2, 0, -3. Min is -3. Pop -3. Top is 0. Min is -2."),
                                new TestCase("4\n1 5\n4\n1 2\n4", "5\n2", false),
                                new TestCase("5\n1 10\n1 20\n3\n2\n3", "20\n10", false)
                        ),
                        "Store pairs (value, minSoFar) or maintain a parallel min-tracker stack."
                ),
                new DsaProblem(
                        "STK-104",
                        topic,
                        topicTitle,
                        "D. Evaluate Reverse Polish Notation",
                        Difficulty.MEDIUM,
                        1000,
                        256,
                        "You are given an array of strings tokens representing an arithmetic expression in Reverse Polish Notation (postfix). Evaluate the expression and return an integer representing its value. Valid operators are '+', '-', '*', and '/'. Division truncates toward zero.",
                        "The first line contains an integer m (1 <= m <= 10^5) — the number of tokens.\nThe second line contains m space-separated tokens.",
                        "Print a single integer — the evaluated value of the expression.",
                        "1 <= m <= 10^5\nEach token is either an operator or an integer in [-10^4, 10^4]",
                        List.of(
                                new TestCase("5\n2 1 + 3 *", "9", true, "((2 + 1) * 3) = 9."),
                                new TestCase("5\n4 13 5 / +", "6", true, "(4 + (13 / 5)) = 4 + 2 = 6."),
                                new TestCase("13\n10 6 9 3 + -11 * / * 17 + 5 +", "22", false),
                                new TestCase("3\n5 3 -", "2", false)
                        ),
                        "Iterate through tokens: push operands onto stack; when an operator is encountered, pop two operands, apply operator, and push result back."
                ),
                new DsaProblem(
                        "STK-105",
                        topic,
                        topicTitle,
                        "E. Largest Rectangle in Histogram",
                        Difficulty.HARD,
                        1500,
                        256,
                        "Given an array of integers heights of length n representing the histogram's bar height where the width of each bar is 1, return the area of the largest rectangle in the histogram.",
                        "The first line contains an integer n (1 <= n <= 10^5).\nThe second line contains n space-separated integers heights_1, heights_2, ..., heights_n (0 <= heights_i <= 10^9).",
                        "Print a single integer — the maximum area of a rectangle that can be formed within the histogram.",
                        "1 <= n <= 10^5\n0 <= heights_i <= 10^9",
                        List.of(
                                new TestCase("6\n2 1 5 6 2 3", "10", true, "Bars [5, 6] can form a rectangle of height 5 and width 2: area = 10."),
                                new TestCase("2\n2 4", "4", false),
                                new TestCase("5\n1 1 1 1 1", "5", false),
                                new TestCase("4\n6 2 5 4", "12", false)
                        ),
                        "Use a monotonic increasing stack storing bar indices to calculate the maximum rectangular area spanning left and right in O(n) time."
                )
        );
    }

    // =========================================================================
    // DATA STRUCTURES - TOPIC 4: QUEUES & DEQUES (5 Problems)
    // =========================================================================
    private static List<DsaProblem> buildQueueProblems() {
        String topic = "queues";
        String topicTitle = "Data Structures: Queues & Deques";

        return List.of(
                new DsaProblem(
                        "QUE-101",
                        topic,
                        topicTitle,
                        "A. Implement Queue using Two Stacks",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Implement a First-In-First-Out (FIFO) queue using two stacks. You must handle q queries of two types:\n- 1 x: Enqueue element x to the back of the queue.\n- 2: Dequeue the element from the front of the queue and print it.",
                        "The first line contains an integer q (1 <= q <= 10^5).\nThe following q lines describe queries: \"1 x\" or \"2\". It is guaranteed type 2 queries are only called on a non-empty queue.",
                        "For each query of type 2, print the dequeued element on a new line.",
                        "1 <= q <= 10^5\n-10^9 <= x <= 10^9",
                        List.of(
                                new TestCase("5\n1 10\n1 20\n2\n1 30\n2", "10\n20", true, "Enqueued 10, 20. First dequeue returns 10. Enqueue 30. Second dequeue returns 20."),
                                new TestCase("3\n1 5\n1 8\n2", "5", false),
                                new TestCase("4\n1 1\n2\n1 2\n2", "1\n2", false)
                        ),
                        "Push to stack1 on enqueue. For dequeue, if stack2 is empty, pop all elements from stack1 into stack2, then pop stack2."
                ),
                new DsaProblem(
                        "QUE-102",
                        topic,
                        topicTitle,
                        "B. First Non-Repeating Character in Stream",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Given a stream of n lowercase characters as a string s, after reading each character, find the first non-repeating character seen so far. If no non-repeating character exists, output '#' for that character position.",
                        "The first line contains a single string s (1 <= |s| <= 10^5) of lowercase English characters.",
                        "Print a string of length |s| where the i-th character represents the first non-repeating character after processing the prefix s[0..i].",
                        "1 <= |s| <= 10^5",
                        List.of(
                                new TestCase("aabc", "a#bb", true, "After 'a': 'a'. After 'a': no unique, '#'. After 'b': 'b'. After 'c': 'b' is still first unique."),
                                new TestCase("zz", "z#", false),
                                new TestCase("abc", "aaa", false),
                                new TestCase("racecar", "rrrrrrc", false)
                        ),
                        "Use a FIFO queue to store characters in insertion order and a frequency array. Pop from the front while frequency > 1."
                ),
                new DsaProblem(
                        "QUE-103",
                        topic,
                        topicTitle,
                        "C. Circular Queue Buffer Simulation",
                        Difficulty.MEDIUM,
                        1000,
                        256,
                        "Design your implementation of a circular queue of fixed capacity k. Process q operations:\n- ENQUEUE x: Inserts an element into the circular queue. Print \"true\" if successful, else \"false\".\n- DEQUEUE: Deletes an element from the circular queue. Print \"true\" if successful, else \"false\".\n- FRONT: Gets the front item. Print the value, or -1 if empty.\n- REAR: Gets the last item. Print the value, or -1 if empty.",
                        "The first line contains two integers k and q (1 <= k <= 10^4, 1 <= q <= 10^5).\nThe next q lines each contain an operation: \"ENQUEUE x\", \"DEQUEUE\", \"FRONT\", or \"REAR\".",
                        "For each operation, print the required output on a new line.",
                        "1 <= k <= 10^4\n1 <= q <= 10^5\n-10^9 <= x <= 10^9",
                        List.of(
                                new TestCase("3 6\nENQUEUE 1\nENQUEUE 2\nENQUEUE 3\nENQUEUE 4\nREAR\nFRONT", "true\ntrue\ntrue\nfalse\n3\n1", true, "Queue has capacity 3. Enqueue 4 fails (queue full). Rear is 3, front is 1."),
                                new TestCase("2 4\nENQUEUE 5\nDEQUEUE\nFRONT\nREAR", "true\ntrue\n-1\n-1", false)
                        ),
                        "Maintain head, tail, and size pointers modulo k."
                ),
                new DsaProblem(
                        "QUE-104",
                        topic,
                        topicTitle,
                        "D. Sliding Window Maximum (Monotonic Deque)",
                        Difficulty.MEDIUM,
                        1000,
                        256,
                        "You are given an array of n integers and a sliding window of size k which moves from the very left of the array to the very right. You can only see the k numbers in the window. Each time the sliding window moves right by one position, output the maximum number in the current window.",
                        "The first line contains two integers n and k (1 <= k <= n <= 10^5).\nThe second line contains n space-separated integers a_1, a_2, ..., a_n (-10^9 <= a_i <= 10^9).",
                        "Print n - k + 1 space-separated integers representing the maximum in each window from left to right.",
                        "1 <= k <= n <= 10^5\n-10^9 <= a_i <= 10^9",
                        List.of(
                                new TestCase("8 3\n1 3 -1 -3 5 3 6 7", "3 3 5 5 6 7", true, "Windows: [1, 3, -1]->3, [3, -1, -3]->3, [-1, -3, 5]->5, [-3, 5, 3]->5, [5, 3, 6]->6, [3, 6, 7]->7."),
                                new TestCase("1 1\n1", "1", false),
                                new TestCase("4 2\n9 11 8 5", "11 11 8", false),
                                new TestCase("5 3\n5 4 3 2 1", "5 4 3", false)
                        ),
                        "Use a double-ended queue (deque) storing indices, maintaining elements in monotonically decreasing order."
                ),
                new DsaProblem(
                        "QUE-105",
                        topic,
                        topicTitle,
                        "E. Gas Station Circular Tour",
                        Difficulty.HARD,
                        1500,
                        256,
                        "There are n gas stations along a circular route, where the amount of gas at the i-th station is gas[i]. You have a car with an unlimited gas tank and it costs cost[i] of gas to travel from the i-th station to its next (i + 1)-th station. You begin the journey with an empty tank at one of the gas stations. Return the starting gas station's 0-based index if you can travel around the circuit once in the clockwise direction, otherwise return -1. If there exists a solution, it is guaranteed to be unique.",
                        "The first line contains an integer n (1 <= n <= 10^5).\nThe second line contains n space-separated integers gas[0], ..., gas[n-1] (0 <= gas[i] <= 10^4).\nThe third line contains n space-separated integers cost[0], ..., cost[n-1] (0 <= cost[i] <= 10^4).",
                        "Print the starting gas station's index (0 to n - 1), or -1 if impossible.",
                        "1 <= n <= 10^5\n0 <= gas[i], cost[i] <= 10^4",
                        List.of(
                                new TestCase("5\n1 2 3 4 5\n3 4 5 1 2", "3", true, "Start at station 3 (gas=4, cost=1). You can complete the circuit with gas remaining."),
                                new TestCase("3\n2 3 4\n3 4 3", "-1", true, "Total gas = 9 < total cost = 10, impossible to complete circuit."),
                                new TestCase("1\n5\n4", "0", false),
                                new TestCase("4\n4 6 7 4\n6 5 3 5", "1", false)
                        ),
                        "If total gas < total cost, return -1. Otherwise, reset start index to i + 1 whenever running surplus drops below zero."
                )
        );
    }

    // =========================================================================
    // DATA STRUCTURES - TOPIC 5: HASH MAPS & SETS (5 Problems)
    // =========================================================================
    private static List<DsaProblem> buildHashMapProblems() {
        String topic = "hash maps";
        String topicTitle = "Data Structures: Hash Maps & Sets";

        return List.of(
                new DsaProblem(
                        "HASH-101",
                        topic,
                        topicTitle,
                        "A. First Non-Repeating Element in Array",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Given an array of n integers, find the first non-repeating element (an element that occurs only once). If all elements repeat, print -1.",
                        "The first line contains an integer n (1 <= n <= 10^5).\nThe second line contains n space-separated integers a_1, a_2, ..., a_n (-10^9 <= a_i <= 10^9).",
                        "Print the first non-repeating integer, or -1 if none exists.",
                        "1 <= n <= 10^5\n-10^9 <= a_i <= 10^9",
                        List.of(
                                new TestCase("6\n4 5 1 2 0 4", "5", true, "4 occurs twice. 5 is the first element occurring only once."),
                                new TestCase("4\n1 2 1 2", "-1", false),
                                new TestCase("5\n9 9 9 9 3", "3", false),
                                new TestCase("1\n42", "42", false)
                        ),
                        "Use a hash map to count frequencies in a first pass, then iterate through the array in order to find the first element with count 1."
                ),
                new DsaProblem(
                        "HASH-102",
                        topic,
                        topicTitle,
                        "B. Subarray Sum Equals K",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Given an array of n integers and an integer k, return the total number of continuous subarrays whose sum equals to k.",
                        "The first line contains two integers n and k (1 <= n <= 10^5, -10^9 <= k <= 10^9).\nThe second line contains n space-separated integers a_1, a_2, ..., a_n (-10^4 <= a_i <= 10^4).",
                        "Print a single integer — the total number of subarrays whose sum is k.",
                        "1 <= n <= 10^5\n-10^9 <= k <= 10^9\n-10^4 <= a_i <= 10^4",
                        List.of(
                                new TestCase("3 2\n1 1 1", "2", true, "Subarrays [1, 1] at indices [0, 1] and [1, 2] both sum to 2."),
                                new TestCase("3 3\n1 2 3", "2", false),
                                new TestCase("5 0\n0 0 0 0 0", "15", false),
                                new TestCase("4 -2\n1 -1 -2 0", "3", false)
                        ),
                        "Maintain prefix sums and store their frequencies in a Hash Map to count prefixSum - k in O(1)."
                ),
                new DsaProblem(
                        "HASH-103",
                        topic,
                        topicTitle,
                        "C. Group Anagrams",
                        Difficulty.MEDIUM,
                        1000,
                        256,
                        "Given an array of n strings containing lowercase English letters, group the anagrams together. For deterministic output, words within each group should be sorted alphabetically, and the groups should be printed in alphabetical order of their first word.",
                        "The first line contains an integer n (1 <= n <= 10^4).\nThe second line contains n space-separated strings (each length 1 to 100).",
                        "Print each anagram group on a new line, with words separated by a single space.",
                        "1 <= n <= 10^4\n1 <= |s_i| <= 100",
                        List.of(
                                new TestCase("6\neat tea tan ate nat bat", "ate eat tea\nbat\nnat tan", true, "The groups are [ate, eat, tea], [bat], and [nat, tan]."),
                                new TestCase("1\na", "a", false),
                                new TestCase("2\nab ba", "ab ba", false)
                        ),
                        "Sort the characters of each string to use as the hash map key, storing matching words in a list."
                ),
                new DsaProblem(
                        "HASH-104",
                        topic,
                        topicTitle,
                        "D. Longest Consecutive Elements Sequence",
                        Difficulty.MEDIUM,
                        1000,
                        256,
                        "Given an unsorted array of integers nums of size n, return the length of the longest consecutive elements sequence (e.g. [1, 2, 3, 4]). You must write an algorithm that runs in O(n) time.",
                        "The first line contains an integer n (1 <= n <= 10^5).\nThe second line contains n space-separated integers nums_1, nums_2, ..., nums_n (-10^9 <= nums_i <= 10^9).",
                        "Print a single integer — the length of the longest consecutive elements sequence.",
                        "1 <= n <= 10^5\n-10^9 <= nums_i <= 10^9",
                        List.of(
                                new TestCase("6\n100 4 200 1 3 2", "4", true, "The longest consecutive sequence is [1, 2, 3, 4] with length 4."),
                                new TestCase("10\n0 3 7 2 5 8 4 6 0 1", "9", true, "Sequence is [0, 1, 2, 3, 4, 5, 6, 7, 8], length 9."),
                                new TestCase("1\n50", "1", false),
                                new TestCase("4\n10 20 30 40", "1", false)
                        ),
                        "Insert all numbers into a HashSet. Only start counting consecutive streaks from x if x - 1 is not in the set."
                ),
                new DsaProblem(
                        "HASH-105",
                        topic,
                        topicTitle,
                        "E. Four Sum Count II (Quadruple Sum Zero)",
                        Difficulty.HARD,
                        1500,
                        256,
                        "Given four integer arrays A, B, C, and D all of length n, return the number of tuples (i, j, k, l) such that 0 <= i, j, k, l < n and A[i] + B[j] + C[k] + D[l] == 0.",
                        "The first line contains an integer n (1 <= n <= 500).\nThe next 4 lines each contain n space-separated integers for arrays A, B, C, and D respectively (-10^7 <= val <= 10^7).",
                        "Print a single integer — the number of tuples summing to zero.",
                        "1 <= n <= 500\n-10^7 <= val <= 10^7",
                        List.of(
                                new TestCase("2\n1 2\n-2 -1\n-1 2\n0 2", "2", true, "Two valid tuples: (0, 0, 0, 1) -> 1 + (-2) + (-1) + 2 = 0, and (1, 1, 0, 0) -> 2 + (-1) + (-1) + 0 = 0."),
                                new TestCase("1\n0\n0\n0\n0", "1", false),
                                new TestCase("2\n1 1\n1 1\n-1 -1\n-1 -1", "16", false)
                        ),
                        "Store all pairwise sums A[i] + B[j] in a Hash Map with their frequencies in O(n^2), then iterate through all C[k] + D[l] checking for -(C[k] + D[l])."
                )
        );
    }

    // =========================================================================
    // DATA STRUCTURES - TOPIC 6: HEAPS & PRIORITY QUEUES (5 Problems)
    // =========================================================================
    private static List<DsaProblem> buildHeapProblems() {
        String topic = "heaps";
        String topicTitle = "Data Structures: Heaps & Priority Queues";

        return List.of(
                new DsaProblem(
                        "HEAP-101",
                        topic,
                        topicTitle,
                        "A. K-th Largest Element in an Array",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Given an integer array nums of size n and an integer k, return the k-th largest element in the array using a Min-Heap.",
                        "The first line contains two integers n and k (1 <= k <= n <= 10^5).\nThe second line contains n space-separated integers nums_1, nums_2, ..., nums_n (-10^9 <= nums_i <= 10^9).",
                        "Print a single integer — the k-th largest element.",
                        "1 <= k <= n <= 10^5\n-10^9 <= nums_i <= 10^9",
                        List.of(
                                new TestCase("6 2\n3 2 1 5 6 4", "5", true, "The 2nd largest element in sorted order [6, 5, 4, 3, 2, 1] is 5."),
                                new TestCase("9 4\n3 2 3 1 2 4 5 5 6", "4", false),
                                new TestCase("1 1\n10", "10", false),
                                new TestCase("5 5\n7 10 4 3 20", "3", false)
                        ),
                        "Maintain a Min-Heap of size k. For each incoming element, if heap size exceeds k, poll the minimum."
                ),
                new DsaProblem(
                        "HEAP-102",
                        topic,
                        topicTitle,
                        "B. Minimum Cost to Connect Wooden Ropes",
                        Difficulty.EASY,
                        1000,
                        256,
                        "There are given n ropes of different lengths. You need to connect all these ropes into one rope. The cost to connect two ropes is equal to sum of their lengths. Find the minimum total cost to connect all the ropes.",
                        "The first line contains an integer n (1 <= n <= 10^5).\nThe second line contains n space-separated integers representing rope lengths (1 <= len_i <= 10^5).",
                        "Print a single integer — the minimum cost to connect all ropes.",
                        "1 <= n <= 10^5\n1 <= len_i <= 10^5",
                        List.of(
                                new TestCase("4\n4 3 2 6", "29", true, "Connect 2 and 3 (cost 5, ropes=[4, 6, 5]). Connect 4 and 5 (cost 9, ropes=[6, 9]). Connect 6 and 9 (cost 15). Total = 5 + 9 + 15 = 29."),
                                new TestCase("5\n4 2 7 6 9", "62", false),
                                new TestCase("2\n1 2", "3", false),
                                new TestCase("1\n5", "0", false)
                        ),
                        "Use a Min-Heap (Huffman Coding principle). Repeatedly extract the two smallest elements and push back their sum."
                ),
                new DsaProblem(
                        "HEAP-103",
                        topic,
                        topicTitle,
                        "C. Top K Frequent Elements",
                        Difficulty.MEDIUM,
                        1000,
                        256,
                        "Given an integer array of size n, find the k most frequent elements. If two elements have the same frequency, output the smaller element first. The results should be sorted in descending order of frequency; ties broken by ascending numerical value.",
                        "The first line contains two integers n and k (1 <= k <= n <= 10^5).\nThe second line contains n space-separated integers a_1, a_2, ..., a_n (-10^5 <= a_i <= 10^5).",
                        "Print k space-separated integers representing the top k elements according to the specified ordering.",
                        "1 <= k <= n <= 10^5\n-10^5 <= a_i <= 10^5",
                        List.of(
                                new TestCase("6 2\n1 1 1 2 2 3", "1 2", true, "1 appears 3 times, 2 appears 2 times, 3 appears 1 time. Top 2 are 1 and 2."),
                                new TestCase("7 3\n4 4 2 2 3 3 1", "2 3 4", true, "Frequencies: 4 (2), 2 (2), 3 (2), 1 (1). Tied frequencies sorted ascending gives 2 3 4."),
                                new TestCase("1 1\n99", "99", false),
                                new TestCase("8 2\n-5 -5 -5 10 10 20 20 20", "-5 20", false)
                        ),
                        "Count frequencies with a Hash Map, then maintain a Min-Heap or sort entries in O(n log k)."
                ),
                new DsaProblem(
                        "HEAP-104",
                        topic,
                        topicTitle,
                        "D. Merge K Sorted Arrays",
                        Difficulty.MEDIUM,
                        1500,
                        256,
                        "You are given k sorted arrays, each containing n integers. Merge all k sorted arrays into a single sorted array using a Min-Heap.",
                        "The first line contains two integers k and n (1 <= k, n <= 500).\nThe next k lines each contain n space-separated integers in ascending order.",
                        "Print k * n space-separated integers in non-decreasing order.",
                        "1 <= k, n <= 500\n-10^9 <= val <= 10^9",
                        List.of(
                                new TestCase("3 3\n1 4 7\n2 5 8\n3 6 9", "1 2 3 4 5 6 7 8 9", true, "Merged 3 arrays of 3 elements into 1 sorted sequence."),
                                new TestCase("2 2\n1 10\n2 5", "1 2 5 10", false),
                                new TestCase("1 3\n2 4 6", "2 4 6", false)
                        ),
                        "Store elements with their array index in a Min-Heap of size k. Poll the min, append to output, and push the next element from that array."
                ),
                new DsaProblem(
                        "HEAP-105",
                        topic,
                        topicTitle,
                        "E. Running Stream Median",
                        Difficulty.HARD,
                        2000,
                        256,
                        "You receive a continuous stream of n integers. After reading each integer, you must output the current median of all integers seen so far. If the total number of integers is odd, the median is the middle element. If even, the median is the average of the two middle elements, truncated down to the nearest integer (floor).",
                        "The first line contains an integer n (1 <= n <= 10^5) — the number of elements in the stream.\nThe second line contains n space-separated integers x_1, x_2, ..., x_n (0 <= x_i <= 10^9).",
                        "Print n space-separated integers, where the i-th integer is the floor of the median after reading x_i.",
                        "1 <= n <= 10^5\n0 <= x_i <= 10^9",
                        List.of(
                                new TestCase("5\n5 15 1 3 2", "5 10 5 4 3", true, "After 5: 5. After 15: avg(5,15)=10. After 1: 5. After 3: avg(3,5)=4. After 2: 3."),
                                new TestCase("4\n1 2 3 4", "1 1 2 2", false),
                                new TestCase("1\n100", "100", false),
                                new TestCase("6\n10 20 30 40 50 60", "10 15 20 25 30 35", false)
                        ),
                        "Use two heaps: a Max-Heap for the smaller half and a Min-Heap for the larger half, keeping them balanced in size."
                )
        );
    }

    // =========================================================================
    // DATA STRUCTURES - TOPIC 7: TREES & BINARY SEARCH TREES (5 Problems)
    // =========================================================================
    private static List<DsaProblem> buildTreeProblems() {
        String topic = "trees";
        String topicTitle = "Data Structures: Trees & BST";

        return List.of(
                new DsaProblem(
                        "TREE-101",
                        topic,
                        topicTitle,
                        "A. Binary Tree Inorder & Level-Order Traversal",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Given a binary tree represented as an array in level-order serialization where -1 represents a null node, construct the binary tree and print both its Inorder traversal and Level-Order traversal (excluding nulls).",
                        "The first line contains an integer n (1 <= n <= 10^4).\nThe second line contains n space-separated integers representing the level-order serialization (-1 indicates null).",
                        "Print two lines:\nLine 1: Inorder traversal node values separated by space.\nLine 2: Level-order traversal node values separated by space.",
                        "1 <= n <= 10^4\n-1 <= val_i <= 10^9",
                        List.of(
                                new TestCase("7\n1 2 3 4 5 -1 6", "4 2 5 1 3 6\n1 2 3 4 5 6", true, "Root 1, left child 2 (children 4, 5), right child 3 (right child 6)."),
                                new TestCase("3\n1 -1 2", "1 2\n1 2", false),
                                new TestCase("1\n42", "42\n42", false)
                        ),
                        "Construct tree using a queue for level-order parsing. Recursively traverse left-root-right for inorder."
                ),
                new DsaProblem(
                        "TREE-102",
                        topic,
                        topicTitle,
                        "B. Maximum Depth & Diameter of Binary Tree",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Given a binary tree serialized in level-order (-1 for null), compute both its maximum depth (number of nodes along the longest path from the root node down to the farthest leaf node) and its diameter (the length of the longest path between any two nodes in a tree, measured in edges).",
                        "The first line contains an integer n (1 <= n <= 10^4).\nThe second line contains n space-separated integers (-1 for null).",
                        "Print two space-separated integers: maximum depth and diameter.",
                        "1 <= n <= 10^4\n-1 <= val_i <= 10^9",
                        List.of(
                                new TestCase("5\n1 2 3 4 5", "3 3", true, "Depth is 3 (1->2->4). Diameter is 3 edges (4 to 5 via 2, or 4 to 3)."),
                                new TestCase("3\n1 2 -1", "2 1", false),
                                new TestCase("1\n1", "1 0", false),
                                new TestCase("7\n1 2 3 4 -1 -1 5", "3 4", false)
                        ),
                        "Compute height recursively; at each node, candidate diameter is leftHeight + rightHeight."
                ),
                new DsaProblem(
                        "TREE-103",
                        topic,
                        topicTitle,
                        "C. Validate Binary Search Tree (BST)",
                        Difficulty.MEDIUM,
                        1000,
                        256,
                        "Given the root of a binary tree serialized in level-order (-1 for null), determine if it is a valid Binary Search Tree (BST). A valid BST satisfies: left subtree contains only nodes with keys strictly less than the node's key; right subtree contains only nodes with keys strictly greater than the node's key; both subtrees must also be binary search trees.",
                        "The first line contains an integer n (1 <= n <= 10^5).\nThe second line contains n space-separated integers (-1 for null).",
                        "Print \"YES\" if the tree is a valid BST, or \"NO\" otherwise.",
                        "1 <= n <= 10^5\n-10^9 <= val_i <= 10^9",
                        List.of(
                                new TestCase("3\n2 1 3", "YES", true, "Root 2 has left child 1 and right child 3, strictly satisfying BST properties."),
                                new TestCase("5\n5 1 4 -1 -1 3 6", "NO", true, "Root 5 has right child 4 which is less than 5, violating BST."),
                                new TestCase("1\n10", "YES", false),
                                new TestCase("3\n2 2 2", "NO", false)
                        ),
                        "Verify with recursive range bounds (minVal, maxVal) or check that inorder traversal is strictly monotonically increasing."
                ),
                new DsaProblem(
                        "TREE-104",
                        topic,
                        topicTitle,
                        "D. Lowest Common Ancestor (LCA) in BST",
                        Difficulty.MEDIUM,
                        1000,
                        256,
                        "Given a valid Binary Search Tree (BST) serialized in level-order with unique values, and two values u and v present in the tree, find the value of their Lowest Common Ancestor (LCA).",
                        "The first line contains an integer n (1 <= n <= 10^5).\nThe second line contains n space-separated integers (-1 for null).\nThe third line contains two space-separated integers u and v.",
                        "Print a single integer — the value of the Lowest Common Ancestor.",
                        "1 <= n <= 10^5\n-10^9 <= val, u, v <= 10^9",
                        List.of(
                                new TestCase("7\n6 2 8 0 4 7 9\n2 8", "6", true, "The LCA of 2 and 8 is 6."),
                                new TestCase("7\n6 2 8 0 4 7 9\n2 4", "2", true, "The LCA of 2 and 4 is 2 since a node can be a descendant of itself."),
                                new TestCase("3\n2 1 3\n1 3", "2", false)
                        ),
                        "If both u and v are smaller than root, search left. If both are greater, search right. Otherwise, root is the LCA."
                ),
                new DsaProblem(
                        "TREE-105",
                        topic,
                        topicTitle,
                        "E. Binary Tree Zigzag Level Order Traversal",
                        Difficulty.HARD,
                        1500,
                        256,
                        "Given the root of a binary tree serialized in level-order (-1 for null), return the zigzag level order traversal of its nodes' values (i.e. from left to right, then right to left for the next level and alternate between).",
                        "The first line contains an integer n (1 <= n <= 10^5).\nThe second line contains n space-separated integers (-1 for null).",
                        "Print each level on a new line, with node values separated by a space.",
                        "1 <= n <= 10^5\n-10^9 <= val_i <= 10^9",
                        List.of(
                                new TestCase("7\n3 9 20 -1 -1 15 7", "3\n20 9\n15 7", true, "Level 0: [3]. Level 1 reversed: [20, 9]. Level 2: [15, 7]."),
                                new TestCase("1\n1", "1", false),
                                new TestCase("5\n1 2 3 4 5", "1\n3 2\n4 5", false)
                        ),
                        "Use BFS with a double-ended queue or reverse alternate levels based on the depth modulo 2."
                )
        );
    }

    // =========================================================================
    // DATA STRUCTURES - TOPIC 8: DISJOINT SET UNION (5 Problems)
    // =========================================================================
    private static List<DsaProblem> buildDsuProblems() {
        String topic = "dsu";
        String topicTitle = "Data Structures: Disjoint Set Union";

        return List.of(
                new DsaProblem(
                        "DSU-101",
                        topic,
                        topicTitle,
                        "A. Dynamic Network Connectivity & Queries",
                        Difficulty.EASY,
                        1000,
                        256,
                        "You are managing a dynamic communication network with n nodes labeled 1 to n. Initially, each node is in its own isolated cluster. You are given q queries of two types:\n- 1 u v: Merge the cluster containing node u with the cluster containing node v.\n- 2 u v: Check if node u and node v belong to the same cluster. Print \"YES\" or \"NO\".",
                        "The first line contains two integers n and q (1 <= n, q <= 2 * 10^5).\nThe following q lines each contain a query: \"1 u v\" or \"2 u v\" (1 <= u, v <= n).",
                        "For each query of type 2, print \"YES\" if u and v are connected, or \"NO\" otherwise on a new line.",
                        "1 <= n, q <= 2 * 10^5\n1 <= u, v <= n",
                        List.of(
                                new TestCase("5 6\n1 1 2\n1 2 3\n2 1 3\n2 1 4\n1 4 5\n2 3 5", "YES\nNO\nNO", true, "Nodes 1, 2, 3 merged. Query 2 1 3 -> YES. Query 2 1 4 -> NO. Nodes 4, 5 merged. Query 2 3 5 -> NO."),
                                new TestCase("3 3\n1 1 2\n1 2 3\n2 1 3", "YES", false),
                                new TestCase("4 2\n2 1 2\n2 3 4", "NO\nNO", false)
                        ),
                        "Implement Union-Find with path compression and rank optimization for nearly O(alpha(n)) per operation."
                ),
                new DsaProblem(
                        "DSU-102",
                        topic,
                        topicTitle,
                        "B. Number of Connected Components",
                        Difficulty.EASY,
                        1000,
                        256,
                        "You have a graph of n nodes labeled 1 to n. You are given an array of m undirected edges. Return the number of connected components in the graph using Disjoint Set Union.",
                        "The first line contains two integers n and m (1 <= n <= 10^5, 0 <= m <= 2 * 10^5).\nThe following m lines each contain two integers u and v (1 <= u, v <= n).",
                        "Print a single integer — the number of connected components.",
                        "1 <= n <= 10^5\n0 <= m <= 2 * 10^5\n1 <= u, v <= n",
                        List.of(
                                new TestCase("5 3\n1 2\n2 3\n4 5", "2", true, "Components are {1, 2, 3} and {4, 5}. Total = 2."),
                                new TestCase("5 0", "5", false),
                                new TestCase("4 3\n1 2\n2 3\n3 4", "1", false),
                                new TestCase("4 1\n1 2", "3", false)
                        ),
                        "Initialize count = n. For each edge, if find(u) != find(v), union(u, v) and decrement count by 1."
                ),
                new DsaProblem(
                        "DSU-103",
                        topic,
                        topicTitle,
                        "C. Redundant Connection (Cycle Edge Detection)",
                        Difficulty.MEDIUM,
                        1000,
                        256,
                        "In this problem, a tree is an undirected graph that is connected and has no cycles. You are given a graph that started as a tree with n nodes labeled 1 to n, with one additional edge added. The added edge has two different vertices chosen from 1 to n, and was not an edge that already existed. Given the sequence of n edges, return an edge that can be removed so that the resulting graph is a tree of n nodes. If there are multiple answers, return the edge that occurs last in the input.",
                        "The first line contains an integer n (3 <= n <= 10^5).\nThe following n lines each contain two integers u and v (1 <= u, v <= n).",
                        "Print two space-separated integers u and v representing the redundant edge.",
                        "3 <= n <= 10^5\n1 <= u, v <= n",
                        List.of(
                                new TestCase("3\n1 2\n1 3\n2 3", "2 3", true, "Edge 2-3 creates a cycle with 1-2 and 1-3."),
                                new TestCase("5\n1 2\n2 3\n3 4\n1 4\n1 5", "1 4", true, "Edge 1-4 creates a cycle."),
                                new TestCase("4\n1 2\n2 3\n3 4\n2 4", "2 4", false)
                        ),
                        "Iterate through the edges. If find(u) == find(v), this edge forms a cycle and is redundant."
                ),
                new DsaProblem(
                        "DSU-104",
                        topic,
                        topicTitle,
                        "D. Largest Component Size by Union Rank",
                        Difficulty.MEDIUM,
                        1500,
                        256,
                        "You are given n nodes labeled 1 to n, each initially isolated in its own set of size 1. You receive q union queries \"u v\". After processing each union query, print the size of the largest connected component in the entire graph.",
                        "The first line contains two integers n and q (1 <= n, q <= 10^5).\nThe next q lines each contain two integers u and v (1 <= u, v <= n).",
                        "For each of the q queries, print the maximum component size on a new line.",
                        "1 <= n, q <= 10^5\n1 <= u, v <= n",
                        List.of(
                                new TestCase("5 4\n1 2\n3 4\n2 4\n1 5", "2\n2\n4\n5", true, "After 1-2: max size 2. After 3-4: max size 2. After 2-4: {1,2,3,4} max size 4. After 1-5: {1,2,3,4,5} max size 5."),
                                new TestCase("3 2\n1 2\n2 3", "2\n3", false),
                                new TestCase("4 1\n1 2", "2", false)
                        ),
                        "Maintain component sizes array size[root]. When uniting two roots, size[root1] += size[root2], updating maxSize."
                ),
                new DsaProblem(
                        "DSU-105",
                        topic,
                        topicTitle,
                        "E. Minimum Spanning Tree (Kruskal's Algorithm)",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Given a connected, undirected graph with n nodes labeled 1 to n and m weighted edges, compute the total weight of its Minimum Spanning Tree (MST) using Kruskal's algorithm with Disjoint Set Union.",
                        "The first line contains two integers n and m (1 <= n <= 10^5, n - 1 <= m <= 2 * 10^5).\nThe next m lines each contain three integers u, v, and w (1 <= u, v <= n, 1 <= w <= 10^9).",
                        "Print a single integer — the total weight of the Minimum Spanning Tree.",
                        "1 <= n <= 10^5\nn - 1 <= m <= 2 * 10^5\n1 <= w <= 10^9",
                        List.of(
                                new TestCase("4 5\n1 2 1\n2 3 4\n3 4 2\n1 4 5\n1 3 3", "6", true, "Selected edges: (1-2, w=1), (3-4, w=2), (1-3, w=3). Total MST weight = 6."),
                                new TestCase("3 3\n1 2 5\n2 3 3\n1 3 1", "4", false),
                                new TestCase("2 1\n1 2 10", "10", false),
                                new TestCase("5 7\n1 2 2\n1 3 3\n2 3 1\n2 4 1\n3 4 4\n3 5 5\n4 5 7", "9", false)
                        ),
                        "Sort edges by weight ascending. For each edge, if find(u) != find(v), add weight to total and union(u, v)."
                )
        );
    }

    // =========================================================================
    // DATA STRUCTURES - TOPIC 9: TRIE (PREFIX TREES) (5 Problems)
    // =========================================================================
    private static List<DsaProblem> buildTrieProblems() {
        String topic = "trie";
        String topicTitle = "Data Structures: Trie (Prefix Tree)";

        return List.of(
                new DsaProblem(
                        "TRIE-101",
                        topic,
                        topicTitle,
                        "A. Implement Trie (Prefix Tree)",
                        Difficulty.EASY,
                        1000,
                        256,
                        "A Trie (pronounced as \"try\") or prefix tree is a tree data structure used to efficiently store and retrieve keys in a dataset of strings. Implement a Trie with q operations:\n- 1 word: Inserts the string word into the trie.\n- 2 word: Prints \"YES\" if the string word is in the trie (i.e., was inserted before), or \"NO\" otherwise.\n- 3 prefix: Prints \"YES\" if there is a previously inserted string word that has the prefix prefix, or \"NO\" otherwise.",
                        "The first line contains an integer q (1 <= q <= 10^5).\nThe following q lines describe operations: \"1 word\", \"2 word\", or \"3 prefix\" (consisting of lowercase English letters, length 1 to 100).",
                        "For each query of type 2 and 3, print \"YES\" or \"NO\" on a new line.",
                        "1 <= q <= 10^5\n1 <= |word|, |prefix| <= 100",
                        List.of(
                                new TestCase("6\n1 apple\n2 apple\n2 app\n3 app\n1 app\n2 app", "YES\nNO\nYES\nYES", true, "Insert apple -> search apple=YES, search app=NO, startsWith app=YES. Insert app -> search app=YES."),
                                new TestCase("3\n1 code\n2 code\n2 cod", "YES\nNO", false),
                                new TestCase("2\n1 tree\n3 tr", "YES", false)
                        ),
                        "Each node contains an array of child pointers children[26] and a boolean isEndOfWord."
                ),
                new DsaProblem(
                        "TRIE-102",
                        topic,
                        topicTitle,
                        "B. Longest Common Prefix of String Array",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Write a function using a Trie to find the longest common prefix string amongst an array of n lowercase strings. If there is no common prefix, print -1.",
                        "The first line contains an integer n (1 <= n <= 10^4).\nThe second line contains n space-separated strings consisting of lowercase English letters (1 <= |s_i| <= 1000).",
                        "Print the longest common prefix string, or -1 if no common prefix exists.",
                        "1 <= n <= 10^4\n1 <= |s_i| <= 1000",
                        List.of(
                                new TestCase("3\nflower flow flight", "fl", true, "Longest prefix shared by flower, flow, and flight is \"fl\"."),
                                new TestCase("3\ndog racecar car", "-1", true, "There is no common prefix among the input strings."),
                                new TestCase("2\ninterview intermediate", "inter", false),
                                new TestCase("1\nalgorithm", "algorithm", false)
                        ),
                        "Insert all strings into a Trie and follow the unique branch from the root until branching or end-of-word occurs."
                ),
                new DsaProblem(
                        "TRIE-103",
                        topic,
                        topicTitle,
                        "C. Search Suggestions System (Autocomplete)",
                        Difficulty.MEDIUM,
                        1500,
                        256,
                        "You are given an array of n strings products and a string searchWord. We want to design a system that suggests at most three product names from products after each character of searchWord is typed. Suggested products should have common prefix with searchWord. If there are more than three products with a common prefix return the three lexicographically minimums products.",
                        "The first line contains an integer n (1 <= n <= 10^4).\nThe second line contains n space-separated strings representing the products.\nThe third line contains the searchWord (1 <= |searchWord| <= 1000).",
                        "Print |searchWord| lines. Each line contains up to 3 space-separated suggestions in lexicographical order, or \"EMPTY\" if no matching product exists.",
                        "1 <= n <= 10^4\n1 <= |products_i|, |searchWord| <= 1000",
                        List.of(
                                new TestCase("5\nmobile mouse moneypot monitor mousepad\nmouse", "mobile moneypot monitor\nmobile moneypot monitor\nmouse mousepad\nmouse mousepad\nmouse mousepad", true, "Prefix 'm': mobile, moneypot, monitor. Prefix 'mo': mobile, moneypot, monitor. Prefix 'mou': mouse, mousepad. Prefix 'mous': mouse, mousepad. Prefix 'mouse': mouse, mousepad."),
                                new TestCase("2\nhaven hello\nhi", "haven hello\nEMPTY", false),
                                new TestCase("3\nbag bagg banner\nbag", "bag bagg banner\nbag bagg banner\nbag bagg banner", false)
                        ),
                        "Store at each Trie node a sorted list of at most 3 lexicographically smallest words passing through it."
                ),
                new DsaProblem(
                        "TRIE-104",
                        topic,
                        topicTitle,
                        "D. Word Break Using Trie Dictionary",
                        Difficulty.MEDIUM,
                        1500,
                        256,
                        "Given a string s and a dictionary of n strings wordDict, determine if s can be segmented into a space-separated sequence of one or more dictionary words. Print \"YES\" if s can be segmented, or \"NO\" otherwise. Note that the same word in the dictionary may be reused multiple times in the segmentation.",
                        "The first line contains the string s (1 <= |s| <= 300).\nThe second line contains an integer n (1 <= n <= 1000).\nThe third line contains n space-separated strings wordDict_1, ..., wordDict_n.",
                        "Print \"YES\" if segmentation is possible, or \"NO\" otherwise.",
                        "1 <= |s| <= 300\n1 <= n <= 1000\n1 <= |wordDict_i| <= 50",
                        List.of(
                                new TestCase("leetcode\n2\nleet code", "YES", true, "Return YES because \"leetcode\" can be segmented as \"leet code\"."),
                                new TestCase("applepenapple\n2\napple pen", "YES", true, "Can be segmented as \"apple pen apple\"."),
                                new TestCase("catsandog\n5\ncats dog sand and cat", "NO", false),
                                new TestCase("a\n1\na", "YES", false)
                        ),
                        "Insert wordDict into a Trie. Use dynamic programming dp[i] = can segment s[0..i], walking through the Trie for suffix matches."
                ),
                new DsaProblem(
                        "TRIE-105",
                        topic,
                        topicTitle,
                        "E. Maximum XOR of Two Numbers in an Array",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Given an integer array nums of size n, return the maximum result of nums[i] XOR nums[j], where 0 <= i <= j < n using a 31-bit binary Trie in O(n * 32) time.",
                        "The first line contains an integer n (1 <= n <= 10^5).\nThe second line contains n space-separated integers nums_1, nums_2, ..., nums_n (0 <= nums_i <= 2^31 - 1).",
                        "Print a single integer — the maximum XOR value.",
                        "1 <= n <= 10^5\n0 <= nums_i <= 2^31 - 1",
                        List.of(
                                new TestCase("6\n3 10 5 25 2 8", "28", true, "The maximum result is 5 XOR 25 = 28."),
                                new TestCase("3\n0 0 0", "0", false),
                                new TestCase("4\n14 70 53 83", "127", false),
                                new TestCase("2\n1 2", "3", false)
                        ),
                        "Build a 31-bit binary Trie (0 and 1 branches). For each number, greedily choose opposite bit branches to maximize XOR value."
                )
        );
    }

    private static List<DsaProblem> buildSortingProblems() {
        String topic = "sorting algorithms";
        String topicTitle = "Sorting Algorithms & Complexity Analysis";

        return List.of(
                new DsaProblem(
                        "SORT-201",
                        topic,
                        topicTitle,
                        "A. K-th Smallest Selection",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Given an array of n integers and an integer k, find the k-th smallest element in the array (1-indexed).",
                        "The first line contains two integers n and k (1 <= k <= n <= 10^5).\nThe second line contains n space-separated integers a_1, a_2, ..., a_n (-10^9 <= a_i <= 10^9).",
                        "Print a single integer — the k-th smallest element.",
                        "1 <= k <= n <= 10^5\n-10^9 <= a_i <= 10^9",
                        List.of(
                                new TestCase("6 3\n7 10 4 3 20 15", "7", true, "Sorted: 3, 4, 7, 10, 15, 20. The 3rd smallest is 7."),
                                new TestCase("5 1\n5 4 3 2 1", "1", false),
                                new TestCase("5 5\n5 4 3 2 1", "5", false)
                        ),
                        "Standard sort or QuickSelect both solve this efficiently."
                ),
                new DsaProblem(
                        "SORT-202",
                        topic,
                        topicTitle,
                        "B. Parity Segregated Sort",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Given an array of n positive integers, sort the array so that all even numbers appear first sorted in ascending order, followed by all odd numbers sorted in descending order.",
                        "The first line contains an integer n (1 <= n <= 10^5).\nThe second line contains n space-separated integers a_1, a_2, ..., a_n (1 <= a_i <= 10^9).",
                        "Print the sorted array elements separated by a space.",
                        "1 <= n <= 10^5\n1 <= a_i <= 10^9",
                        List.of(
                                new TestCase("6\n1 2 3 5 4 7", "2 4 7 5 3 1", true, "Evens: [2, 4] ascending. Odds: [7, 5, 3, 1] descending."),
                                new TestCase("4\n2 4 6 8", "2 4 6 8", false),
                                new TestCase("3\n9 3 7", "9 7 3", false)
                        ),
                        "Separate evens and odds, sort each independently, and concatenate."
                ),
                new DsaProblem(
                        "SORT-203",
                        topic,
                        topicTitle,
                        "C. Inversion Count (Merge Sort)",
                        Difficulty.MEDIUM,
                        1500,
                        256,
                        "An inversion in an array A is a pair of indices (i, j) such that 1 <= i < j <= n and A[i] > A[j]. Calculate the total number of inversions in the array.",
                        "The first line contains an integer n (1 <= n <= 2 * 10^5).\nThe second line contains n space-separated integers a_1, a_2, ..., a_n (1 <= a_i <= 10^9).",
                        "Print a single integer — the total count of inversions. Note that the answer may exceed standard 32-bit integer limits (use 64-bit integer / long).",
                        "1 <= n <= 2 * 10^5\n1 <= a_i <= 10^9",
                        List.of(
                                new TestCase("5\n2 4 1 3 5", "3", true, "The inversions are: (2, 1), (4, 1), (4, 3). Total = 3."),
                                new TestCase("5\n5 4 3 2 1", "10", false),
                                new TestCase("4\n1 2 3 4", "0", false)
                        ),
                        "Modify Merge Sort to count cross-inversions during the merge step in O(n log n)."
                ),
                new DsaProblem(
                        "SORT-204",
                        topic,
                        topicTitle,
                        "D. Dutch National Flag 3-Way Partition",
                        Difficulty.MEDIUM,
                        1000,
                        256,
                        "Given an array containing n integers with values only from {0, 1, 2}, sort the array in ascending order in-place using only O(1) extra space and a single pass.",
                        "The first line contains an integer n (1 <= n <= 2 * 10^5).\nThe second line contains n space-separated integers containing only 0, 1, or 2.",
                        "Print the sorted array separated by spaces.",
                        "1 <= n <= 2 * 10^5\na_i in {0, 1, 2}",
                        List.of(
                                new TestCase("6\n2 0 2 1 1 0", "0 0 1 1 2 2", true, "Elements 0, 1, 2 grouped together in non-decreasing order."),
                                new TestCase("3\n2 0 1", "0 1 2", false),
                                new TestCase("4\n0 0 0 0", "0 0 0 0", false)
                        ),
                        "Use 3 pointers (low, mid, high) swapping elements according to Dijkstra's 3-way partition algorithm."
                ),
                new DsaProblem(
                        "SORT-205",
                        topic,
                        topicTitle,
                        "E. Merge K Sorted Streams",
                        Difficulty.HARD,
                        2000,
                        256,
                        "You are given k sorted arrays. The total number of elements across all arrays is N. Merge all k sorted arrays into a single sorted array.",
                        "The first line contains an integer k (1 <= k <= 10^4) — the number of sorted arrays.\nFor each array, the next line contains an integer m_i (1 <= m_i <= 10^5) — the size of this array, followed by m_i space-separated integers in non-decreasing order.\nTotal elements N = sum(m_i) <= 2 * 10^5.",
                        "Print the combined N integers in non-decreasing order, separated by a space.",
                        "1 <= k <= 10^4\nTotal elements N <= 2 * 10^5",
                        List.of(
                                new TestCase("3\n3\n1 4 7\n3\n2 5 8\n2\n3 6", "1 2 3 4 5 6 7 8", true, "Merging the 3 sorted sequences yields 1 2 3 4 5 6 7 8."),
                                new TestCase("2\n2\n10 20\n2\n5 15", "5 10 15 20", false),
                                new TestCase("1\n4\n1 2 3 4", "1 2 3 4", false)
                        ),
                        "Use a Min-Heap of size k holding the current smallest element from each array to achieve O(N log k) complexity."
                )
        );
    }

    // =========================================================================
    // TOPIC 3: SEARCHING (2 Easy, 2 Medium, 1 Tough)
    // =========================================================================
    private static List<DsaProblem> buildSearchingProblems() {
        String topic = "searching";
        String topicTitle = "Linear, Binary Search & Search on Answer";

        return List.of(
                new DsaProblem(
                        "SEARCH-301",
                        topic,
                        topicTitle,
                        "A. First and Last Position",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Given an array of n integers sorted in non-decreasing order and a target value x, find the starting and ending indices (1-indexed) of x. If x is not present, output -1 -1.",
                        "The first line contains two integers n and x (1 <= n <= 2 * 10^5, -10^9 <= x <= 10^9).\nThe second line contains n space-separated integers a_1, a_2, ..., a_n (-10^9 <= a_i <= 10^9) in sorted order.",
                        "Print two space-separated integers: the 1-indexed first and last position of x, or -1 -1 if not found.",
                        "1 <= n <= 2 * 10^5\n-10^9 <= a_i, x <= 10^9",
                        List.of(
                                new TestCase("6 8\n5 7 7 8 8 10", "4 5", true, "The value 8 first appears at position 4 and ends at position 5."),
                                new TestCase("6 6\n5 7 7 8 8 10", "-1 -1", true, "The value 6 is not in the array."),
                                new TestCase("1 5\n5", "1 1", false),
                                new TestCase("5 2\n2 2 2 2 2", "1 5", false)
                        ),
                        "Use binary search twice (lower_bound and upper_bound) in O(log n)."
                ),
                new DsaProblem(
                        "SEARCH-302",
                        topic,
                        topicTitle,
                        "B. Search in Rotated Sorted Array",
                        Difficulty.EASY,
                        1000,
                        256,
                        "An array of n distinct integers sorted in ascending order was rotated at an unknown pivot. Given target value x, find its 1-indexed position in the rotated array, or -1 if not found.",
                        "The first line contains two integers n and x (1 <= n <= 2 * 10^5, -10^9 <= x <= 10^9).\nThe second line contains n space-separated distinct integers.",
                        "Print the 1-indexed position of x, or -1 if not present.",
                        "1 <= n <= 2 * 10^5\nAll elements are distinct",
                        List.of(
                                new TestCase("7 0\n4 5 6 7 0 1 2", "5", true, "0 is at index 5."),
                                new TestCase("7 3\n4 5 6 7 0 1 2", "-1", true, "3 is not in the array."),
                                new TestCase("5 1\n1 2 3 4 5", "1", false)
                        ),
                        "At least one half of the rotated array is always strictly sorted. Use this property to discard half the space each step."
                ),
                new DsaProblem(
                        "SEARCH-303",
                        topic,
                        topicTitle,
                        "C. Koko Banana Consumption (Search on Answer)",
                        Difficulty.MEDIUM,
                        1500,
                        256,
                        "There are n piles of bananas, the i-th pile has p_i bananas. The guards will be gone for h hours. You want to choose an integer speed k (bananas-per-hour) to finish all piles within h hours. In each hour, you choose a pile and eat up to k bananas from it. Find the minimum integer k.",
                        "The first line contains two integers n and h (1 <= n <= h <= 10^9, n <= 10^5).\nThe second line contains n space-separated integers p_1, p_2, ..., p_n (1 <= p_i <= 10^9).",
                        "Print a single integer — the minimum speed k.",
                        "1 <= n <= 10^5\nn <= h <= 10^9\n1 <= p_i <= 10^9",
                        List.of(
                                new TestCase("4 8\n3 6 7 11", "4", true, "With speed 4: ceil(3/4)+ceil(6/4)+ceil(7/4)+ceil(11/4) = 1+2+2+3 = 8 <= 8."),
                                new TestCase("5 5\n30 11 23 4 20", "30", false),
                                new TestCase("5 6\n30 11 23 4 20", "23", false)
                        ),
                        "The feasibility predicate canFinish(k) is monotonic. Binary search for k in [1, max(p_i)]."
                ),
                new DsaProblem(
                        "SEARCH-304",
                        topic,
                        topicTitle,
                        "D. Aggressive Cows (Maximized Minimum)",
                        Difficulty.MEDIUM,
                        1500,
                        256,
                        "Farmer John has n stalls located at positions x_1, x_2, ..., x_n along a straight line. He wants to assign c cows to stalls such that the minimum distance between any two cows is as large as possible. Find this largest minimum distance.",
                        "The first line contains two integers n and c (2 <= c <= n <= 10^5).\nThe second line contains n space-separated integers x_1, x_2, ..., x_n (0 <= x_i <= 10^9).",
                        "Print a single integer — the largest possible minimum distance between cows.",
                        "2 <= c <= n <= 10^5\n0 <= x_i <= 10^9",
                        List.of(
                                new TestCase("5 3\n1 2 8 4 9", "3", true, "Stalls sorted: 1, 2, 4, 8, 9. Placing cows at 1, 4, 8 or 1, 4, 9 gives min distance 3."),
                                new TestCase("3 2\n1 5 10", "9", false),
                                new TestCase("4 4\n0 10 20 30", "10", false)
                        ),
                        "Sort stall positions, then binary search on answer distance D with greedy placement check."
                ),
                new DsaProblem(
                        "SEARCH-305",
                        topic,
                        topicTitle,
                        "E. Median of Two Sorted Arrays",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Given two sorted arrays A of size n and B of size m, find the median of the combined sorted array in O(log(min(n, m))) time complexity. If total size n + m is odd, return floor(median). If even, return floor of average.",
                        "The first line contains two integers n and m (0 <= n, m <= 10^5, n + m >= 1).\nThe second line contains n integers representing array A.\nThe third line contains m integers representing array B.",
                        "Print a single integer — the floor of the overall median.",
                        "0 <= n, m <= 10^5\n1 <= n + m <= 2 * 10^5\n-10^9 <= A[i], B[j] <= 10^9",
                        List.of(
                                new TestCase("2 2\n1 3\n2 4", "2", true, "Combined: [1, 2, 3, 4]. Middle values are 2 and 3. Average is 2.5, floor is 2."),
                                new TestCase("2 1\n1 2\n3", "2", true, "Combined: [1, 2, 3]. Median is 2."),
                                new TestCase("0 3\n\n10 20 30", "20", false),
                                new TestCase("4 0\n1 2 3 4\n", "2", false)
                        ),
                        "Partition both arrays simultaneously using binary search on the smaller array."
                )
        );
    }

    // =========================================================================
    // TOPIC 4: GRAPHS (2 Easy, 2 Medium, 1 Tough)
    // =========================================================================
    private static List<DsaProblem> buildGraphsProblems() {
        String topic = "graphs";
        String topicTitle = "Graph Theory, Shortest Path & Connectivity";

        return List.of(
                new DsaProblem(
                        "GRAPH-401",
                        topic,
                        topicTitle,
                        "A. Breadth-First Grid Shortest Path",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Given an R x C grid where '.' represents an empty cell and '#' represents an obstacle, find the minimum number of steps to travel from the top-left corner (0, 0) to bottom-right corner (R-1, C-1). You can move up, down, left, or right. If unreachable, print -1.",
                        "The first line contains two integers R and C (1 <= R, C <= 1000).\nThe next R lines each contain a string of length C containing only '.' and '#'.",
                        "Print the minimum steps required, or -1 if unreachable.",
                        "1 <= R, C <= 1000",
                        List.of(
                                new TestCase("3 3\n...\n.#.\n...", "4", true, "Optimal path: (0,0)->(0,1)->(0,2)->(1,2)->(2,2) takes 4 steps."),
                                new TestCase("2 2\n..\n##", "-1", false),
                                new TestCase("1 1\n.", "0", false)
                        ),
                        "Standard BFS queue guarantees shortest distance on unweighted graphs."
                ),
                new DsaProblem(
                        "GRAPH-402",
                        topic,
                        topicTitle,
                        "B. Bipartite Graph 2-Coloring",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Given an undirected graph with n vertices and m edges, determine if it is bipartite (i.e. vertices can be colored using 2 colors such that no two adjacent vertices share the same color).",
                        "The first line contains two integers n and m (1 <= n <= 10^5, 0 <= m <= 2 * 10^5).\nThe next m lines each contain two integers u and v (1 <= u, v <= n, u != v) describing an edge.",
                        "Print \"YES\" if the graph is bipartite, or \"NO\" otherwise.",
                        "1 <= n <= 10^5\n0 <= m <= 2 * 10^5",
                        List.of(
                                new TestCase("4 4\n1 2\n2 3\n3 4\n4 1", "YES", true, "Even cycle of length 4 can be colored 1-2-1-2."),
                                new TestCase("3 3\n1 2\n2 3\n3 1", "NO", true, "Odd cycle of length 3 cannot be 2-colored."),
                                new TestCase("3 0", "YES", false)
                        ),
                        "A graph is bipartite if and only if it contains no odd cycles. Use BFS/DFS 2-coloring."
                ),
                new DsaProblem(
                        "GRAPH-403",
                        topic,
                        topicTitle,
                        "C. Dijkstra Single-Source Shortest Path",
                        Difficulty.MEDIUM,
                        1500,
                        256,
                        "Given a directed weighted graph with n vertices and m edges with non-negative edge weights, compute the shortest distance from vertex 1 to all vertices 1..n. If a vertex is unreachable, print -1 for its distance.",
                        "The first line contains two integers n and m (1 <= n <= 10^5, 0 <= m <= 2 * 10^5).\nThe next m lines each contain three integers u, v, w (1 <= u, v <= n, 0 <= w <= 10^9) representing a directed edge from u to v of weight w.",
                        "Print n space-separated integers, where the i-th integer is the shortest distance from vertex 1 to vertex i.",
                        "1 <= n <= 10^5\n0 <= m <= 2 * 10^5\n0 <= w <= 10^9",
                        List.of(
                                new TestCase("4 4\n1 2 2\n2 3 3\n1 3 6\n3 4 1", "0 2 5 6", true, "Distances from 1: 1->0, 2->2, 3->5 (via 2), 4->6."),
                                new TestCase("3 1\n1 2 5", "0 5 -1", false)
                        ),
                        "Use Dijkstra's algorithm with a Min-Priority Queue in O((V + E) log V)."
                ),
                new DsaProblem(
                        "GRAPH-404",
                        topic,
                        topicTitle,
                        "D. Course Schedule (Topological Sort)",
                        Difficulty.MEDIUM,
                        1500,
                        256,
                        "There are n courses labeled 1 to n. Some courses have prerequisites. Given m directed edges (u, v) meaning course u must be taken before course v, find any valid order to take all courses. If it is impossible to finish all courses due to a cycle, print -1.",
                        "The first line contains two integers n and m (1 <= n <= 10^5, 0 <= m <= 2 * 10^5).\nThe next m lines each contain two integers u and v (1 <= u, v <= n, u != v).",
                        "If a valid ordering exists, print n space-separated integers representing the order. Otherwise, print -1.",
                        "1 <= n <= 10^5\n0 <= m <= 2 * 10^5",
                        List.of(
                                new TestCase("4 3\n1 2\n2 3\n3 4", "1 2 3 4", true, "A linear chain of prerequisites."),
                                new TestCase("2 2\n1 2\n2 1", "-1", true, "Cyclic dependency: impossible to complete.")
                        ),
                        "Use Kahn's algorithm with in-degrees or DFS cycle detection."
                ),
                new DsaProblem(
                        "GRAPH-405",
                        topic,
                        topicTitle,
                        "E. Critical Bridges in Network (Tarjan)",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Given a connected undirected graph with n vertices and m edges, find the total number of bridges (critical connection edges whose removal disconnects the graph).",
                        "The first line contains two integers n and m (2 <= n <= 10^5, n-1 <= m <= 2 * 10^5).\nThe next m lines each contain two integers u and v (1 <= u, v <= n, u != v) describing an undirected edge.",
                        "Print a single integer — the count of bridge edges in the graph.",
                        "2 <= n <= 10^5\n1 <= m <= 2 * 10^5",
                        List.of(
                                new TestCase("4 4\n1 2\n2 3\n3 1\n3 4", "1", true, "Edge (3, 4) is the only bridge. Cycle 1-2-3 has no bridges."),
                                new TestCase("3 3\n1 2\n2 3\n3 1", "0", false),
                                new TestCase("4 3\n1 2\n2 3\n3 4", "3", false)
                        ),
                        "Use Tarjan's DFS bridge-finding algorithm tracking discovery times `tin[u]` and lowest reachable ancestor `low[u]`."
                )
        );
    }

    // =========================================================================
    // TOPIC 5: RANGE QUERIES (2 Easy, 2 Medium, 1 Tough)
    // =========================================================================
    private static List<DsaProblem> buildRangeQueriesProblems() {
        String topic = "range queries";
        String topicTitle = "Range Queries: Segment Trees & Fenwick Trees";

        return List.of(
                new DsaProblem(
                        "RANGE-501",
                        topic,
                        topicTitle,
                        "A. 2D Prefix Sum Subgrid",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Given an N x M matrix of integers and Q queries, each asking for the sum of integers in the rectangle bounded by top-left (r1, c1) and bottom-right (r2, c2) (1-indexed). Answer each query in O(1) time after 2D prefix sum preprocessing.",
                        "The first line contains three integers N, M, Q (1 <= N, M <= 1000, 1 <= Q <= 10^5).\nThe next N lines each contain M space-separated integers (-1000 <= val <= 1000).\nThe next Q lines each contain four integers r1, c1, r2, c2 (1 <= r1 <= r2 <= N, 1 <= c1 <= c2 <= M).",
                        "For each query, print the subgrid sum on a new line.",
                        "1 <= N, M <= 1000\n1 <= Q <= 10^5",
                        List.of(
                                new TestCase("3 3 2\n1 2 3\n4 5 6\n7 8 9\n1 1 2 2\n2 2 3 3", "12\n28", true, "Subgrid (1,1)-(2,2) sum = 1+2+4+5 = 12. Subgrid (2,2)-(3,3) sum = 5+6+8+9 = 28."),
                                new TestCase("1 1 1\n5\n1 1 1 1", "5", false)
                        ),
                        "Formula: pref[r2][c2] - pref[r1-1][c2] - pref[r2][c1-1] + pref[r1-1][c1-1]."
                ),
                new DsaProblem(
                        "RANGE-502",
                        topic,
                        topicTitle,
                        "B. Difference Array Range Additions",
                        Difficulty.EASY,
                        1000,
                        256,
                        "You have an array of n zeros. You are given q updates of the form (l, r, v), meaning add v to all elements from index l to r (1-indexed). After applying all q updates, output the final array values.",
                        "The first line contains two integers n and q (1 <= n, q <= 2 * 10^5).\nThe next q lines each contain three integers l, r, v (1 <= l <= r <= n, -10^9 <= v <= 10^9).",
                        "Print n space-separated integers representing the final array.",
                        "1 <= n, q <= 2 * 10^5",
                        List.of(
                                new TestCase("5 3\n1 3 2\n2 4 3\n3 5 -1", "2 5 4 2 -1", true, "After (1,3)+2: [2,2,2,0,0]. After (2,4)+3: [2,5,5,3,0]. After (3,5)-1: [2,5,4,2,-1]."),
                                new TestCase("3 1\n1 3 10", "10 10 10", false)
                        ),
                        "Use difference array diff[l] += v and diff[r+1] -= v, then take prefix sum."
                ),
                new DsaProblem(
                        "RANGE-503",
                        topic,
                        topicTitle,
                        "C. Dynamic Point Update & Range Sum (Fenwick Tree)",
                        Difficulty.MEDIUM,
                        1500,
                        256,
                        "Given an array of n integers and q queries:\n- \"1 idx val\": add val to element at position idx (1-indexed).\n- \"2 l r\": calculate sum of elements from index l to r inclusive.",
                        "The first line contains two integers n and q (1 <= n, q <= 2 * 10^5).\nThe second line contains n integers a_1, ..., a_n (0 <= a_i <= 10^9).\nThe next q lines contain queries of type 1 or 2.",
                        "For each query of type 2, print the range sum on a new line.",
                        "1 <= n, q <= 2 * 10^5",
                        List.of(
                                new TestCase("5 3\n1 2 3 4 5\n2 1 3\n1 2 10\n2 1 3", "6\n16", true, "Initial sum 1..3 = 1+2+3 = 6. After adding 10 to index 2, sum 1..3 = 1+12+3 = 16."),
                                new TestCase("3 2\n0 0 0\n1 1 5\n2 1 3", "5", false)
                        ),
                        "Use a Binary Indexed Tree (Fenwick) for O(log n) updates and prefix queries."
                ),
                new DsaProblem(
                        "RANGE-504",
                        topic,
                        topicTitle,
                        "D. Segment Tree Range Minimum Query",
                        Difficulty.MEDIUM,
                        1500,
                        256,
                        "Given an array of n integers, answer q queries:\n- \"1 idx val\": update element at index idx to val.\n- \"2 l r\": find the minimum value in range [l, r].",
                        "The first line contains two integers n and q (1 <= n, q <= 2 * 10^5).\nThe second line contains n integers (-10^9 <= a_i <= 10^9).\nThe next q lines each contain a query of type 1 or 2.",
                        "For each type 2 query, print the minimum on a new line.",
                        "1 <= n, q <= 2 * 10^5",
                        List.of(
                                new TestCase("5 3\n5 2 8 6 3\n2 1 3\n1 2 9\n2 1 3", "2\n5", true, "Min in range [1, 3] is 2. After updating index 2 to 9, min in range [1, 3] becomes 5."),
                                new TestCase("3 2\n10 20 30\n2 2 3\n2 1 3", "20\n10", false)
                        ),
                        "Build a Segment Tree storing min(left_child, right_child)."
                ),
                new DsaProblem(
                        "RANGE-505",
                        topic,
                        topicTitle,
                        "E. Range Addition & Range Sum (Lazy Segment Tree)",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Maintain an array of n elements (initially all 0) under q operations:\n- \"1 l r v\": Add v to every element in range [l, r].\n- \"2 l r\": Print the sum of elements in range [l, r].",
                        "The first line contains two integers n and q (1 <= n, q <= 2 * 10^5).\nThe next q lines each contain a query \"1 l r v\" or \"2 l r\" (1 <= l <= r <= n, -10^6 <= v <= 10^6).",
                        "For each query of type 2, print the range sum on a new line.",
                        "1 <= n, q <= 2 * 10^5",
                        List.of(
                                new TestCase("5 4\n1 1 3 2\n2 1 5\n1 3 5 3\n2 2 4", "6\n13", true, "Range add [1,3]+2 gives [2,2,2,0,0], sum 1..5 is 6. Range add [3,5]+3 gives [2,2,5,3,3], sum 2..4 is 2+5+3 = 10? Wait: 2+5+3 = 10? Wait: [2,2,2+3,3,3] = [2,2,5,3,3]. Sum of index 2..4 is 2+5+3 = 10. Wait! Let's check sample: 1 1 3 2 -> index 1,2,3 get 2. Range 2..4 is index 2,3,4. Index 2 has 2, index 3 has 2+3=5, index 4 has 3. Sum = 2+5+3 = 10.")
                        ),
                        "Lazy propagation defers updates to child nodes until accessed."
                )
        );
    }

    // =========================================================================
    // TOPIC 6: ALGORITHMIC PARADIGMS (2 Easy, 2 Medium, 1 Tough)
    // =========================================================================
    private static List<DsaProblem> buildParadigmsProblems() {
        String topic = "algorithmic paradigms";
        String topicTitle = "Dynamic Programming, Greedy & Backtracking";

        return List.of(
                new DsaProblem(
                        "DP-601",
                        topic,
                        topicTitle,
                        "A. Maximum Subarray Sum (Kadane)",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Given an array of n integers, find the contiguous subarray (containing at least one number) which has the largest sum and output its sum.",
                        "The first line contains an integer n (1 <= n <= 2 * 10^5).\nThe second line contains n space-separated integers (-10^9 <= a_i <= 10^9).",
                        "Print a single integer — the maximum subarray sum.",
                        "1 <= n <= 2 * 10^5\n-10^9 <= a_i <= 10^9",
                        List.of(
                                new TestCase("9\n-2 1 -3 4 -1 2 1 -5 4", "6", true, "Subarray [4, -1, 2, 1] has the largest sum = 6."),
                                new TestCase("5\n-1 -2 -3 -4 -5", "-1", false),
                                new TestCase("1\n100", "100", false)
                        ),
                        "Kadane's algorithm keeps running max: cur = max(a[i], cur + a[i])."
                ),
                new DsaProblem(
                        "DP-602",
                        topic,
                        topicTitle,
                        "B. Coin Change Minimum Coins",
                        Difficulty.EASY,
                        1000,
                        256,
                        "You are given n distinct coin denominations and an integer target amount S. Find the minimum number of coins needed to make up that amount. Each coin can be used unlimited times. If the amount cannot be made up, print -1.",
                        "The first line contains two integers n and S (1 <= n <= 100, 1 <= S <= 10^5).\nThe second line contains n distinct integers c_1, ..., c_n (1 <= c_i <= 10^5).",
                        "Print the minimum number of coins, or -1 if impossible.",
                        "1 <= n <= 100\n1 <= S <= 10^5",
                        List.of(
                                new TestCase("3 11\n1 2 5", "3", true, "11 = 5 + 5 + 1 (3 coins)."),
                                new TestCase("2 3\n2 4", "-1", true, "Cannot form 3 using only coins 2 and 4."),
                                new TestCase("1 0\n1", "0", false)
                        ),
                        "dp[x] = min(dp[x], dp[x - c] + 1)."
                ),
                new DsaProblem(
                        "DP-603",
                        topic,
                        topicTitle,
                        "C. Classic 0/1 Knapsack",
                        Difficulty.MEDIUM,
                        1500,
                        256,
                        "You have n items, each with a weight w_i and a value v_i. You have a knapsack of capacity W. Choose a subset of items such that total weight does not exceed W and total value is maximized.",
                        "The first line contains two integers n and W (1 <= n <= 1000, 1 <= W <= 10^4).\nThe next n lines each contain two integers w_i and v_i (1 <= w_i <= W, 1 <= v_i <= 10^9).",
                        "Print a single integer — the maximum value that fits in the knapsack.",
                        "1 <= n <= 1000\n1 <= W <= 10^4",
                        List.of(
                                new TestCase("3 50\n10 60\n20 100\n30 120", "220", true, "Taking item 2 (wt 20, val 100) and item 3 (wt 30, val 120) gives wt 50 and max val 220."),
                                new TestCase("1 10\n15 100", "0", false)
                        ),
                        "Standard 1D reverse-iteration DP: dp[j] = max(dp[j], dp[j - w] + v)."
                ),
                new DsaProblem(
                        "DP-604",
                        topic,
                        topicTitle,
                        "D. Longest Increasing Subsequence in O(N log N)",
                        Difficulty.MEDIUM,
                        1500,
                        256,
                        "Given an integer array of size n, find the length of the longest strictly increasing subsequence.",
                        "The first line contains an integer n (1 <= n <= 2 * 10^5).\nThe second line contains n space-separated integers (-10^9 <= a_i <= 10^9).",
                        "Print a single integer — the length of the longest strictly increasing subsequence.",
                        "1 <= n <= 2 * 10^5",
                        List.of(
                                new TestCase("8\n10 9 2 5 3 7 101 18", "4", true, "The LIS is [2, 3, 7, 101] or [2, 5, 7, 101], length 4."),
                                new TestCase("6\n0 1 0 3 2 3", "4", false),
                                new TestCase("4\n7 7 7 7", "1", false)
                        ),
                        "Maintain an array of minimum tails and use binary search (std::lower_bound) in O(n log n)."
                ),
                new DsaProblem(
                        "DP-605",
                        topic,
                        topicTitle,
                        "E. Traveling Salesperson Bitmask DP",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Given an N x N cost matrix where cost[i][j] is the travel cost from city i to city j, find the minimum cost to visit all N cities (0 to N-1) exactly once and return to starting city 0.",
                        "The first line contains an integer N (2 <= N <= 16).\nThe next N lines each contain N integers representing the cost matrix (0 <= cost[i][j] <= 10^6, cost[i][i] = 0).",
                        "Print a single integer — the minimum tour cost.",
                        "2 <= N <= 16\n0 <= cost[i][j] <= 10^6",
                        List.of(
                                new TestCase("4\n0 10 15 20\n10 0 35 25\n15 35 0 30\n20 25 30 0", "80", true, "Tour 0->1->3->2->0 gives cost 10 + 25 + 30 + 15 = 80."),
                                new TestCase("2\n0 5\n5 0", "10", false)
                        ),
                        "dp[mask][u] where mask represents the visited bitset of cities and u is the current city. O(N^2 * 2^N)."
                )
        );
    }

    // =========================================================================
    // TOPIC 7: STRING ALGORITHMS (2 Easy, 2 Medium, 1 Tough)
    // =========================================================================
    private static List<DsaProblem> buildStringProblems() {
        String topic = "string algorithms";
        String topicTitle = "String Algorithms, KMP & Suffix Automata";

        return List.of(
                new DsaProblem(
                        "STR-701",
                        topic,
                        topicTitle,
                        "A. Valid Anagram Verification",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Given two lowercase English strings s and t, determine if t is an anagram of s (i.e. has the exact same character frequencies).",
                        "The first line contains string s.\nThe second line contains string t (1 <= |s|, |t| <= 2 * 10^5).",
                        "Print \"YES\" if t is an anagram of s, or \"NO\" otherwise.",
                        "1 <= |s|, |t| <= 2 * 10^5",
                        List.of(
                                new TestCase("anagram\nnagaram", "YES", true, "Same character frequencies."),
                                new TestCase("rat\ncar", "NO", true, "Different letters."),
                                new TestCase("a\na", "YES", false)
                        ),
                        "Count frequencies in an array of size 26."
                ),
                new DsaProblem(
                        "STR-702",
                        topic,
                        topicTitle,
                        "B. Longest Common Prefix",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Given an array of n strings, find the longest string S that is a prefix of every string in the array. If there is no common prefix, print \"-1\".",
                        "The first line contains an integer n (1 <= n <= 10^4).\nThe next n lines contain the strings (total characters <= 2 * 10^5).",
                        "Print the longest common prefix, or \"-1\" if none exists.",
                        "1 <= n <= 10^4\nTotal length <= 2 * 10^5",
                        List.of(
                                new TestCase("3\nflower\nflow\nflight", "fl", true, "\"fl\" is common to all 3 words."),
                                new TestCase("3\ndog\nracecar\ncar", "-1", true, "No common prefix."),
                                new TestCase("1\nalgorithm", "algorithm", false)
                        ),
                        "Iterate column by column or sort and compare first and last."
                ),
                new DsaProblem(
                        "STR-703",
                        topic,
                        topicTitle,
                        "C. KMP Pattern Occurrence Matching",
                        Difficulty.MEDIUM,
                        1500,
                        256,
                        "Given text T and pattern P, find all 1-indexed starting positions where P occurs in T. Output the total number of matches, followed by the 1-indexed starting positions in ascending order on a new line. If no match, print 0.",
                        "The first line contains text T (1 <= |T| <= 5 * 10^5).\nThe second line contains pattern P (1 <= |P| <= |T|).",
                        "The first line of output: integer count of matches.\nThe second line of output: space-separated 1-indexed positions (if count > 0).",
                        "1 <= |P| <= |T| <= 5 * 10^5",
                        List.of(
                                new TestCase("AABAACAADAABAABA\nAABA", "3\n1 9 12", true, "Occurs at index 1, 9, 12."),
                                new TestCase("ABCDE\nXYZ", "0", false),
                                new TestCase("AAAAA\nAA", "4\n1 2 3 4", false)
                        ),
                        "Precompute the KMP prefix function pi in O(|P|), then match T in O(|T|)."
                ),
                new DsaProblem(
                        "STR-704",
                        topic,
                        topicTitle,
                        "D. Trie Prefix Word Counter",
                        Difficulty.MEDIUM,
                        1500,
                        256,
                        "You are building an autocomplete query engine. You are given n words inserted into a dictionary. Then you must answer q queries: for a given prefix string, count how many dictionary words have this prefix.",
                        "The first line contains two integers n and q (1 <= n, q <= 10^5).\nThe next n lines each contain a lowercase dictionary word.\nThe next q lines each contain a prefix query string.\nTotal length of all words and queries <= 5 * 10^5.",
                        "For each query, print the number of words matching the prefix on a new line.",
                        "1 <= n, q <= 10^5\nTotal characters <= 5 * 10^5",
                        List.of(
                                new TestCase("4 3\napple\napp\napricot\nbanana\napp\nap\nban", "2\n3\n1", true, "\"app\": apple, app (2). \"ap\": apple, app, apricot (3). \"ban\": banana (1)."),
                                new TestCase("2 1\ncode\nforces\ncpp", "0", false)
                        ),
                        "Insert each word into a Trie incrementing a pass_count integer at every node."
                ),
                new DsaProblem(
                        "STR-705",
                        topic,
                        topicTitle,
                        "E. Longest Duplicate Substring (Rolling Hash)",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Given a string S, find the length of the longest duplicate substring that appears at least twice in S. The two occurrences may overlap. If no substring appears twice, print 0.",
                        "The first line contains string S (2 <= |S| <= 10^5) consisting of lowercase English letters.",
                        "Print a single integer — the maximum length of a duplicate substring.",
                        "2 <= |S| <= 10^5",
                        List.of(
                                new TestCase("banana", "3", true, "\"ana\" appears twice at index 1 and 3 (0-indexed). Max length = 3."),
                                new TestCase("abcd", "0", true, "All substrings are unique."),
                                new TestCase("aaaaa", "4", false)
                        ),
                        "Binary search on the length L of the substring, checking for duplicates using Rabin-Karp polynomial rolling hashing."
                )
        );
    }

    // =========================================================================
    // TOPIC 8: MATHEMATICS (2 Easy, 2 Medium, 1 Tough)
    // =========================================================================
    private static List<DsaProblem> buildMathProblems() {
        String topic = "mathematics";
        String topicTitle = "Discrete Mathematics & Number Theory for CP";

        return List.of(
                new DsaProblem(
                        "MATH-801",
                        topic,
                        topicTitle,
                        "A. Extended Euclidean Algorithm & GCD",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Given two positive integers a and b, find integers x and y such that a*x + b*y = gcd(a, b). Print gcd(a, b), x, and y separated by a space. If multiple pairs (x, y) exist, any valid pair is accepted.",
                        "The first line contains two integers a and b (1 <= a, b <= 10^9).",
                        "Print three space-separated integers: gcd(a, b), x, and y.",
                        "1 <= a, b <= 10^9",
                        List.of(
                                new TestCase("30 20", "10 1 -1", true, "gcd(30, 20) = 10. 30*(1) + 20*(-1) = 30 - 20 = 10."),
                                new TestCase("35 15", "5 1 -2", false),
                                new TestCase("7 5", "1 -2 3", false)
                        ),
                        "Use the Extended Euclidean Algorithm recursively or iteratively."
                ),
                new DsaProblem(
                        "MATH-802",
                        topic,
                        topicTitle,
                        "B. Sieve of Eratosthenes Prime Count",
                        Difficulty.EASY,
                        1000,
                        256,
                        "Given an integer N, count the total number of prime numbers strictly less than or equal to N.",
                        "The first line contains a single integer N (1 <= N <= 5 * 10^6).",
                        "Print a single integer — the count of primes <= N.",
                        "1 <= N <= 5 * 10^6",
                        List.of(
                                new TestCase("10", "4", true, "Primes <= 10 are 2, 3, 5, 7 (count = 4)."),
                                new TestCase("1", "0", false),
                                new TestCase("100", "25", false),
                                new TestCase("5000000", "348513", false)
                        ),
                        "Use an optimized boolean/bitset Sieve of Eratosthenes."
                ),
                new DsaProblem(
                        "MATH-803",
                        topic,
                        topicTitle,
                        "C. Fast Modular Exponentiation",
                        Difficulty.MEDIUM,
                        1000,
                        256,
                        "Given three integers A, B, and M, compute (A^B) mod M efficiently.",
                        "The first line contains three space-separated integers A, B, M (0 <= A <= 10^18, 0 <= B <= 10^18, 1 <= M <= 10^9 + 7).",
                        "Print a single integer — the value of (A^B) mod M.",
                        "0 <= A, B <= 10^18\n1 <= M <= 10^9 + 7",
                        List.of(
                                new TestCase("2 10 1000", "24", true, "2^10 = 1024. 1024 mod 1000 = 24."),
                                new TestCase("3 5 7", "5", false),
                                new TestCase("5 0 100", "1", false)
                        ),
                        "Binary exponentiation solves this in O(log B) steps."
                ),
                new DsaProblem(
                        "MATH-804",
                        topic,
                        topicTitle,
                        "D. Combinatorics nCr Modulo 10^9+7",
                        Difficulty.MEDIUM,
                        1500,
                        256,
                        "You need to answer Q queries. Each query gives two integers N and R. Compute nCr = N! / (R! * (N - R)!) modulo 10^9 + 7.",
                        "The first line contains an integer Q (1 <= Q <= 10^5).\nThe next Q lines each contain two integers N and R (0 <= R <= N <= 2 * 10^5).",
                        "For each query, print nCr mod (10^9 + 7) on a new line.",
                        "1 <= Q <= 10^5\n0 <= R <= N <= 2 * 10^5",
                        List.of(
                                new TestCase("3\n5 2\n6 3\n10 5", "10\n20\n252", true, "5C2 = 10, 6C3 = 20, 10C5 = 252."),
                                new TestCase("1\n0 0", "1", false)
                        ),
                        "Precompute factorials and modular inverse factorials using Fermat's Little Theorem in O(N), answering each query in O(1)."
                ),
                new DsaProblem(
                        "MATH-805",
                        topic,
                        topicTitle,
                        "E. Matrix Exponentiation (Fibonacci 10^18)",
                        Difficulty.HARD,
                        2000,
                        256,
                        "The Fibonacci sequence is defined as F(0) = 0, F(1) = 1, and F(n) = F(n-1) + F(n-2) for n >= 2. Given n, calculate F(n) modulo 10^9 + 7.",
                        "The first line contains a single integer n (0 <= n <= 10^18).",
                        "Print F(n) mod (10^9 + 7).",
                        "0 <= n <= 10^18",
                        List.of(
                                new TestCase("10", "55", true, "F(10) = 55."),
                                new TestCase("0", "0", false),
                                new TestCase("1", "1", false),
                                new TestCase("100", "687995182", false)
                        ),
                        "Use 2x2 companion matrix exponentiation: [[1, 1], [1, 0]]^(n-1) in O(log n)."
                )
        );
    }

    // =========================================================================
    // WEB DEVELOPMENT (8 Topics x 5 Exercises)
    // =========================================================================

    // =========================================================================
    // WEB DEVELOPMENT - HTML5 & Semantic Web Structure (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildHtml5Problems() {
        String topic = "html5";
        String topicTitle = "HTML5 & Semantic Web Structure";

        return List.of(
                new DsaProblem(
                        "WH-101",
                        topic,
                        topicTitle,
                        "A. Personal Portfolio with Semantic HTML5",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Build a personal portfolio page using semantic HTML5 elements.",
                        "Single HTML file with header, nav, main, sections (About, Projects, Contact), article for project items, and footer. Use proper figure and figcaption for project previews.",
                        "Clean semantic HTML5 structure with no non-semantic container overuse. Working nav links and validated form inputs.",
                        "Project Difficulty: Easy | Tech: HTML5 Semantic Elements",
                        List.of(
                                new TestCase("Open index.html in browser", "All semantic sections render in order", true, "Semantic markup verified"),
                                new TestCase("Submit contact form empty", "Browser constraint validation prompts user", false),
                                new TestCase("Lighthouse Accessibility Audit", "Accessibility score >= 90", false)
                        ),
                        "Use landmark elements (<main>, <nav>, <header>, <footer>) for screen reader navigation."
                ),
                new DsaProblem(
                        "WH-102",
                        topic,
                        topicTitle,
                        "B. Accessible Product Card Component",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Create an accessible e-commerce product card component using HTML5.",
                        "Product card with image (alt text), heading hierarchy (h2/h3), pricing in <data> tag, star ratings with aria-label, and Add to Cart button.",
                        "Accessible standalone HTML component passing WCAG AA requirements.",
                        "Project Difficulty: Easy | Tech: HTML5, ARIA, Accessibility",
                        List.of(
                                new TestCase("WAVE accessibility scanner", "0 contrast or missing alt errors", true, "Passed automated check"),
                                new TestCase("Keyboard TAB focus", "Focus ring visible on button and links", false),
                                new TestCase("Screen reader inspection", "Card title, price, and actions announced properly", false)
                        ),
                        "Always provide meaningful alternative text for informative product visuals."
                ),
                new DsaProblem(
                        "WH-103",
                        topic,
                        topicTitle,
                        "C. Multi-Step Registration Form",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build a 3-step registration form using native HTML5 form controls and validation.",
                        "Multi-step form: Step 1 (Account: email, password with pattern), Step 2 (Profile: name, avatar file upload with accept filter), Step 3 (Preferences: checkboxes, select). Native <progress> element.",
                        "Multi-fieldset form with validation on required fields and pattern matching.",
                        "Project Difficulty: Medium | Tech: HTML5 Forms, Constraint Validation",
                        List.of(
                                new TestCase("Input invalid email format", "Browser blocks progression with invalid email message", true, "Native validation triggers"),
                                new TestCase("Upload non-image file", "File picker filters by image/* MIME types", false),
                                new TestCase("Progress element state", "<progress value='2' max='3'> reflects step 2", false)
                        ),
                        "Leverage input types like email, tel, url and pattern attributes for built-in validation."
                ),
                new DsaProblem(
                        "WH-104",
                        topic,
                        topicTitle,
                        "D. Responsive Media Embed & Tables",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Construct a rich media page with responsive video, audio, picture elements, and accessible data tables.",
                        "<video> with poster and subtitles track, <audio> player with fallback, <picture> with media query breakpoints, and <table> with <thead>, <tbody>, <th scope='col'>.",
                        "Media players function natively across modern viewports with accessible tabular data.",
                        "Project Difficulty: Medium | Tech: HTML5 Media, Responsive Images, Accessible Tables",
                        List.of(
                                new TestCase("Resize viewport below 768px", "<picture> loads mobile-optimized asset from srcset", true, "Breakpoint asset loaded"),
                                new TestCase("Screen reader table navigation", "Headers announced for each data cell via scope attribute", false),
                                new TestCase("Play video track", "Subtitles render in WebVTT format", false)
                        ),
                        "Use <source> elements with explicit MIME types inside <video> and <audio>."
                ),
                new DsaProblem(
                        "WH-105",
                        topic,
                        topicTitle,
                        "E. Complete Blog Article with Structured SEO",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Author a production-ready blog article page with full meta tags, OpenGraph data, and Schema.org JSON-LD.",
                        "Complete <head> metadata (og:title, og:image, twitter:card, canonical link), semantic <article> with <time datetime>, and embedded <script type='application/ld+json'> Schema.org Article schema.",
                        "Valid HTML5 document with structured data passing Google Rich Results test.",
                        "Project Difficulty: Hard | Tech: HTML5, SEO, Schema.org JSON-LD, OpenGraph",
                        List.of(
                                new TestCase("Google Rich Results Test", "Valid Article structured data recognized", true, "Schema detected"),
                                new TestCase("Social share preview generator", "OpenGraph title, image, and description rendered", false),
                                new TestCase("W3C HTML5 Validator", "0 validation errors or deprecation warnings", false)
                        ),
                        "Include canonical URLs and accurate ISO 8601 timestamps in <time datetime> tags."
                )
        );
    }

    // =========================================================================
    // WEB DEVELOPMENT - Modern CSS3, Flexbox & CSS Grid (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildCss3Problems() {
        String topic = "css3";
        String topicTitle = "Modern CSS3, Flexbox & CSS Grid";

        return List.of(
                new DsaProblem(
                        "WC-101",
                        topic,
                        topicTitle,
                        "A. Responsive Flexbox Navigation Bar",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Create a modern responsive navigation bar using CSS Flexbox.",
                        "Navbar with logo on left, centered links, and right action button. Below 768px, links stack or collapse into a mobile drawer.",
                        "Clean Flexbox navigation with smooth hover transitions and mobile responsiveness.",
                        "Project Difficulty: Easy | Tech: CSS3 Flexbox, Media Queries, Transitions",
                        List.of(
                                new TestCase("Desktop viewport >= 1024px", "Logo, center nav links, and CTA aligned horizontally", true, "Flex alignment verified"),
                                new TestCase("Mobile viewport <= 600px", "Flex direction switches to column or drawer view", false),
                                new TestCase("Hover over nav links", "Smooth 0.2s color and underline transition", false)
                        ),
                        "Use justify-content: space-between and align-items: center for standard navbars."
                ),
                new DsaProblem(
                        "WC-102",
                        topic,
                        topicTitle,
                        "B. CSS Grid Photo Gallery with Spanning Cards",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Build a responsive photo gallery using CSS Grid repeat and auto-fit.",
                        "Grid container using repeat(auto-fit, minmax(240px, 1fr)) with gap. Featured images span 2 columns and 2 rows.",
                        "Responsive masonry-style photo grid that reflows seamlessly across screen sizes.",
                        "Project Difficulty: Easy | Tech: CSS Grid, auto-fit, minmax, grid-column",
                        List.of(
                                new TestCase("Resize viewport from 1200px to 400px", "Columns adjust from 4 to 1 column automatically", true, "Grid reflows smoothly"),
                                new TestCase("Featured card inspection", "Featured element spans grid-column: span 2", false),
                                new TestCase("Image hover effect", "Subtle transform: scale(1.03) and box-shadow elevation", false)
                        ),
                        "Use object-fit: cover on images inside grid cells to prevent distortion."
                ),
                new DsaProblem(
                        "WC-103",
                        topic,
                        topicTitle,
                        "C. Responsive Admin Dashboard Layout",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Design an admin dashboard layout using CSS Grid Areas and CSS Custom Properties.",
                        "Layout with sidebar, top header, main content, and metrics cards. Use grid-template-areas: 'sidebar header' 'sidebar main'. Theming via CSS variables (--bg-primary, --text-primary).",
                        "Fluid admin dashboard that converts sidebar to bottom bar on mobile viewports.",
                        "Project Difficulty: Medium | Tech: CSS Grid Areas, Custom Properties, Fluid Typography",
                        List.of(
                                new TestCase("Switch theme attribute data-theme='dark'", "Background and text CSS variables update immediately", true, "Dark theme applied"),
                                new TestCase("Mobile breakpoint < 768px", "Grid areas collapse to single column with sticky header", false),
                                new TestCase("Typography scaling", "clamp(1rem, 2.5vw, 1.5rem) scales smoothly across resolutions", false)
                        ),
                        "Define global theme variables on :root for easy switching."
                ),
                new DsaProblem(
                        "WC-104",
                        topic,
                        topicTitle,
                        "D. CSS Animation Loading Spinner & Skeleton Screen",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Develop pure CSS loading spinners and skeleton placeholder shimmer animations.",
                        "Dual-ring CSS spinner using @keyframes rotate. Skeleton loading card with linear-gradient shimmer animation moving left-to-right using background-position.",
                        "Silky 60fps loading UI states without JavaScript animation dependencies.",
                        "Project Difficulty: Medium | Tech: CSS @keyframes, Gradients, Shimmer Animation",
                        List.of(
                                new TestCase("Inspect spinner element", "Infinite linear rotation animation active", true, "Smooth rotation verified"),
                                new TestCase("Inspect skeleton card", "Shimmer gradient oscillates continuously across width", false),
                                new TestCase("Performance audit", "Zero layout thrashing; uses transform and opacity only", false)
                        ),
                        "Animate transform and opacity for optimal GPU-accelerated performance."
                ),
                new DsaProblem(
                        "WC-105",
                        topic,
                        topicTitle,
                        "E. Full E-Commerce Product Page with Dark Mode",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Build a responsive e-commerce product showcase with CSS Scroll Snap and dark mode support.",
                        "Interactive product page: horizontal thumbnail scroll using scroll-snap-type: x mandatory, size selector pills, accordion specs, and @media (prefers-color-scheme: dark) override.",
                        "Production-ready product detail UI that respects user system preferences.",
                        "Project Difficulty: Hard | Tech: CSS Scroll Snap, prefers-color-scheme, Flex/Grid Hybrid",
                        List.of(
                                new TestCase("Swipe product image gallery", "Images snap precisely to viewport center", true, "Scroll snap verified"),
                                new TestCase("System dark mode active", "Colors automatically switch to dark palette", false),
                                new TestCase("Button active state", "Micro-interaction scale(0.97) on click/active", false)
                        ),
                        "Use scroll-padding and scroll-snap-align: center for consistent carousel alignment."
                )
        );
    }

    // =========================================================================
    // WEB DEVELOPMENT - JavaScript ES6+ & DOM Manipulation (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildJavascriptProblems() {
        String topic = "javascript";
        String topicTitle = "JavaScript ES6+ & DOM Manipulation";

        return List.of(
                new DsaProblem(
                        "WJ-101",
                        topic,
                        topicTitle,
                        "A. Dynamic Interactive Todo List",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Build an interactive task list using vanilla JavaScript DOM methods.",
                        "Task manager supporting add task, mark as completed (toggle class), delete task, and item counter. Persist state in localStorage.",
                        "Working task app with full DOM event listeners and local persistence.",
                        "Project Difficulty: Easy | Tech: Vanilla JS, DOM Events, localStorage",
                        List.of(
                                new TestCase("Add task 'Study DOM'", "New <li> element appended to list with text", true, "Item added to DOM"),
                                new TestCase("Click task item", "Toggles .completed class with strikethrough", false),
                                new TestCase("Page reload", "Tasks restored from localStorage via JSON.parse", false)
                        ),
                        "Use Event Delegation on the parent <ul> for efficient event handling."
                ),
                new DsaProblem(
                        "WJ-102",
                        topic,
                        topicTitle,
                        "B. Real-Time Text Character & Word Counter",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Implement a live character and word counter for a textarea.",
                        "Real-time analyzer measuring character count, word count, estimated reading time, and remaining characters against a 280-char limit. Warning class applied when < 20 characters remain.",
                        "Responsive text counter updating synchronously on 'input' events.",
                        "Project Difficulty: Easy | Tech: JavaScript ES6, String Methods, Regex",
                        List.of(
                                new TestCase("Type 5 words into textarea", "Word counter shows 5; character counter updates", true, "Counts accurate"),
                                new TestCase("Type 270 characters", "Badge switches to warning yellow/red style", false),
                                new TestCase("Exceed 280 characters", "Input trimmed or submit button disabled", false)
                        ),
                        "Split by regex /\\s+/ to accurately count words while handling multiple spaces."
                ),
                new DsaProblem(
                        "WJ-103",
                        topic,
                        topicTitle,
                        "C. Fetch API Weather Widget with Async/Await",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Create an asynchronous weather search widget consuming a REST API.",
                        "Search input for city name. Fetch weather data using async/await and Fetch API. Display temperature, condition, icon, and humidity. Handle loading spinner and network errors.",
                        "Robust API-driven widget with comprehensive try/catch error handling.",
                        "Project Difficulty: Medium | Tech: JS Async/Await, Fetch API, JSON Parsing",
                        List.of(
                                new TestCase("Search 'London'", "Displays parsed temperature and weather condition", true, "API response rendered"),
                                new TestCase("Search non-existent city", "Displays user-friendly error: 'City not found'", false),
                                new TestCase("Network disconnect", "Catches network exception and displays retry prompt", false)
                        ),
                        "Always check response.ok before parsing response.json() in Fetch requests."
                ),
                new DsaProblem(
                        "WJ-104",
                        topic,
                        topicTitle,
                        "D. Interactive Quiz Engine with Timer",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Develop a timed multiple-choice quiz engine in pure JavaScript.",
                        "Quiz engine rendering questions sequentially from an array of objects. 30-second countdown timer per question using setInterval. Score calculation and final review breakdown.",
                        "Modular quiz app with state management, countdown timer, and score summary.",
                        "Project Difficulty: Medium | Tech: JS Timers, Array Methods, Closures",
                        List.of(
                                new TestCase("Start Quiz", "First question renders with 4 options and timer starts", true, "Quiz initialized"),
                                new TestCase("Timer reaches 0", "Auto-advances to next question marked as unanswered", false),
                                new TestCase("Complete all questions", "Final score percentage and correct answers review shown", false)
                        ),
                        "Clear intervals using clearInterval() whenever moving between questions or completing."
                ),
                new DsaProblem(
                        "WJ-105",
                        topic,
                        topicTitle,
                        "E. Drag-and-Drop Kanban Task Board",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Construct a multi-column Kanban board using HTML5 Drag and Drop API.",
                        "Kanban board with columns (Backlog, In Progress, Done). Drag cards between columns with visual drop target highlighting. Add/edit/delete cards with full state sync in localStorage.",
                        "Fully functional drag-and-drop workflow with smooth reordering and data persistence.",
                        "Project Difficulty: Hard | Tech: HTML5 Drag & Drop API, ES6 Modules, State Sync",
                        List.of(
                                new TestCase("Drag task from Backlog to Done", "Card relocates to Done column and state updates", true, "Drop handled correctly"),
                                new TestCase("Drag over column", "Column background highlights with .drag-over class", false),
                                new TestCase("Refresh page after drag", "New column arrangement persists from localStorage", false)
                        ),
                        "Prevent default on dragover event to allow drop on container elements."
                )
        );
    }

    // =========================================================================
    // WEB DEVELOPMENT - React.js Modern Frontend Framework (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildReactProblems() {
        String topic = "react";
        String topicTitle = "React.js Modern Frontend Framework";

        return List.of(
                new DsaProblem(
                        "WR-101",
                        topic,
                        topicTitle,
                        "A. State Counter with History & Reducer",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Build a feature-rich counter component using React useReducer.",
                        "Counter supporting increment, decrement, reset, step size configuration, and undo history of past counts via useReducer action dispatch.",
                        "Well-structured React component managing complex state transitions cleanly.",
                        "Project Difficulty: Easy | Tech: React, useReducer, Action Types",
                        List.of(
                                new TestCase("Dispatch INCREMENT action", "Count increments by configured step size", true, "State updated via reducer"),
                                new TestCase("Dispatch UNDO action", "Count reverts to previous value in history stack", false),
                                new TestCase("Configure step to 5 and increment", "Count increases by 5", false)
                        ),
                        "Keep reducer functions pure and return new state objects immutably."
                ),
                new DsaProblem(
                        "WR-102",
                        topic,
                        topicTitle,
                        "B. Filterable Product Grid with Search & Sort",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Create a searchable, filterable product catalog using React useState and useMemo.",
                        "Product catalog with text search, category pills, price range slider, and sort dropdown. Use useMemo to optimize filtered list calculations.",
                        "Fast, responsive catalog filtering hundreds of items without UI lag.",
                        "Project Difficulty: Easy | Tech: React, useState, useMemo, Controlled Inputs",
                        List.of(
                                new TestCase("Type 'laptop' in search", "Grid updates in real-time to show matching items", true, "Filtered list reactive"),
                                new TestCase("Select category 'Electronics'", "Shows items matching both category and search query", false),
                                new TestCase("Change sort to 'Price: Low to High'", "List sorts ascending without extra re-renders", false)
                        ),
                        "Wrap expensive filter/sort operations in useMemo with proper dependency arrays."
                ),
                new DsaProblem(
                        "WR-103",
                        topic,
                        topicTitle,
                        "C. GitHub User Profile Explorer with useEffect",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build a GitHub user search app with data fetching, caching, and error boundaries.",
                        "Search GitHub users, fetch profile and repository statistics using useEffect. Show loading skeletons during fetch, render repo cards, and handle 404/rate-limit states.",
                        "Polished data-fetching component with cleanup and abort controllers.",
                        "Project Difficulty: Medium | Tech: React, useEffect, AbortController, Fetch",
                        List.of(
                                new TestCase("Search 'octocat'", "Renders profile stats, avatar, and top 5 repositories", true, "Data fetched and displayed"),
                                new TestCase("Rapidly change search query", "Previous in-flight request aborted cleanly via AbortController", false),
                                new TestCase("Enter invalid username", "Renders error message: 'User not found'", false)
                        ),
                        "Return cleanup function in useEffect to abort pending requests on unmount/re-render."
                ),
                new DsaProblem(
                        "WR-104",
                        topic,
                        topicTitle,
                        "D. Multi-Step Checkout Wizard with React Context",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Develop a multi-step checkout wizard sharing state via React Context.",
                        "3-step wizard: Step 1 (Shipping Address), Step 2 (Payment Details), Step 3 (Review & Confirm). Context provides form state, validation flags, and navigation methods.",
                        "Robust multi-step form with step navigation, validation gates, and review summary.",
                        "Project Difficulty: Medium | Tech: React Context API, useContext, Custom Hooks",
                        List.of(
                                new TestCase("Step 1 with empty required fields", "Next button disabled or shows field errors", true, "Validation gate active"),
                                new TestCase("Fill Step 1 and proceed to Step 2", "Shipping state preserved in Context", false),
                                new TestCase("Navigate back to Step 1", "Entered values remain populated in inputs", false)
                        ),
                        "Create a custom useCheckout() hook to consume context with safety checks."
                ),
                new DsaProblem(
                        "WR-105",
                        topic,
                        topicTitle,
                        "E. Custom Hook Suite: useLocalStorage & useDebounce",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Build reusable custom React hooks (useLocalStorage, useDebounce, useFetch).",
                        "Create a library of custom hooks: 1) useLocalStorage(key, initialValue) with cross-tab sync, 2) useDebounce(value, delay) for input throttling, 3) useFetch(url) with cache and retry.",
                        "Modular, thoroughly tested React custom hooks ready for production usage.",
                        "Project Difficulty: Hard | Tech: Custom React Hooks, Generics, Event Listeners",
                        List.of(
                                new TestCase("Type fast into debounced input", "Debounced value updates only after 300ms idle", true, "Debounce verified"),
                                new TestCase("Update state via useLocalStorage", "Value updates in localStorage and across browser tabs", false),
                                new TestCase("Call useFetch with failing endpoint", "Hook exposes error state and retry() callback", false)
                        ),
                        "Handle SSR edge cases (typeof window !== 'undefined') in localStorage hooks."
                )
        );
    }

    // =========================================================================
    // WEB DEVELOPMENT - Backend Engineering: Node.js & Express (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildNodeProblems() {
        String topic = "node";
        String topicTitle = "Backend Engineering: Node.js & Express";

        return List.of(
                new DsaProblem(
                        "WN-101",
                        topic,
                        topicTitle,
                        "A. RESTful CRUD API with Express & Validation",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Build a complete RESTful CRUD API for managing books with Express.",
                        "Express application providing GET /api/books, GET /api/books/:id, POST /api/books, PUT /api/books/:id, and DELETE /api/books/:id with in-memory storage and status codes (200, 201, 400, 404).",
                        "Fully functioning REST endpoints tested against standard HTTP requests.",
                        "Project Difficulty: Easy | Tech: Node.js, Express, REST Architecture",
                        List.of(
                                new TestCase("POST /api/books with title & author", "Returns 201 Created with generated book ID", true, "Book created successfully"),
                                new TestCase("GET /api/books/999", "Returns 404 Not Found with JSON error message", false),
                                new TestCase("DELETE /api/books/:id", "Returns 204 No Content; subsequent GET returns 404", false)
                        ),
                        "Use express.json() middleware to parse JSON request payloads."
                ),
                new DsaProblem(
                        "WN-102",
                        topic,
                        topicTitle,
                        "B. Custom Request Logger & Error Middleware",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Implement modular logging and centralized error handling middleware in Express.",
                        "1) Logger middleware logging method, path, timestamp, and duration (ms). 2) Central error handler catching thrown errors and returning formatted JSON errors with proper status codes.",
                        "Clean Express pipeline with non-blocking logging and centralized error responses.",
                        "Project Difficulty: Easy | Tech: Express Middleware, Error Handling",
                        List.of(
                                new TestCase("Send request to any route", "Console logs '[GET] /api/items - 200 (12ms)'", true, "Logger middleware active"),
                                new TestCase("Route throws new Error('Database down')", "Error handler catches and responds with 500 JSON", false),
                                new TestCase("Request invalid route", "404 not found handler triggers formatted JSON", false)
                        ),
                        "Error middleware functions in Express must take 4 parameters: (err, req, res, next)."
                ),
                new DsaProblem(
                        "WN-103",
                        topic,
                        topicTitle,
                        "C. Secure File Upload Service with Multer",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build a file upload endpoint supporting image storage, MIME validation, and size limits.",
                        "POST /api/upload using multer. Restrict to JPEG/PNG, max size 5MB. Generate unique file names using UUID/timestamp and return downloadable public file URL.",
                        "Secure file upload route protecting against invalid MIME types and oversize payloads.",
                        "Project Difficulty: Medium | Tech: Node.js, Express, Multer, File System",
                        List.of(
                                new TestCase("Upload 2MB PNG image", "Returns 200 with public image access URL", true, "File uploaded and stored"),
                                new TestCase("Upload 10MB file", "Returns 400 with 'File size exceeds limit (5MB)'", false),
                                new TestCase("Upload .exe disguised file", "Multer fileFilter rejects non-image MIME type", false)
                        ),
                        "Store file metadata in a database and keep files on disk or object storage."
                ),
                new DsaProblem(
                        "WN-104",
                        topic,
                        topicTitle,
                        "D. Sliding Window Rate Limiting Middleware",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Develop an in-memory sliding window rate limiter middleware for Express.",
                        "Rate limiter allowing maximum 100 requests per IP per 15-minute window. Set standard HTTP rate limit headers (X-RateLimit-Limit, X-RateLimit-Remaining, Retry-After). Return 429 when exceeded.",
                        "Production-ready rate limiter protecting backend services from traffic spikes.",
                        "Project Difficulty: Medium | Tech: Express Middleware, Rate Limiting, HTTP Headers",
                        List.of(
                                new TestCase("Send 5 requests from same IP", "Headers show Remaining: 95; status 200", true, "Rate limit tracking works"),
                                new TestCase("Send 101st request within window", "Returns 429 Too Many Requests with Retry-After header", false),
                                new TestCase("Wait until window expires", "Request count resets and requests succeed again", false)
                        ),
                        "Use Redis in distributed systems; in-memory Map works great for single-instance apps."
                ),
                new DsaProblem(
                        "WN-105",
                        topic,
                        topicTitle,
                        "E. Production E-Commerce API with JWT & Filtering",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Architect an e-commerce API with JWT authentication, pagination, sorting, and search.",
                        "Endpoints for products and orders. Implement query parameter parsing for pagination (?page=1&limit=20), filtering (?category=tech&price_lte=500), sorting (?sort=-price), and text search.",
                        "Comprehensive e-commerce backend with protected routes and scalable query filtering.",
                        "Project Difficulty: Hard | Tech: Express, JWT Auth, Advanced Query Parsing",
                        List.of(
                                new TestCase("GET /api/products?page=2&limit=10", "Returns page 2 items with totalPages and totalItems metadata", true, "Pagination verified"),
                                new TestCase("GET /api/products?sort=-price", "Returns items ordered by price descending", false),
                                new TestCase("POST /api/orders without Bearer token", "Returns 401 Unauthorized", false)
                        ),
                        "Sanitize and validate all query parameters before passing them to database queries."
                )
        );
    }

    // =========================================================================
    // WEB DEVELOPMENT - Databases: PostgreSQL & MongoDB (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildDatabaseProblems() {
        String topic = "database";
        String topicTitle = "Databases: PostgreSQL & MongoDB";

        return List.of(
                new DsaProblem(
                        "WDB-101",
                        topic,
                        topicTitle,
                        "A. PostgreSQL E-Commerce Relational Schema",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Design and implement a relational database schema in PostgreSQL with foreign keys and constraints.",
                        "CREATE TABLE definitions for users, products, categories, orders, and order_items. Include PRIMARY KEY, FOREIGN KEY with ON DELETE CASCADE, NOT NULL, and UNIQUE constraints.",
                        "Normalized relational schema in 3NF with verified data integrity.",
                        "Project Difficulty: Easy | Tech: PostgreSQL, SQL DDL, Constraints, Foreign Keys",
                        List.of(
                                new TestCase("Insert order_item with invalid product_id", "PostgreSQL rejects insert with foreign key violation", true, "Integrity enforced"),
                                new TestCase("Delete parent user", "Cascades appropriately or restricts per schema design", false),
                                new TestCase("Query schema metadata", "Tables, foreign keys, and indexes reflect design", false)
                        ),
                        "Always add indexes on foreign key columns for optimal JOIN performance."
                ),
                new DsaProblem(
                        "WDB-102",
                        topic,
                        topicTitle,
                        "B. MongoDB Product Catalog with Mongoose ODM",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Create a flexible product catalog schema and model using MongoDB and Mongoose.",
                        "Mongoose schema with nested objects (dimensions, tags array), custom validators (price > 0), timestamps, and virtual property for discountedPrice. CRUD helper methods.",
                        "Robust Mongoose schema with schema validation and virtual properties.",
                        "Project Difficulty: Easy | Tech: MongoDB, Mongoose ODM, Schema Validation",
                        List.of(
                                new TestCase("Save product with negative price", "Mongoose throws ValidationError on price", true, "Validation active"),
                                new TestCase("Query product instance", "Virtual 'discountedPrice' calculates correctly", false),
                                new TestCase("Update product with new tags", "$addToSet appends tags without duplicates", false)
                        ),
                        "Use lean() on read queries when Mongoose document methods are not needed for faster performance."
                ),
                new DsaProblem(
                        "WDB-103",
                        topic,
                        topicTitle,
                        "C. Advanced SQL Queries: Aggregations & Joins",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Write advanced SQL queries for sales analytics using JOINs, GROUP BY, and HAVING.",
                        "Queries to calculate: 1) Monthly revenue breakdown by category, 2) Top 5 customers by lifetime spend, 3) Products never ordered (LEFT JOIN / NULL check), 4) Categories with > $10,000 revenue.",
                        "Accurate SQL analytical queries with optimized join order.",
                        "Project Difficulty: Medium | Tech: PostgreSQL, Complex JOINs, Aggregations, HAVING",
                        List.of(
                                new TestCase("Execute monthly revenue query", "Returns grouped monthly sales with category names", true, "Aggregation verified"),
                                new TestCase("Execute unpurchased products query", "Identifies items in products table missing from order_items", false),
                                new TestCase("Execute top 5 customers query", "Returns top 5 ranked by SUM(order_total) DESC", false)
                        ),
                        "Use HAVING instead of WHERE when filtering on aggregate values like SUM or COUNT."
                ),
                new DsaProblem(
                        "WDB-104",
                        topic,
                        topicTitle,
                        "D. MongoDB Complex Aggregation Pipeline",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Construct a multi-stage MongoDB aggregation pipeline for business analytics.",
                        "Aggregation pipeline using $match (date range), $unwind (items), $lookup (join product details), $group (calculate total sales and average order value), and $sort.",
                        "Performant aggregation pipeline computing multi-dimensional metrics.",
                        "Project Difficulty: Medium | Tech: MongoDB, Aggregation Framework, $lookup",
                        List.of(
                                new TestCase("Run aggregation for Q3 2026", "Pipeline outputs quarterly revenue and item breakdown", true, "Metrics calculated"),
                                new TestCase("Inspect $lookup stage", "Product details correctly merged into order items", false),
                                new TestCase("Inspect performance with explain()", "Pipeline uses index on date field during $match stage", false)
                        ),
                        "Place $match and $project stages as early as possible in the pipeline to reduce document volume."
                ),
                new DsaProblem(
                        "WDB-105",
                        topic,
                        topicTitle,
                        "E. Database Migration & Transaction Management",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Implement database transaction handling and rollback mechanisms for order checkout.",
                        "Checkout transaction in PostgreSQL / Node.js: 1) Deduct product inventory, 2) Create order record, 3) Create order items, 4) Deduct user balance. If any step fails, ROLLBACK entire transaction.",
                        "ACID-compliant checkout flow with complete isolation and error rollbacks.",
                        "Project Difficulty: Hard | Tech: PostgreSQL, Transactions, ACID, Row Locking",
                        List.of(
                                new TestCase("Checkout with sufficient stock & funds", "All records committed; stock deducted", true, "Transaction committed"),
                                new TestCase("Checkout when inventory deduction fails", "Transaction rolls back; user balance unchanged", false),
                                new TestCase("Simultaneous checkout on last item", "SELECT FOR UPDATE prevents race condition / overselling", false)
                        ),
                        "Use row-level locking (SELECT ... FOR UPDATE) inside transactions to prevent race conditions."
                )
        );
    }

    // =========================================================================
    // WEB DEVELOPMENT - Authentication, JWT & Web Security (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildAuthProblems() {
        String topic = "auth";
        String topicTitle = "Authentication, JWT & Web Security";

        return List.of(
                new DsaProblem(
                        "WAU-101",
                        topic,
                        topicTitle,
                        "A. Secure Password Hashing with Bcrypt",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Implement password hashing, salting, and verification using bcrypt.",
                        "Functions for hashPassword(plainText, saltRounds) and verifyPassword(plainText, hash). Demonstrate timing-attack safety and verify hashes change with unique salts.",
                        "Secure cryptographic password storage module.",
                        "Project Difficulty: Easy | Tech: Node.js, Bcrypt, Cryptography",
                        List.of(
                                new TestCase("Hash password 'Secret123!'", "Generates 60-character bcrypt hash string starting with $2b$", true, "Hash generated"),
                                new TestCase("Verify with correct password", "verifyPassword returns true", false),
                                new TestCase("Verify with incorrect password", "verifyPassword returns false", false)
                        ),
                        "Use a cost factor (salt rounds) of 10 to 12 for optimal security/performance balance."
                ),
                new DsaProblem(
                        "WAU-102",
                        topic,
                        topicTitle,
                        "B. JWT Token Generation & Verification Middleware",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Build an Express authentication middleware using JSON Web Tokens (JWT).",
                        "1) generateToken(userPayload) with 1-hour expiration, 2) authenticateToken middleware extracting token from 'Authorization: Bearer <token>', verifying secret, and attaching req.user.",
                        "Secure JWT authentication middleware rejecting malformed, expired, or unsigned tokens.",
                        "Project Difficulty: Easy | Tech: Node.js, JWT, Express Middleware",
                        List.of(
                                new TestCase("Request with valid JWT", "req.user populated and next() called with 200 response", true, "Access granted"),
                                new TestCase("Request with expired JWT", "Returns 401 with message 'Token expired'", false),
                                new TestCase("Request without Auth header", "Returns 401 with message 'Authorization token required'", false)
                        ),
                        "Never store sensitive information like passwords in JWT payloads as they are base64 encoded."
                ),
                new DsaProblem(
                        "WAU-103",
                        topic,
                        topicTitle,
                        "C. Complete Auth Flow: Register, Login, Refresh",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build a production authentication flow with short-lived access tokens and refresh tokens.",
                        "Auth system: POST /auth/register, POST /auth/login (returns access token 15m + refresh token 7d in HttpOnly cookie), POST /auth/refresh (rotates refresh token), POST /auth/logout.",
                        "Secure token rotation flow with HttpOnly cookies preventing XSS token theft.",
                        "Project Difficulty: Medium | Tech: JWT, Refresh Tokens, HttpOnly Cookies, Auth Flows",
                        List.of(
                                new TestCase("Login with valid credentials", "Receives access token and HttpOnly refresh cookie", true, "Login succeeded"),
                                new TestCase("POST /auth/refresh with valid cookie", "Receives new access token and rotated refresh token", false),
                                new TestCase("Re-use old refresh token", "System detects token replay and revokes token family", false)
                        ),
                        "Store refresh tokens in database with revocation flags to enable remote logout."
                ),
                new DsaProblem(
                        "WAU-104",
                        topic,
                        topicTitle,
                        "D. Role-Based Access Control (RBAC) System",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Implement a flexible Role-Based Access Control middleware for Express.",
                        "RBAC middleware authorize(['ADMIN', 'MANAGER']) checking user role permissions against requested route. Support hierarchical roles and permission inheritance.",
                        "Granular permission enforcement guarding administrative and restricted endpoints.",
                        "Project Difficulty: Medium | Tech: RBAC, Express Middleware, Authorization",
                        List.of(
                                new TestCase("User with 'ADMIN' role accesses /api/admin", "Access granted; 200 OK", true, "Admin authorized"),
                                new TestCase("User with 'USER' role accesses /api/admin", "Access denied; returns 403 Forbidden", false),
                                new TestCase("User with 'MANAGER' accesses scoped resource", "Allowed for manager permissions", false)
                        ),
                        "Differentiate between 401 Unauthorized (unauthenticated) and 403 Forbidden (insufficient role)."
                ),
                new DsaProblem(
                        "WAU-105",
                        topic,
                        topicTitle,
                        "E. OAuth 2.0 Social Authentication with Google",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Implement OAuth 2.0 social login integration with Google using Passport.js.",
                        "Google OAuth 2.0 flow: /auth/google redirect, /auth/google/callback handler, user profile extraction, auto-provisioning account in database, and session/JWT issuance.",
                        "Seamless third-party social login integration with user provisioning.",
                        "Project Difficulty: Hard | Tech: OAuth 2.0, Passport.js, Google API, Node.js",
                        List.of(
                                new TestCase("Navigate to /auth/google", "Redirects to accounts.google.com consent screen", true, "OAuth redirect verified"),
                                new TestCase("Callback with valid code", "Exchanges code for profile, creates user, issues JWT", false),
                                new TestCase("Callback with user denied", "Redirects to login page with error query param", false)
                        ),
                        "Store OAuth provider ID and email separately to allow linking multiple login methods."
                )
        );
    }

    // =========================================================================
    // WEB DEVELOPMENT - Full-Stack DevOps & Cloud Deployment (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildDeployProblems() {
        String topic = "deploy";
        String topicTitle = "Full-Stack DevOps & Cloud Deployment";

        return List.of(
                new DsaProblem(
                        "WDP-101",
                        topic,
                        topicTitle,
                        "A. Production Environment Config & Secrets Setup",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Configure application environment configuration and secret management.",
                        "Setup dotenv with environment validation using Joi/Zod: ensure PORT, DATABASE_URL, and JWT_SECRET are defined on startup or terminate with clear diagnostics. Create .env.example template.",
                        "Fail-fast configuration loader guaranteeing all required environment variables exist.",
                        "Project Difficulty: Easy | Tech: Node.js, dotenv, Config Validation",
                        List.of(
                                new TestCase("Start app with valid .env", "App initializes on configured PORT", true, "Config loaded"),
                                new TestCase("Start app with missing JWT_SECRET", "App throws descriptive error on startup and exits (code 1)", false),
                                new TestCase("Inspect repository", ".env is ignored in .gitignore; .env.example contains dummy templates", false)
                        ),
                        "Never commit actual secrets or credentials to version control."
                ),
                new DsaProblem(
                        "WDP-102",
                        topic,
                        topicTitle,
                        "B. Multi-Stage Dockerfile for Node.js Application",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Write an optimized, secure multi-stage Dockerfile for a Node.js full-stack application.",
                        "Multi-stage Dockerfile: Stage 1 (builder) installs devDependencies, builds frontend/TypeScript. Stage 2 (production) uses lightweight alpine image, non-root user (node), copies only prod artifacts.",
                        "Minimal Docker image (< 150MB) running securely as non-root user.",
                        "Project Difficulty: Easy | Tech: Docker, Multi-Stage Builds, Alpine Linux",
                        List.of(
                                new TestCase("docker build -t app:prod .", "Build succeeds through all stages", true, "Image built"),
                                new TestCase("docker run -p 3000:3000 app:prod", "Container starts and responds to HTTP requests", false),
                                new TestCase("Inspect container user", "Runs under 'node' user (UID 1000), not root", false)
                        ),
                        "Use .dockerignore to exclude node_modules, tests, and local logs from build context."
                ),
                new DsaProblem(
                        "WDP-103",
                        topic,
                        topicTitle,
                        "C. Automated CI/CD Pipeline with GitHub Actions",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Create a GitHub Actions CI/CD workflow for automated linting, testing, and Docker deployment.",
                        "Workflow file .github/workflows/ci-cd.yml: runs on push to main. Jobs: 1) lint and unit tests, 2) build Docker image, 3) push image to container registry with commit SHA tag, 4) trigger deployment webhook.",
                        "Automated continuous integration and deployment pipeline ensuring clean code promotion.",
                        "Project Difficulty: Medium | Tech: GitHub Actions, CI/CD, Automated Testing, Docker Hub",
                        List.of(
                                new TestCase("Push failing unit test", "CI workflow fails on test step and blocks deployment", true, "Test gate active"),
                                new TestCase("Push passing commit to main", "All jobs pass; image tagged and pushed to registry", false),
                                new TestCase("Check workflow run time", "Caches npm dependencies to complete under 2 minutes", false)
                        ),
                        "Use GitHub Secrets for all registry credentials and deployment API keys."
                ),
                new DsaProblem(
                        "WDP-104",
                        topic,
                        topicTitle,
                        "D. Full-Stack Cloud Deployment (Vercel & Render)",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Deploy full-stack architecture with React frontend on Vercel and Express API on Render.",
                        "Configure frontend for production API URLs, setup vercel.json routing for SPA fallback, configure Render web service with environment secrets and health-check endpoint GET /health.",
                        "Live public application with decoupled frontend/backend and verified HTTPS CORS communication.",
                        "Project Difficulty: Medium | Tech: Vercel, Render, CORS, Cloud Architecture",
                        List.of(
                                new TestCase("Access frontend on vercel.app", "SPA loads with HTTPS and renders components", true, "Frontend live"),
                                new TestCase("API call to backend on onrender.com", "CORS headers allow Vercel origin; returns JSON data", false),
                                new TestCase("Direct URL refresh on /dashboard", "vercel.json rewrite prevents 404 and serves index.html", false)
                        ),
                        "Configure explicit CORS origins in backend instead of wildcard (*) in production."
                ),
                new DsaProblem(
                        "WDP-105",
                        topic,
                        topicTitle,
                        "E. Kubernetes Deployment Manifest with Auto-Scaling",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Author production Kubernetes manifests with Deployments, Services, Ingress, and HPA.",
                        "k8s manifests: 1) Deployment (3 replicas, rolling update strategy, liveness/readiness probes), 2) Service (ClusterIP), 3) Ingress (TLS termination), 4) HorizontalPodAutoscaler (CPU > 70% threshold).",
                        "Declarative Kubernetes configuration ready for cloud cluster deployment.",
                        "Project Difficulty: Hard | Tech: Kubernetes, HPA, Ingress, Resource Limits",
                        List.of(
                                new TestCase("kubectl apply -f k8s/", "All pods, service, and ingress created successfully", true, "Manifests applied"),
                                new TestCase("Simulate high CPU traffic", "HPA scales pod replicas from 3 to 8 automatically", false),
                                new TestCase("Trigger rolling deployment", "Zero downtime during rollout; readiness probes ensure traffic hits ready pods", false)
                        ),
                        "Always define resource requests and limits (CPU/memory) on containers in Kubernetes."
                )
        );
    }

    // =========================================================================
    // MOBILE APP DEVELOPMENT (8 Topics x 5 Exercises)
    // =========================================================================

    // =========================================================================
    // MOBILE APP DEVELOPMENT - Flutter & Dart Cross-Platform Foundations (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildFlutterProblems() {
        String topic = "flutter";
        String topicTitle = "Flutter & Dart Cross-Platform Foundations";

        return List.of(
                new DsaProblem(
                        "FL-101",
                        topic,
                        topicTitle,
                        "A. Responsive Flutter Profile Card UI",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Build a modern profile card widget in Flutter with responsive layout and theming.",
                        "Flutter StatelessWidget rendering CircleAvatar, name/bio Text widgets, follower stats Row with custom dividers, and Follow/Message action Buttons.",
                        "Clean, pixel-perfect Flutter widget responsive across portrait and landscape screens.",
                        "Project Difficulty: Easy | Tech: Flutter Widgets, Dart, ThemeData",
                        List.of(
                                new TestCase("Render ProfileCard in MaterialApp", "Displays avatar, info rows, and buttons correctly", true, "UI rendered"),
                                new TestCase("Switch device orientation to landscape", "Layout wraps in SingleChildScrollView with no overflow errors", false),
                                new TestCase("Apply dark ThemeData", "Colors adapt seamlessly to dark color scheme", false)
                        ),
                        "Use SafeArea to ensure content is not obscured by system status bars or notches."
                ),
                new DsaProblem(
                        "FL-102",
                        topic,
                        topicTitle,
                        "B. Interactive Shopping Cart Counter with StatefulWidget",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Create a shopping cart item counter widget in Flutter managing internal state.",
                        "StatefulWidget supporting plus/minus buttons, dynamic quantity animation, min/max limits, and callback onQuantityChanged(int qty).",
                        "Smooth interactive widget handling state changes with animated transitions.",
                        "Project Difficulty: Easy | Tech: Flutter StatefulWidget, AnimatedSwitcher",
                        List.of(
                                new TestCase("Tap plus button", "Counter increments and fires onQuantityChanged callback", true, "State updated"),
                                new TestCase("Tap minus button at minimum 1", "Minus button becomes disabled; quantity remains 1", false),
                                new TestCase("Rapid button clicks", "AnimatedSwitcher smoothly fades between numbers", false)
                        ),
                        "Call setState() only around the minimal variable updates needed."
                ),
                new DsaProblem(
                        "FL-103",
                        topic,
                        topicTitle,
                        "C. REST API Client with Flutter FutureBuilder",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build a Flutter list screen consuming a REST API using the http package and FutureBuilder.",
                        "Fetch list of articles from REST endpoint. Use FutureBuilder to render CircularProgressIndicator during loading, article ListView on success, and retry button on network error.",
                        "Robust data-driven Flutter screen handling all asynchronous connection states.",
                        "Project Difficulty: Medium | Tech: Flutter http, FutureBuilder, ListView.builder",
                        List.of(
                                new TestCase("Launch screen with network connection", "Shows spinner then smoothly displays article list", true, "Data loaded"),
                                new TestCase("Simulate HTTP 500 error", "Renders error icon and 'Retry' button", false),
                                new TestCase("Pull to refresh on list", "RefreshIndicator re-fetches latest articles", false)
                        ),
                        "Extract API fetching into a dedicated repository class instead of calling http directly in build()."
                ),
                new DsaProblem(
                        "FL-104",
                        topic,
                        topicTitle,
                        "D. Multi-Page Navigation with Hero Animations",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Implement multi-screen navigation in Flutter with shared element Hero animations.",
                        "Grid of recipe cards. Tapping a card transitions to RecipeDetailScreen using Navigator.push with a shared Hero image tag and smooth slide transition.",
                        "Fluid navigation flow with seamless shared element transitions.",
                        "Project Difficulty: Medium | Tech: Flutter Navigator, Hero Widget, PageRouteBuilder",
                        List.of(
                                new TestCase("Tap recipe card on grid", "Card image expands into detail header via Hero animation", true, "Hero transition verified"),
                                new TestCase("Press back button", "Hero image collapses back to grid cell position smoothly", false),
                                new TestCase("Pass arguments to detail screen", "Detail screen receives Recipe model object accurately", false)
                        ),
                        "Ensure Hero tags are unique per item in the list (e.g. 'recipe-image-${recipe.id}')."
                ),
                new DsaProblem(
                        "FL-105",
                        topic,
                        topicTitle,
                        "E. Offline-First Task Manager with SQLite (sqflite)",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Architect an offline-first task management app in Flutter with local SQLite persistence.",
                        "Complete task manager using sqflite: create database, CRUD operations, transactions, category filters, and search query. State sync with local DB.",
                        "Production offline-first mobile app with instantaneous UI updates and SQLite persistence.",
                        "Project Difficulty: Hard | Tech: Flutter, Dart, sqflite, SQLite, Path Provider",
                        List.of(
                                new TestCase("Create new task offline", "Task written to SQLite and appears immediately in UI", true, "Task persisted"),
                                new TestCase("Kill app and relaunch", "Tasks loaded from local database on startup", false),
                                new TestCase("Search task by keyword", "SQL LIKE query filters tasks with low latency", false)
                        ),
                        "Keep database connections as singletons and run heavy queries asynchronously."
                )
        );
    }

    // =========================================================================
    // MOBILE APP DEVELOPMENT - React Native & Expo Mobile Framework (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildReactNativeProblems() {
        String topic = "reactnative";
        String topicTitle = "React Native & Expo Mobile Framework";

        return List.of(
                new DsaProblem(
                        "RN-101",
                        topic,
                        topicTitle,
                        "A. Custom Touch Feedback Button & Theme System",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Build a customizable button component with haptic feedback and dynamic styling in React Native.",
                        "Component wrapping Pressable with animated opacity, disabled states, variant colors (primary, secondary, outline), and expo-haptics trigger.",
                        "Polished mobile touch component with immediate visual and tactile feedback.",
                        "Project Difficulty: Easy | Tech: React Native, StyleSheet, Pressable, Expo",
                        List.of(
                                new TestCase("Press button", "Triggers subtle haptic feedback and scales to 0.98 opacity", true, "Feedback active"),
                                new TestCase("Set disabled={true}", "Renders muted background and ignores touch gestures", false),
                                new TestCase("Pass variant='outline'", "Renders bordered transparent button with colored text", false)
                        ),
                        "Use StyleSheet.create for all styles to optimize native bridge performance."
                ),
                new DsaProblem(
                        "RN-102",
                        topic,
                        topicTitle,
                        "B. Infinite Scrolling Image Feed with FlatList",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Implement an infinite scrolling image feed using React Native FlatList.",
                        "Feed rendering image cards with author info, likes counter, onEndReached pagination trigger, ListHeaderComponent, and RefreshControl pull-to-refresh.",
                        "Performant 60fps scrolling list with automatic pagination and memory optimization.",
                        "Project Difficulty: Easy | Tech: React Native FlatList, RefreshControl, Image",
                        List.of(
                                new TestCase("Scroll to bottom of feed", "onEndReached fires and fetches next page of image items", true, "Pagination triggered"),
                                new TestCase("Pull down from top of list", "Refresh spinner activates and reloads initial dataset", false),
                                new TestCase("Inspect memory usage", "FlatList recycles offscreen views with initialNumToRender", false)
                        ),
                        "Always provide a keyExtractor function returning a unique string ID."
                ),
                new DsaProblem(
                        "RN-103",
                        topic,
                        topicTitle,
                        "C. Cross-Platform Camera & Media Picker with Expo",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build an image capture and gallery picker module using Expo Camera and ImagePicker.",
                        "Interface allowing user to take photo with camera or select from photo library. Handle iOS/Android permissions gracefully, show preview, and crop image before saving.",
                        "Full-featured media acquisition screen with robust permission flows.",
                        "Project Difficulty: Medium | Tech: Expo Camera, Expo ImagePicker, Permissions",
                        List.of(
                                new TestCase("Request camera permission", "Displays native permission prompt with custom reason", true, "Permission requested"),
                                new TestCase("Take photo with camera", "Returns local URI and renders high-res preview", false),
                                new TestCase("User denies permission", "Renders helpful message directing user to app settings", false)
                        ),
                        "Check and request permissions using ImagePicker.requestMediaLibraryPermissionsAsync()."
                ),
                new DsaProblem(
                        "RN-104",
                        topic,
                        topicTitle,
                        "D. Bottom Tab & Native Stack Navigation with React Navigation",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Configure a complete mobile navigation structure using React Navigation 6/7.",
                        "Navigation hierarchy: Bottom Tab Navigator (Feed, Search, Profile) with vector icons and badge counts, with nested Native Stack Navigator for detail screens.",
                        "Intuitive native mobile navigation hierarchy supporting deep linking and back stacks.",
                        "Project Difficulty: Medium | Tech: React Navigation, Bottom Tabs, Native Stack",
                        List.of(
                                new TestCase("Tap Search tab", "Switches to Search screen and highlights tab icon", true, "Tab navigation verified"),
                                new TestCase("Navigate to ItemDetail from Feed", "Pushes onto stack with native header back button", false),
                                new TestCase("Update notification badge count", "Tab bar displays numeric badge on icon", false)
                        ),
                        "Use @react-navigation/native-stack for true native navigation transitions."
                ),
                new DsaProblem(
                        "RN-105",
                        topic,
                        topicTitle,
                        "E. Persistent Offline Store with Zustand & MMKV",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Build a high-performance offline state management layer using Zustand and react-native-mmkv.",
                        "Global state store for user preferences, auth session, and cached feed data. Sync state synchronously with MMKV storage for instant app cold starts.",
                        "Ultra-fast persistent state store with zero-delay startup hydration.",
                        "Project Difficulty: Hard | Tech: Zustand, react-native-mmkv, Mobile State",
                        List.of(
                                new TestCase("Update theme preference to 'dark'", "Zustand updates memory state and writes to MMKV", true, "State updated"),
                                new TestCase("Cold launch application", "State immediately available on first render without flash", false),
                                new TestCase("Perform 1000 store writes", "MMKV completes writes in under 5ms", false)
                        ),
                        "MMKV is significantly faster than AsyncStorage for synchronous key-value storage."
                )
        );
    }

    // =========================================================================
    // MOBILE APP DEVELOPMENT - Native Android: Kotlin & Jetpack Compose (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildKotlinProblems() {
        String topic = "kotlin";
        String topicTitle = "Native Android: Kotlin & Jetpack Compose";

        return List.of(
                new DsaProblem(
                        "AK-101",
                        topic,
                        topicTitle,
                        "A. Jetpack Compose Interactive Counter Card",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Build an interactive material card in Jetpack Compose managing internal remember state.",
                        "@Composable function rendering a Material3 Card with title, animated number text using AnimatedContent, Increment/Decrement OutlinedIconButtons, and reset confirmation dialog.",
                        "Modern declarative Android UI component following Material3 design guidelines.",
                        "Project Difficulty: Easy | Tech: Kotlin, Jetpack Compose, Material3",
                        List.of(
                                new TestCase("Click '+' button", "AnimatedContent smoothly transitions counter to next number", true, "Compose state active"),
                                new TestCase("Click Reset button", "AlertDialog appears requesting confirmation before resetting", false),
                                new TestCase("Device configuration change", "rememberSaveable preserves counter state across rotation", false)
                        ),
                        "Use rememberSaveable to retain state across Android configuration changes."
                ),
                new DsaProblem(
                        "AK-102",
                        topic,
                        topicTitle,
                        "B. Asynchronous Image Gallery with Coil & LazyVerticalGrid",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Create an asynchronous image grid in Jetpack Compose using Coil.",
                        "LazyVerticalGrid displaying image items fetched from URL list. Use AsyncImage from Coil with crossfade animation, placeholder shimmer, and error fallback icon.",
                        "Smooth scrolling Android grid loading remote images efficiently.",
                        "Project Difficulty: Easy | Tech: Kotlin, Jetpack Compose, Coil, LazyGrid",
                        List.of(
                                new TestCase("Scroll image grid", "LazyGrid recycles items seamlessly; images load with crossfade", true, "Images loading"),
                                new TestCase("Load with broken image URL", "Coil renders error drawable fallback icon", false),
                                new TestCase("Inspect grid columns", "GridCells.Adaptive(minSize = 120.dp) adapts to tablet/phone width", false)
                        ),
                        "Use Crossfade(true) in AsyncImage for pleasant image load transitions."
                ),
                new DsaProblem(
                        "AK-103",
                        topic,
                        topicTitle,
                        "C. MVVM Architecture with ViewModel & StateFlow",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Implement Android MVVM pattern using Kotlin Coroutines, StateFlow, and ViewModel.",
                        "TaskViewModel exposing UiState sealed class (Loading, Success(tasks), Error). Fetch data asynchronously using viewModelScope.launch and collect in Compose with collectAsStateWithLifecycle.",
                        "Clean Android architecture separating business logic from declarative UI.",
                        "Project Difficulty: Medium | Tech: Kotlin, Android ViewModel, StateFlow, Coroutines",
                        List.of(
                                new TestCase("ViewModel initialization", "UiState transitions from Loading to Success with task list", true, "State flow verified"),
                                new TestCase("Simulate network failure", "UiState emits Error; UI renders retry snackbar", false),
                                new TestCase("Rotate screen", "ViewModel retains state without re-triggering network call", false)
                        ),
                        "Always collect StateFlow in Compose using collectAsStateWithLifecycle() for lifecycle safety."
                ),
                new DsaProblem(
                        "AK-104",
                        topic,
                        topicTitle,
                        "D. Local SQLite Database with Room & Flow",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build a local persistence layer in Android using Room ORM and Kotlin Flow.",
                        "Room setup: @Entity UserNote, @Dao with insert, update, delete, and getAllNotes(): Flow<List<UserNote>>, and @Database singleton with migrations.",
                        "Reactive local data persistence automatically updating UI when database changes.",
                        "Project Difficulty: Medium | Tech: Android Room, Kotlin Flow, SQLite, DAO",
                        List.of(
                                new TestCase("Insert new note via DAO", "Room emits updated List<UserNote> via Flow automatically", true, "Reactive DB verified"),
                                new TestCase("Query note by keyword", "DAO search query returns matching filtered notes", false),
                                new TestCase("Delete note", "Note removed from SQLite and UI updates in real-time", false)
                        ),
                        "Room DAO methods returning Flow automatically run queries on background dispatchers."
                ),
                new DsaProblem(
                        "AK-105",
                        topic,
                        topicTitle,
                        "E. Modular Clean Architecture with Hilt & Retrofit",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Architect an Android application following Clean Architecture principles with Hilt DI and Retrofit.",
                        "Multi-layer architecture: Domain (UseCases, Repository Interface), Data (Retrofit REST API, Room Cache, Repository Impl), Presentation (Compose, ViewModel). Dependency injection via Hilt.",
                        "Enterprise-grade Android app architecture with complete testability and separation of concerns.",
                        "Project Difficulty: Hard | Tech: Hilt DI, Retrofit2, Clean Architecture, Repository Pattern",
                        List.of(
                                new TestCase("Inject Repository into UseCase", "Hilt provides singleton instances via @Inject constructor", true, "DI verified"),
                                new TestCase("Execute GetUsersUseCase", "Repository checks local Room cache first, then fetches from Retrofit", false),
                                new TestCase("Run Unit Test on ViewModel", "Mock repository verifies UseCase execution and state transitions", false)
                        ),
                        "Annotate Hilt modules with @InstallIn(SingletonComponent::class) for application-wide dependencies."
                )
        );
    }

    // =========================================================================
    // MOBILE APP DEVELOPMENT - Native iOS: Swift & SwiftUI Architecture (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildSwiftProblems() {
        String topic = "swift";
        String topicTitle = "Native iOS: Swift & SwiftUI Architecture";

        return List.of(
                new DsaProblem(
                        "SW-101",
                        topic,
                        topicTitle,
                        "A. Declarative Profile View in SwiftUI",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Create a polished iOS profile screen using SwiftUI declarative views and modifiers.",
                        "SwiftUI View composing AsyncImage, VStack/HStack, custom SF Symbols, Section grouping, and Edit Profile modal sheet presentation with @State toggle.",
                        "Native iOS screen utilizing standard SwiftUI design patterns and system iconography.",
                        "Project Difficulty: Easy | Tech: Swift, SwiftUI, SF Symbols, View Modifiers",
                        List.of(
                                new TestCase("Render ProfileView in Preview", "Displays avatar, bio, and settings list with SF Symbols", true, "SwiftUI rendered"),
                                new TestCase("Tap 'Edit Profile' button", "Presents modal view using .sheet(isPresented: $showEdit)", false),
                                new TestCase("Toggle dark mode preview", "Adapts color scheme using Color(uiColor: .systemBackground)", false)
                        ),
                        "Use SF Symbols via Image(systemName:) for consistent iOS system iconography."
                ),
                new DsaProblem(
                        "SW-102",
                        topic,
                        topicTitle,
                        "B. Interactive Task List with Swipe Actions",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Build an interactive task list in SwiftUI with swipe actions and animations.",
                        "SwiftUI List with ForEach over @State task items. Implement .swipeActions for delete and pin, .onMove for drag reordering, and .animation for list changes.",
                        "Responsive iOS task list with standard gesture interactions.",
                        "Project Difficulty: Easy | Tech: SwiftUI List, @State, Swipe Actions",
                        List.of(
                                new TestCase("Swipe task item to left", "Reveals red 'Delete' action and removes item with animation", true, "Swipe action verified"),
                                new TestCase("Swipe task item to right", "Reveals blue 'Pin' action and moves item to top of list", false),
                                new TestCase("Drag item to reorder", ".onMove modifier updates array order", false)
                        ),
                        "Make data models conform to Identifiable for clean ForEach integration."
                ),
                new DsaProblem(
                        "SW-103",
                        topic,
                        topicTitle,
                        "C. MVVM Architecture with ObservableObject & Combine",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Implement MVVM pattern in SwiftUI using @StateObject, ObservableObject, and URLSession.",
                        "CryptoPriceViewModel conforming to ObservableObject with @Published var coins: [Coin]. Fetch real-time market data asynchronously with async/await and update SwiftUI View.",
                        "Reactive SwiftUI screen bound cleanly to ViewModel with asynchronous updates.",
                        "Project Difficulty: Medium | Tech: Swift Concurrency, @Published, ObservableObject, async/await",
                        List.of(
                                new TestCase("ViewModel loads crypto data", "@Published coins array updates and triggers View body refresh", true, "Async fetch verified"),
                                new TestCase("Simulate network timeout", "ViewModel sets errorMessage; View displays alert banner", false),
                                new TestCase("Pull to refresh list", ".refreshable modifier awaits viewModel.fetchCoins()", false)
                        ),
                        "Ensure UI state mutations in ViewModels happen on the MainActor (@MainActor)."
                ),
                new DsaProblem(
                        "SW-104",
                        topic,
                        topicTitle,
                        "D. CoreData Persistence with SwiftUI @FetchRequest",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build a local persistence layer in iOS using CoreData and SwiftUI @FetchRequest.",
                        "CoreData model for JournalEntry. PersistenceController singleton managing NSPersistentContainer. SwiftUI View using @FetchRequest with NSSortDescriptor and NSPredicate filtering.",
                        "Robust CoreData integration with automatic SwiftUI view updates on context save.",
                        "Project Difficulty: Medium | Tech: iOS CoreData, @FetchRequest, NSPersistentContainer",
                        List.of(
                                new TestCase("Save new journal entry", "viewContext.save() persists entity to SQLite store", true, "CoreData saved"),
                                new TestCase("View observe fetch results", "@FetchRequest automatically animates new entry into list", false),
                                new TestCase("Filter entries by tag", "NSPredicate dynamically updates fetch results", false)
                        ),
                        "Always handle viewContext.save() inside a do-catch block to handle database errors."
                ),
                new DsaProblem(
                        "SW-105",
                        topic,
                        topicTitle,
                        "E. Custom Navigation Stack with Deep Linking",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Architect a scalable navigation system using SwiftUI NavigationStack and NavigationPath.",
                        "NavigationStack managing a programmatic NavigationPath. Support deep link URL parsing (myapp://item/123), push/pop to root, and state restoration.",
                        "Decoupled iOS navigation architecture supporting complex routing and deep links.",
                        "Project Difficulty: Hard | Tech: SwiftUI NavigationStack, NavigationPath, Deep Linking",
                        List.of(
                                new TestCase("Trigger deep link URL", "NavigationPath pushes target screen onto stack automatically", true, "Deep link routed"),
                                new TestCase("Tap 'Back to Home' from depth 4", "path.removeLast(path.count) pops directly to root view", false),
                                new TestCase("Inspect navigation destinations", "Uses .navigationDestination(for: Destination.self) for type safety", false)
                        ),
                        "Define an enum conforming to Hashable for all navigation destinations."
                )
        );
    }

    // =========================================================================
    // MOBILE APP DEVELOPMENT - Mobile State Management & Architecture (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildStateMgmtProblems() {
        String topic = "statemgmt";
        String topicTitle = "Mobile State Management & Architecture";

        return List.of(
                new DsaProblem(
                        "SM-101",
                        topic,
                        topicTitle,
                        "A. BLoC Counter & Theme Switcher in Flutter",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Implement the BLoC (Business Logic Component) pattern for counter and theming in Flutter.",
                        "flutter_bloc implementation: CounterBloc with IncrementEvent and DecrementEvent, and ThemeCubit toggling light/dark mode. UI consuming BlocBuilder and BlocProvider.",
                        "Decoupled state architecture cleanly separating UI events from state transitions.",
                        "Project Difficulty: Easy | Tech: Flutter, flutter_bloc, Cubit, Streams",
                        List.of(
                                new TestCase("Dispatch IncrementEvent", "CounterBloc emits state + 1; BlocBuilder rebuilds count widget", true, "BLoC event processed"),
                                new TestCase("Toggle Theme switch", "ThemeCubit emits new ThemeData; app theme switches instantly", false),
                                new TestCase("Test Bloc in unit test", "blocTest verifies emitted states match expected sequence", false)
                        ),
                        "Use Cubit for simple state and full BLoC when handling complex event-driven workflows."
                ),
                new DsaProblem(
                        "SM-102",
                        topic,
                        topicTitle,
                        "B. Riverpod StateProvider & AsyncNotifier in Flutter",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Build reactive state management using Flutter Riverpod 2.0.",
                        "Riverpod setup: StateProvider for search query, and AsyncNotifierProvider for async user fetching. ConsumerWidget listening with ref.watch and AsyncValue.when handling loading/data/error.",
                        "Compile-safe, decoupled state management without BuildContext dependency.",
                        "Project Difficulty: Easy | Tech: Flutter Riverpod, AsyncNotifier, ConsumerWidget",
                        List.of(
                                new TestCase("Update search StateProvider", "ref.watch triggers re-computation of filtered provider", true, "Riverpod reactive"),
                                new TestCase("AsyncNotifier fetch", "AsyncValue.when renders loading spinner then user list", false),
                                new TestCase("Call ref.invalidate(userProvider)", "Forces provider to re-fetch and refresh data", false)
                        ),
                        "Prefer AsyncNotifier over FutureProvider when supporting mutations and manual refresh."
                ),
                new DsaProblem(
                        "SM-103",
                        topic,
                        topicTitle,
                        "C. Clean Architecture State Flow with Redux",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Implement Redux unidirectional data flow for mobile state management.",
                        "Redux architecture: AppState (immutable), Actions (FetchItems, AddItem), Reducer (pure function), and Middleware for asynchronous API side-effects.",
                        "Predictable unidirectional data flow with centralized state and action logging.",
                        "Project Difficulty: Medium | Tech: Redux, Unidirectional Data Flow, Middleware",
                        List.of(
                                new TestCase("Dispatch AddItemAction", "Reducer calculates new state immutably; store notifies listeners", true, "Action dispatched"),
                                new TestCase("Dispatch AsyncFetchAction", "Middleware intercepts, makes API call, and dispatches SuccessAction", false),
                                new TestCase("Inspect action log", "Every action and state delta logged sequentially for debugging", false)
                        ),
                        "Reducers must remain pure functions without side-effects or direct state mutations."
                ),
                new DsaProblem(
                        "SM-104",
                        topic,
                        topicTitle,
                        "D. Zustand Multi-Store Architecture with Persistence",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Develop modular state stores using Zustand with middleware and selector optimization.",
                        "Zustand stores: useAuthStore (token, login/logout), useCartStore (items, totals), useSettingsStore. Implement persist middleware and selective subscriptions to prevent unnecessary re-renders.",
                        "Scalable multi-store mobile architecture with optimal render performance.",
                        "Project Difficulty: Medium | Tech: Zustand, React Native, Store Selectors, Middleware",
                        List.of(
                                new TestCase("Component subscribes to cart count only", "Cart item price changes; component does not re-render", true, "Selector optimized"),
                                new TestCase("Login via useAuthStore", "Session persists to storage and updates auth state across app", false),
                                new TestCase("Call resetAllStores()", "Middleware clears state across all stores on logout", false)
                        ),
                        "Use atomic selectors like useCartStore(state => state.items.length) to minimize re-renders."
                ),
                new DsaProblem(
                        "SM-105",
                        topic,
                        topicTitle,
                        "E. Complex Real-Time State Sync with WebSockets",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Build a real-time collaborative state sync engine over WebSockets with optimistic updates.",
                        "Mobile state manager handling WebSocket message streams, optimistic UI updates, conflict resolution with vector clocks, and offline action queue with replay on reconnect.",
                        "Resilient real-time state synchronization engine handling network drops seamlessly.",
                        "Project Difficulty: Hard | Tech: WebSockets, Optimistic UI, Conflict Resolution, Offline Queue",
                        List.of(
                                new TestCase("User performs action offline", "UI updates optimistically; action stored in persistent queue", true, "Optimistic update"),
                                new TestCase("Re-establish WebSocket connection", "Queue replays in order; server validates and confirms state", false),
                                new TestCase("Simultaneous edit conflict", "Vector clock resolves conflict deterministically", false)
                        ),
                        "Always assign unique client-generated UUIDs to optimistic actions for tracking."
                )
        );
    }

    // =========================================================================
    // MOBILE APP DEVELOPMENT - Networking, REST APIs & Firebase Backend (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildMobileApiProblems() {
        String topic = "mobileapi";
        String topicTitle = "Networking, REST APIs & Firebase Backend";

        return List.of(
                new DsaProblem(
                        "MA-101",
                        topic,
                        topicTitle,
                        "A. Dio HTTP Client with Interceptors & Retry",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Configure a robust HTTP client in Flutter using Dio with interceptors and retry policies.",
                        "Dio instance configured with base URL, timeouts (10s), AuthInterceptor adding Bearer token, LoggingInterceptor, and RetryInterceptor with exponential backoff on 5xx errors.",
                        "Resilient mobile HTTP networking client with automatic authorization and retry logic.",
                        "Project Difficulty: Easy | Tech: Flutter, Dio, HTTP Interceptors, Exponential Backoff",
                        List.of(
                                new TestCase("Send API request", "AuthInterceptor automatically attaches 'Authorization: Bearer <token>'", true, "Token attached"),
                                new TestCase("Simulate transient 503 error", "RetryInterceptor attempts 3 retries with exponential delay", false),
                                new TestCase("Network timeout occurs", "DioException caught and formatted into user-friendly message", false)
                        ),
                        "Create a unified error mapper that converts DioExceptions into domain Failure objects."
                ),
                new DsaProblem(
                        "MA-102",
                        topic,
                        topicTitle,
                        "B. Firebase Authentication Integration (Email & Google)",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Implement Firebase Authentication for user registration, login, and password reset.",
                        "Firebase Auth service supporting createUserWithEmailAndPassword, signInWithEmailAndPassword, GoogleAuthProvider sign-in, authStateChanges stream listener, and sendPasswordResetEmail.",
                        "Secure authentication service integrating seamlessly with mobile app navigation.",
                        "Project Difficulty: Easy | Tech: Firebase Auth, Flutter/React Native, OAuth",
                        List.of(
                                new TestCase("Register with new email", "Firebase creates user account and sends verification email", true, "User created"),
                                new TestCase("Sign in with correct credentials", "authStateChanges stream emits User object and navigates to Home", false),
                                new TestCase("Sign in with incorrect password", "Catches 'wrong-password' code and displays error message", false)
                        ),
                        "Listen to authStateChanges() stream at the root of the app to handle session routing."
                ),
                new DsaProblem(
                        "MA-103",
                        topic,
                        topicTitle,
                        "C. Cloud Firestore Real-Time CRUD & Queries",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build a real-time database module using Cloud Firestore with structured queries and offline cache.",
                        "Firestore repository supporting collection snapshots (real-time stream), document creation with serverTimestamp(), compound queries (where + orderBy), and offline cache configuration.",
                        "Real-time reactive data layer keeping mobile UI synchronized with cloud database.",
                        "Project Difficulty: Medium | Tech: Cloud Firestore, Real-Time Streams, Compound Queries",
                        List.of(
                                new TestCase("Add document to collection", "New item written to Firestore with server timestamp", true, "Document created"),
                                new TestCase("Listen to query stream", "Stream updates instantly when another client modifies data", false),
                                new TestCase("Execute compound query", "Requires composite index; Firestore console index link generated", false)
                        ),
                        "Compound queries with multiple equality and range filters require composite Firestore indexes."
                ),
                new DsaProblem(
                        "MA-104",
                        topic,
                        topicTitle,
                        "D. Firebase Cloud Messaging (FCM) Push Notifications",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Integrate Firebase Cloud Messaging for foreground, background, and notification tap handling.",
                        "FCM setup: request user permission (iOS/Android 13+), retrieve FCM device token, handle onMessage (foreground local notification banner), and handle onNotificationOpenedApp (deep link navigation).",
                        "Complete push notification lifecycle management with deep link routing.",
                        "Project Difficulty: Medium | Tech: FCM, Push Notifications, Deep Linking",
                        List.of(
                                new TestCase("Receive push while app in background", "System displays native notification banner", true, "Push received"),
                                new TestCase("Tap notification with payload '{route: /promo/1}'", "App opens and navigates directly to target screen", false),
                                new TestCase("Receive push in foreground", "Local notification banner displays without interrupting user", false)
                        ),
                        "On Android 13+, POST_NOTIFICATIONS runtime permission must be requested explicitly."
                ),
                new DsaProblem(
                        "MA-105",
                        topic,
                        topicTitle,
                        "E. Offline-First Sync Engine with Background Queue",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Architect a full offline-first data sync engine with local SQLite cache and cloud API.",
                        "Sync engine: 1) Reads serve from local SQLite immediately, 2) Writes modify local DB and append mutation to SyncQueue, 3) Background worker syncs queue to cloud REST/GraphQL API when online.",
                        "Zero-latency offline-first mobile architecture with bulletproof data synchronization.",
                        "Project Difficulty: Hard | Tech: SQLite, Background Sync, WorkManager, Conflict Resolution",
                        List.of(
                                new TestCase("Create 5 records while offline", "Records available in local UI instantly; 5 mutations queued", true, "Offline queue active"),
                                new TestCase("Device connects to WiFi", "Background worker processes mutations sequentially with server", false),
                                new TestCase("Conflict detected on sync", "Last-Write-Wins or custom merge strategy resolves conflict", false)
                        ),
                        "Use unique client-side UUIDs as primary keys so records can be created offline."
                )
        );
    }

    // =========================================================================
    // MOBILE APP DEVELOPMENT - Local Mobile Storage: SQLite, Room & Realm (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildSqliteProblems() {
        String topic = "sqlite";
        String topicTitle = "Local Mobile Storage: SQLite, Room & Realm";

        return List.of(
                new DsaProblem(
                        "SQ-101",
                        topic,
                        topicTitle,
                        "A. Basic SQLite CRUD Operations & Table Setup",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Create a local SQLite database helper for managing expenses in a mobile app.",
                        "SQLite helper: CREATE TABLE expenses (id INTEGER PRIMARY KEY, title TEXT, amount REAL, category TEXT, date INTEGER). Implement insert, queryAll, update, and delete methods.",
                        "Reliable SQLite database helper class with parameterized SQL queries.",
                        "Project Difficulty: Easy | Tech: SQLite, SQL DDL, Mobile Database",
                        List.of(
                                new TestCase("Insert expense item", "Record inserted with autoincrement ID", true, "Insert verified"),
                                new TestCase("Query all expenses", "Returns List of Expense objects mapped from query cursor", false),
                                new TestCase("Delete expense by ID", "DELETE query executes and removes specified row", false)
                        ),
                        "Always use parameterized queries (?) to prevent SQL injection vulnerabilities."
                ),
                new DsaProblem(
                        "SQ-102",
                        topic,
                        topicTitle,
                        "B. Database Schema Migrations (v1 to v2)",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Implement SQLite schema versioning and upgrade migrations.",
                        "Database OpenHelper with onUpgrade callback: upgrade schema from version 1 to version 2 by adding a 'notes' column (ALTER TABLE) and creating a new 'categories' lookup table without data loss.",
                        "Safe database migration pipeline preserving existing user data across app updates.",
                        "Project Difficulty: Easy | Tech: SQLite Migrations, onUpgrade, Schema Versioning",
                        List.of(
                                new TestCase("Launch app on v1 database", "Tables initialized with version 1 schema", true, "v1 initialized"),
                                new TestCase("Upgrade app to v2", "onUpgrade triggers: 'notes' column added, existing rows intact", false),
                                new TestCase("Query existing rows on v2", "Existing data preserved; 'notes' column defaults to NULL/empty", false)
                        ),
                        "Always test database migrations with existing mock databases before production release."
                ),
                new DsaProblem(
                        "SQ-103",
                        topic,
                        topicTitle,
                        "C. Complex SQLite Queries: Filtering & Aggregations",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Write performant SQLite queries for monthly budget analysis and reporting.",
                        "Queries: 1) Total spending grouped by category for current month, 2) Top 3 largest transactions, 3) Daily expense running total using window functions or group by, 4) Full-text search on titles.",
                        "Analytical SQLite queries generating aggregated reporting metrics with low latency.",
                        "Project Difficulty: Medium | Tech: SQLite, GROUP BY, Date Functions, Aggregation",
                        List.of(
                                new TestCase("Execute monthly summary query", "Returns categorized totals with percentage of total spend", true, "Aggregation verified"),
                                new TestCase("Search expenses by keyword", "Returns matching transactions matching title query", false),
                                new TestCase("Query performance check", "Indexes on category and date ensure sub-5ms query times", false)
                        ),
                        "Use SQLite strftime() function to extract year and month from unix epoch timestamps."
                ),
                new DsaProblem(
                        "SQ-104",
                        topic,
                        topicTitle,
                        "D. Encrypted Mobile Database with SQLCipher",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Secure sensitive mobile database storage using 256-bit AES encryption with SQLCipher.",
                        "Configure SQLCipher with a 256-bit encryption key stored in secure hardware keystore (Android Keystore / iOS Keychain). Demonstrate database cannot be read in plaintext from device storage.",
                        "Encrypted database storage meeting enterprise security and compliance standards.",
                        "Project Difficulty: Medium | Tech: SQLCipher, AES-256 Encryption, KeyStore, Security",
                        List.of(
                                new TestCase("Open database with correct key", "All tables and encrypted data accessible normally", true, "Decryption verified"),
                                new TestCase("Open raw .db file in external SQLite viewer", "File rejected with 'file is encrypted or not a database'", false),
                                new TestCase("Retrieve key from KeyStore", "Encryption key securely fetched without hardcoded secrets", false)
                        ),
                        "Never hardcode SQLCipher passphrases in application source code."
                ),
                new DsaProblem(
                        "SQ-105",
                        topic,
                        topicTitle,
                        "E. High-Performance Realm / ObjectBox Database",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Build a high-throughput mobile data store using Realm / ObjectBox with live reactive queries.",
                        "Data model using ObjectBox / Realm: entity relationships (1:M, M:N), reactive live query subscriptions, background write transactions, and benchmark storing 50,000 sensor data points.",
                        "Ultra-fast object database handling tens of thousands of records with zero-copy deserialization.",
                        "Project Difficulty: Hard | Tech: Realm / ObjectBox, Live Queries, Zero-Copy, High Throughput",
                        List.of(
                                new TestCase("Insert 50,000 records in batch", "Write transaction completes in under 200ms", true, "High throughput verified"),
                                new TestCase("Subscribe to reactive query", "UI receives instant callback when background thread updates objects", false),
                                new TestCase("Query with relationship traversal", "Traverses 1:M relations without expensive SQL joins", false)
                        ),
                        "Use batch transactions (putMany / writeAsync) when inserting large volumes of records."
                )
        );
    }

    // =========================================================================
    // MOBILE APP DEVELOPMENT - CI/CD, App Store & Google Play Deployment (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildPublishProblems() {
        String topic = "publish";
        String topicTitle = "CI/CD, App Store & Google Play Deployment";

        return List.of(
                new DsaProblem(
                        "PB-101",
                        topic,
                        topicTitle,
                        "A. Android Release Keystore & App Signing Setup",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Generate release signing keys and configure Gradle build signing for Android.",
                        "1) Generate PKCS12 release keystore using keytool, 2) Configure build.gradle signingConfigs with environment variables, 3) Build signed release App Bundle (.aab).",
                        "Production-ready signed Android App Bundle ready for Play Console upload.",
                        "Project Difficulty: Easy | Tech: Android Gradle, Keytool, App Signing, AAB",
                        List.of(
                                new TestCase("Generate keystore with keytool", "Creates secure .keystore file with RSA 2048-bit key", true, "Keystore generated"),
                                new TestCase("Run ./gradlew bundleRelease", "Builds signed app-release.aab using configured keystore", false),
                                new TestCase("Verify signature with jarsigner", "Reports valid signature verified against keystore certificate", false)
                        ),
                        "Store keystore credentials in environment variables, never committed to git."
                ),
                new DsaProblem(
                        "PB-102",
                        topic,
                        topicTitle,
                        "B. iOS Distribution Certificate & Provisioning Setup",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Configure Apple Developer certificates, App ID, and Provisioning Profiles in Xcode.",
                        "Setup Apple Distribution Certificate, App ID with push notification capabilities, App Store Provisioning Profile, and configure automatic/manual signing in Xcode build settings.",
                        "Validated Xcode project configuration ready for App Store archive and TestFlight.",
                        "Project Difficulty: Easy | Tech: iOS Xcode, Apple Developer, Provisioning Profiles",
                        List.of(
                                new TestCase("Validate signing in Xcode", "Signing status shows green with valid Provisioning Profile", true, "Signing verified"),
                                new TestCase("Archive project in Xcode", "Creates valid archive in Organizer without signing errors", false),
                                new TestCase("Export for App Store Connect", "Generates uploadable .ipa package with embedded profile", false)
                        ),
                        "Ensure Bundle Identifier in Xcode matches the App ID registered in Apple Developer Portal."
                ),
                new DsaProblem(
                        "PB-103",
                        topic,
                        topicTitle,
                        "C. Fastlane Automation for Android & iOS Deployment",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Automate mobile build and deployment workflows using Fastlane.",
                        "Fastlane setup: Fastfile with lanes: 1) lane :test (runs unit/UI tests), 2) lane :beta (builds and uploads to TestFlight / Firebase App Distribution), 3) lane :release (uploads metadata and build to stores).",
                        "Streamlined Fastlane automation pipeline reducing release time from hours to single command.",
                        "Project Difficulty: Medium | Tech: Fastlane, Ruby, TestFlight, Play Console API",
                        List.of(
                                new TestCase("Run 'fastlane beta'", "Automatically bumps build number, builds IPA/AAB, and uploads to TestFlight", true, "Lane executed"),
                                new TestCase("Run 'fastlane test'", "Executes test suite and generates HTML test summary report", false),
                                new TestCase("Match certificate sync", "fastlane match synchronizes certificates securely across team", false)
                        ),
                        "Use Fastlane Match with encrypted git repo to manage iOS certificates across team members."
                ),
                new DsaProblem(
                        "PB-104",
                        topic,
                        topicTitle,
                        "D. GitHub Actions Mobile CI/CD Pipeline Matrix",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build a multi-platform CI/CD workflow in GitHub Actions for Flutter / React Native.",
                        "GitHub Actions matrix workflow building both Android APK/AAB and iOS IPA on macOS runners. Cache dependencies, execute automated tests, sign artifacts, and upload release assets.",
                        "End-to-end automated mobile build pipeline triggering on git tags (v*).",
                        "Project Difficulty: Medium | Tech: GitHub Actions, macOS Runners, Build Matrix, CI/CD",
                        List.of(
                                new TestCase("Push git tag 'v1.2.0'", "Triggers workflow: builds Android and iOS artifacts concurrently", true, "Workflow triggered"),
                                new TestCase("Inspect build artifacts", "Signed AAB and IPA generated and attached to GitHub Release", false),
                                new TestCase("Check build cache", "Gradle and CocoaPods cached; cuts build duration by 60%", false)
                        ),
                        "Use base64 encoding to store keystores and provisioning profiles in GitHub Secrets."
                ),
                new DsaProblem(
                        "PB-105",
                        topic,
                        topicTitle,
                        "E. App Store Optimization (ASO) & Production Launch Checklist",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Prepare comprehensive app store metadata, screenshots, privacy manifest, and rollout strategy.",
                        "Deliverables: 1) Multi-language store metadata (title, subtitle, keywords, description), 2) Localized screenshot set across required device resolutions, 3) Apple Privacy Manifest (NSPrivacyAccessedAPITypes), 4) Staged rollout plan (10% to 100%).",
                        "Complete launch package ready for App Store and Google Play compliance review.",
                        "Project Difficulty: Hard | Tech: ASO, Apple Privacy Manifest, Staged Rollout, Compliance",
                        List.of(
                                new TestCase("Validate Apple Privacy Manifest", "Xcode reports all required API declarations present", true, "Manifest verified"),
                                new TestCase("Inspect screenshot dimensions", "Matches 6.7', 6.5', 5.5' iPhone and 12.9' iPad requirements", false),
                                new TestCase("Configure staged rollout", "Release starts at 10% on Day 1 and scales to 100% by Day 7", false)
                        ),
                        "Apple strictly requires Privacy Manifests for apps accessing sensitive APIs like UserDefaults."
                )
        );
    }

    // =========================================================================
    // AI & MACHINE LEARNING (8 Topics x 5 Exercises)
    // =========================================================================

    // =========================================================================
    // AI & MACHINE LEARNING - Machine Learning Foundations & Supervised Learning (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildMlFoundationsProblems() {
        String topic = "ml_foundations";
        String topicTitle = "Machine Learning Foundations & Supervised Learning";

        return List.of(
                new DsaProblem(
                        "MF-101",
                        topic,
                        topicTitle,
                        "A. Linear Regression from Scratch with NumPy",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Implement ordinary least squares linear regression from scratch using pure NumPy.",
                        "LinearRegression class with fit(X, y) computing weights via Normal Equation (X^T * X)^-1 * X^T * y, predict(X), and calculation of R^2 and MSE evaluation metrics. Compare against sklearn.",
                        "Verified mathematical implementation of linear regression matching scikit-learn outputs.",
                        "Project Difficulty: Easy | Tech: Python, NumPy, Linear Algebra, Normal Equation",
                        List.of(
                                new TestCase("Fit on synthetic dataset", "Weights match sklearn.linear_model.LinearRegression within 1e-5", true, "Weights verified"),
                                new TestCase("Predict on test samples", "Calculates correct continuous predictions", false),
                                new TestCase("Compute R^2 score", "R^2 matches sklearn.metrics.r2_score", false)
                        ),
                        "Add a bias column of ones to feature matrix X before computing the normal equation."
                ),
                new DsaProblem(
                        "MF-102",
                        topic,
                        topicTitle,
                        "B. Decision Tree Classifier from Scratch",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Build a binary decision tree classifier from scratch using Gini impurity.",
                        "DecisionTree class with recursive split finding based on minimum Gini impurity, max_depth stopping condition, predict() traversal, and accuracy calculation on the Iris dataset.",
                        "Working decision tree algorithm demonstrating recursive partitioning without external ML libraries.",
                        "Project Difficulty: Easy | Tech: Python, NumPy, Decision Trees, Gini Impurity",
                        List.of(
                                new TestCase("Fit on Iris dataset with max_depth=3", "Tree constructs split nodes and achieves > 90% accuracy", true, "Tree trained"),
                                new TestCase("Predict single sample", "Traverses tree nodes to leaf and returns correct class label", false),
                                new TestCase("Inspect split criteria", "Root node selects feature with highest information gain", false)
                        ),
                        "Gini impurity is 1 - sum(p_i^2) where p_i is the proportion of class i in the node."
                ),
                new DsaProblem(
                        "MF-103",
                        topic,
                        topicTitle,
                        "C. Comprehensive Binary Classification Evaluation Pipeline",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build an evaluation pipeline for binary classification with comprehensive metrics.",
                        "Pipeline calculating Confusion Matrix (TP, FP, TN, FN), Precision, Recall, F1-Score, ROC curve coordinates, and Area Under ROC Curve (ROC-AUC). Plot ROC curve with Matplotlib.",
                        "Complete model evaluation report assessing classifier performance across multiple thresholds.",
                        "Project Difficulty: Medium | Tech: Python, scikit-learn, Matplotlib, ROC-AUC",
                        List.of(
                                new TestCase("Evaluate classifier on imbalanced data", "Calculates precision, recall, and F1 accurately", true, "Metrics computed"),
                                new TestCase("Generate ROC curve", "Computes FPR and TPR across thresholds; AUC calculated correctly", false),
                                new TestCase("Inspect Confusion Matrix", "Visualizes heatmap with accurate TP, FP, FN, TN counts", false)
                        ),
                        "Use PR-AUC (Precision-Recall AUC) instead of ROC-AUC when evaluating highly imbalanced datasets."
                ),
                new DsaProblem(
                        "MF-104",
                        topic,
                        topicTitle,
                        "D. Random Forest Classifier with Feature Importance",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Implement a Random Forest ensemble classifier with feature importance analysis.",
                        "RandomForestClassifier ensemble with bagging, bootstrap sampling, random feature subspace selection per split, out-of-bag (OOB) error estimation, and feature importance calculation.",
                        "Robust ensemble model reducing variance with interpretable feature importance rankings.",
                        "Project Difficulty: Medium | Tech: Python, scikit-learn, Ensemble Learning, Bagging",
                        List.of(
                                new TestCase("Train Random Forest with 100 trees", "OOB score closely mirrors test set accuracy", true, "Ensemble trained"),
                                new TestCase("Extract feature importances", "Returns sorted ranking of top predictive features", false),
                                new TestCase("Compare variance against single tree", "Random Forest exhibits significantly lower test variance", false)
                        ),
                        "Setting max_features='sqrt' is the standard rule of thumb for classification random forests."
                ),
                new DsaProblem(
                        "MF-105",
                        topic,
                        topicTitle,
                        "E. End-to-End ML Pipeline with Model Registry",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Construct an automated end-to-end ML pipeline with preprocessing, cross-validation, and MLflow tracking.",
                        "Pipeline: 1) Data validation & imputation, 2) Feature scaling & encoding with ColumnTransformer, 3) 5-fold Stratified Cross-Validation, 4) Hyperparameter search with Optuna, 5) MLflow experiment logging.",
                        "Production-ready training pipeline logging parameters, metrics, and serialized model artifacts.",
                        "Project Difficulty: Hard | Tech: scikit-learn Pipeline, MLflow, Optuna, Hyperparameter Tuning",
                        List.of(
                                new TestCase("Execute automated pipeline", "Runs preprocessing, tuning, and logs run to MLflow", true, "Pipeline executed"),
                                new TestCase("Inspect best hyperparams", "Optuna finds optimal parameters in 30 trials", false),
                                new TestCase("Check MLflow registry", "Best model serialized and registered with version tag", false)
                        ),
                        "Always fit transformers only on training folds to prevent data leakage during cross-validation."
                )
        );
    }

    // =========================================================================
    // AI & MACHINE LEARNING - Linear Algebra & Calculus for AI (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildMathAiProblems() {
        String topic = "math_ai";
        String topicTitle = "Linear Algebra & Calculus for AI";

        return List.of(
                new DsaProblem(
                        "MAI-101",
                        topic,
                        topicTitle,
                        "A. Matrix Operations & Eigenvalues with NumPy",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Implement core linear algebra operations essential for machine learning.",
                        "NumPy module computing matrix multiplication, determinant, matrix inverse, matrix rank, eigenvalues, and eigenvectors of symmetric covariance matrices. Verify A * v = lambda * v.",
                        "Accurate linear algebra toolkit with verified spectral decomposition.",
                        "Project Difficulty: Easy | Tech: Python, NumPy, Linear Algebra, Eigenvalues",
                        List.of(
                                new TestCase("Compute eigenvalues of symmetric matrix", "Returns real eigenvalues and orthonormal eigenvectors", true, "Eigendecomposition verified"),
                                new TestCase("Verify characteristic equation", "A @ v equals lambda * v within 1e-6 numerical tolerance", false),
                                new TestCase("Compute matrix inverse", "A @ inv(A) equals Identity matrix", false)
                        ),
                        "Use np.linalg.eigh for symmetric/Hermitian matrices for faster and numerically stable computation."
                ),
                new DsaProblem(
                        "MAI-102",
                        topic,
                        topicTitle,
                        "B. Gradient Descent Optimization from Scratch",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Implement batch, stochastic (SGD), and mini-batch gradient descent optimizers from scratch.",
                        "Optimizers minimizing multivariate loss functions: calculate analytic gradients, update parameters with learning rate alpha, implement momentum, and plot loss convergence curves.",
                        "Working gradient-based optimizers demonstrating convergence rates across batch sizes.",
                        "Project Difficulty: Easy | Tech: Python, NumPy, Optimization, Gradient Descent",
                        List.of(
                                new TestCase("Run Gradient Descent on quadratic loss", "Converges to global minimum (loss < 1e-4) within 100 iterations", true, "Convergence verified"),
                                new TestCase("Compare SGD vs Batch GD", "Batch GD exhibits smooth curve; SGD oscillates but computes faster", false),
                                new TestCase("Add Momentum (gamma=0.9)", "Accelerates convergence through narrow loss ravines", false)
                        ),
                        "Normalize input features so gradient descent takes symmetric steps toward the minimum."
                ),
                new DsaProblem(
                        "MAI-103",
                        topic,
                        topicTitle,
                        "C. Principal Component Analysis (PCA) from Scratch",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Implement Principal Component Analysis (PCA) dimensionality reduction from scratch.",
                        "PCA class: 1) Mean-center data, 2) Compute covariance matrix, 3) Compute eigenvalues/eigenvectors, 4) Sort components by explained variance ratio, 5) Project data to k dimensions. Compare against sklearn.",
                        "Accurate mathematical PCA implementation reducing dimensionality while preserving maximum variance.",
                        "Project Difficulty: Medium | Tech: Python, NumPy, PCA, Dimensionality Reduction",
                        List.of(
                                new TestCase("Fit PCA on 10D dataset with k=2", "Projects to 2D; explained variance ratio sums to > 85%", true, "PCA projected"),
                                new TestCase("Compare against sklearn PCA", "Projections match scikit-learn PCA within sign ambiguity", false),
                                new TestCase("Generate Scree plot", "Visualizes cumulative explained variance across component count", false)
                        ),
                        "Data must be centered (zero mean) before computing the sample covariance matrix."
                ),
                new DsaProblem(
                        "MAI-104",
                        topic,
                        topicTitle,
                        "D. Multi-Variable Calculus & Backpropagation Engine",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build a micro autograd engine computing automatic differentiation and reverse-mode backpropagation.",
                        "Micrograd-style Value class tracking computational graphs: forward operations (+, *, pow, relu), backward pass computing partial derivatives via the Chain Rule, and gradient checks.",
                        "Lightweight automatic differentiation engine powering neural network weight updates.",
                        "Project Difficulty: Medium | Tech: Python, Autograd, Calculus, Chain Rule, Backprop",
                        List.of(
                                new TestCase("Compute d/dx (x^2 + 2x + 1) at x=3", "Forward pass gives 16; backward pass gives derivative 8", true, "Autograd verified"),
                                new TestCase("Train 2-layer MLP on XOR problem", "Loss decreases to < 0.05 and classifies XOR correctly", false),
                                new TestCase("Numerical gradient check", "Analytic gradient matches (f(x+h) - f(x-h)) / (2h) within 1e-5", false)
                        ),
                        "Always zero gradients (val.grad = 0) before executing a new backward pass."
                ),
                new DsaProblem(
                        "MAI-105",
                        topic,
                        topicTitle,
                        "E. Advanced Optimization: Adam & RMSprop Optimizers",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Implement modern neural network optimizers (AdaGrad, RMSprop, Adam) from scratch.",
                        "Optimizer suite: 1) AdaGrad (adaptive learning rates), 2) RMSprop (exponentially decaying average of squared gradients), 3) Adam (combining first and second moments with bias corrections).",
                        "Production-grade mathematical implementation of the Adam optimizer with bias correction.",
                        "Project Difficulty: Hard | Tech: Python, NumPy, Adam Optimizer, Deep Learning Math",
                        List.of(
                                new TestCase("Optimize Rosenbrock function with Adam", "Adam navigates curved ravine and converges to minimum", true, "Adam optimized"),
                                new TestCase("Inspect bias correction terms", "m_hat and v_hat properly correct for initial zero-bias", false),
                                new TestCase("Compare convergence against SGD", "Adam reaches minimum in 70% fewer iterations than standard SGD", false)
                        ),
                        "Default Adam hyperparameters (beta1=0.9, beta2=0.999, eps=1e-8) work well across most tasks."
                )
        );
    }

    // =========================================================================
    // AI & MACHINE LEARNING - Applied ML & Feature Engineering with Scikit-learn (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildScikitProblems() {
        String topic = "scikit";
        String topicTitle = "Applied ML & Feature Engineering with Scikit-learn";

        return List.of(
                new DsaProblem(
                        "SC-101",
                        topic,
                        topicTitle,
                        "A. Complete Data Preprocessing ColumnTransformer",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Build a robust preprocessing pipeline using scikit-learn ColumnTransformer.",
                        "Pipeline handling: numeric features (MedianImputer + StandardScaler), categorical features (MostFrequentImputer + OneHotEncoder with handle_unknown='ignore'), and Passthrough columns.",
                        "Reusable scikit-learn transformer preventing data leakage between train and test splits.",
                        "Project Difficulty: Easy | Tech: scikit-learn, ColumnTransformer, Pipeline, OneHotEncoder",
                        List.of(
                                new TestCase("Fit on training data and transform test", "Numeric scaled to zero-mean; categories one-hot encoded", true, "Transformation verified"),
                                new TestCase("Test data with unseen category", "handle_unknown='ignore' encodes unseen category as all zeros", false),
                                new TestCase("Check data leakage", "Transformer statistics derived solely from training split", false)
                        ),
                        "Never call fit() or fit_transform() on validation or test sets."
                ),
                new DsaProblem(
                        "SC-102",
                        topic,
                        topicTitle,
                        "B. Cross-Validation & Hyperparameter Tuning with GridSearchCV",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Implement hyperparameter optimization using StratifiedKFold and GridSearchCV.",
                        "Grid search over GradientBoostingClassifier hyperparameters (n_estimators, learning_rate, max_depth) using 5-fold Stratified CV with 'f1_weighted' scoring and learning curve visualization.",
                        "Exhaustive hyperparameter tuning report identifying optimal model configuration.",
                        "Project Difficulty: Easy | Tech: scikit-learn, GridSearchCV, StratifiedKFold, Model Selection",
                        List.of(
                                new TestCase("Run GridSearchCV across 18 parameter combinations", "Identifies best_params_ and best_score_ with 5-fold CV", true, "Grid search complete"),
                                new TestCase("Evaluate best estimator on holdout test", "Test score aligns closely with best cross-validation score", false),
                                new TestCase("Plot learning curves", "Diagnoses whether model suffers from high bias or high variance", false)
                        ),
                        "Use RandomizedSearchCV over GridSearchCV when exploring large parameter spaces."
                ),
                new DsaProblem(
                        "SC-103",
                        topic,
                        topicTitle,
                        "C. Feature Selection & Dimensionality Reduction Pipeline",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build a multi-stage feature selection pipeline to eliminate redundant variables.",
                        "Pipeline chaining: 1) VarianceThreshold (drop constant features), 2) SelectKBest with mutual_info_classif, 3) Recursive Feature Elimination (RFECV) with LogisticRegression, 4) Final classifier.",
                        "Optimized feature selection pipeline improving model interpretability and inference speed.",
                        "Project Difficulty: Medium | Tech: scikit-learn, Feature Selection, RFECV, Mutual Information",
                        List.of(
                                new TestCase("Input dataset with 100 features (80 noise)", "Pipeline selects top 20 truly informative features", true, "Features filtered"),
                                new TestCase("Inspect RFECV grid scores", "Identifies optimal number of features maximizing CV score", false),
                                new TestCase("Compare accuracy before vs after", "Accuracy maintained or improved with 80% fewer features", false)
                        ),
                        "Mutual information captures non-linear relationships, unlike Pearson correlation."
                ),
                new DsaProblem(
                        "SC-104",
                        topic,
                        topicTitle,
                        "D. Imbalanced Classification: SMOTE & Cost-Sensitive Learning",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Solve imbalanced classification using oversampling techniques and cost-sensitive learning.",
                        "Imbalance workflow: compare 1) Baseline model, 2) SMOTE (Synthetic Minority Over-sampling), 3) Class-weight balanced cost-sensitive learning, 4) Precision-Recall curve threshold optimization.",
                        "High-performing classifier handling 95:5 class imbalance without predicting only majority class.",
                        "Project Difficulty: Medium | Tech: imbalanced-learn, SMOTE, PR-AUC, Cost-Sensitive Learning",
                        List.of(
                                new TestCase("Train baseline on 95:5 imbalanced data", "High accuracy but near-zero recall on minority class", true, "Imbalance diagnosed"),
                                new TestCase("Apply SMOTE + Balanced Random Forest", "Minority class recall improves from 10% to 85%", false),
                                new TestCase("Tune classification threshold", "Maximizes F1 score on Precision-Recall curve", false)
                        ),
                        "Never apply SMOTE to the validation or test set; only oversample the training split."
                ),
                new DsaProblem(
                        "SC-105",
                        topic,
                        topicTitle,
                        "E. Production Model Stacking Ensemble & Calibration",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Build a production stacking classifier with probability calibration.",
                        "Ensemble: Base learners (RandomForest, LightGBM, LogisticRegression), Meta-learner (LogisticRegression with StackingClassifier), and CalibratedClassifierCV (isotonic/sigmoid) for accurate probabilities.",
                        "High-accuracy ensemble delivering calibrated prediction probabilities for production deployment.",
                        "Project Difficulty: Hard | Tech: StackingClassifier, Probability Calibration, Brier Score",
                        List.of(
                                new TestCase("Evaluate StackingClassifier vs individual models", "Stacking ensemble outperforms best single base learner", true, "Stacking verified"),
                                new TestCase("Plot calibration curve (reliability diagram)", "Calibrated probabilities closely match empirical frequencies", false),
                                new TestCase("Evaluate Brier score", "Calibrated model achieves significantly lower Brier score", false)
                        ),
                        "Stacking with out-of-fold predictions prevents the meta-learner from overfitting to base predictions."
                )
        );
    }

    // =========================================================================
    // AI & MACHINE LEARNING - Deep Learning, Neural Networks & PyTorch (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildDeepLearningProblems() {
        String topic = "deep_learning";
        String topicTitle = "Deep Learning, Neural Networks & PyTorch";

        return List.of(
                new DsaProblem(
                        "DL-101",
                        topic,
                        topicTitle,
                        "A. PyTorch Tensor Fundamentals & GPU Training",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Master PyTorch tensor operations, broadcasting, and GPU device management.",
                        "PyTorch script demonstrating: tensor creation, mathematical operations, slicing/broadcasting, device agnostic code (cuda/mps/cpu), autograd gradients (x.grad), and DataLoader batching.",
                        "Foundational PyTorch module with verified GPU acceleration and autograd tracking.",
                        "Project Difficulty: Easy | Tech: PyTorch, Tensors, Autograd, CUDA/MPS",
                        List.of(
                                new TestCase("Run tensor math on available GPU/MPS", "Operations execute on targeted hardware accelerator", true, "Hardware acceleration verified"),
                                new TestCase("Compute gradient of y = x^3 + 2x", "x.grad equals 3x^2 + 2 accurately after y.backward()", false),
                                new TestCase("Iterate PyTorch DataLoader", "Batches dataset with configured batch_size and shuffling", false)
                        ),
                        "Always use torch.no_grad() during inference and evaluation to conserve memory."
                ),
                new DsaProblem(
                        "DL-102",
                        topic,
                        topicTitle,
                        "B. Multi-Layer Perceptron (MLP) for Image Classification",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Build and train a Multi-Layer Perceptron in PyTorch on the Fashion-MNIST dataset.",
                        "PyTorch nn.Module with Linear layers, ReLU activations, Dropout(0.2), CrossEntropyLoss, and Adam optimizer. Include training loop with epoch loss and validation accuracy logging.",
                        "Trained PyTorch neural network achieving > 88% classification accuracy on Fashion-MNIST.",
                        "Project Difficulty: Easy | Tech: PyTorch, nn.Module, CrossEntropyLoss, Adam",
                        List.of(
                                new TestCase("Train MLP for 10 epochs", "Training loss steadily decreases; validation accuracy > 88%", true, "MLP trained"),
                                new TestCase("Evaluate test set confusion matrix", "Correctly classifies 10 fashion item categories", false),
                                new TestCase("Save model checkpoint", "torch.save(model.state_dict(), 'mlp.pth') serializes weights", false)
                        ),
                        "PyTorch nn.CrossEntropyLoss applies Softmax internally; do not add Softmax in the final layer."
                ),
                new DsaProblem(
                        "DL-103",
                        topic,
                        topicTitle,
                        "C. Convolutional Neural Network (CNN) with Residual Connections",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Architect a modern CNN with Residual Blocks and Batch Normalization for CIFAR-10.",
                        "PyTorch CNN architecture: Conv2d, BatchNorm2d, ReLU, MaxPool2d, custom ResidualBlock (skip connections), AdaptiveAvgPool2d, and Linear output. Train with learning rate scheduling.",
                        "Deep convolutional architecture with skip connections preventing vanishing gradients.",
                        "Project Difficulty: Medium | Tech: PyTorch, CNN, ResNet, Skip Connections, BatchNorm",
                        List.of(
                                new TestCase("Train ResNet model on CIFAR-10", "Achieves > 85% test accuracy with stable gradient flow", true, "CNN trained"),
                                new TestCase("Inspect skip connection", "Residual addition (x + F(x)) preserves gradient propagation", false),
                                new TestCase("Learning rate scheduler", "CosineAnnealingLR smoothly decays learning rate each epoch", false)
                        ),
                        "Use AdaptiveAvgPool2d((1, 1)) before final Linear layers to support variable input resolutions."
                ),
                new DsaProblem(
                        "DL-104",
                        topic,
                        topicTitle,
                        "D. Recurrent Neural Network (LSTM) for Sequence Modeling",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Implement an LSTM recurrent neural network in PyTorch for time-series forecasting.",
                        "PyTorch nn.LSTM module: multi-layer LSTM with hidden state management, sequence windowing dataset, multi-step ahead forecasting, and RMSE evaluation against baseline models.",
                        "Trained recurrent sequence model capturing temporal patterns and trends.",
                        "Project Difficulty: Medium | Tech: PyTorch, LSTM, Sequence Modeling, Time Series",
                        List.of(
                                new TestCase("Train LSTM on sequential dataset", "Captures cyclical trends with low validation RMSE", true, "LSTM trained"),
                                new TestCase("Forecast next 20 time steps", "Generates coherent multi-step autoregressive trajectory", false),
                                new TestCase("Inspect hidden state tensors", "Shape (num_layers, batch_size, hidden_dim) managed properly", false)
                        ),
                        "Set batch_first=True in nn.LSTM so input tensors follow standard (batch, seq, feature) shape."
                ),
                new DsaProblem(
                        "DL-105",
                        topic,
                        topicTitle,
                        "E. Generative Adversarial Network (DCGAN) for Image Synthesis",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Build and train a Deep Convolutional GAN (DCGAN) to synthesize realistic images.",
                        "DCGAN: 1) Generator (ConvTranspose2d, BatchNorm, ReLU, Tanh), 2) Discriminator (Conv2d, LeakyReLU, BatchNorm, Sigmoid), 3) Alternating minimax training loop with BCEWithLogitsLoss.",
                        "Working generative model synthesizing 64x64 images from random latent noise vectors.",
                        "Project Difficulty: Hard | Tech: PyTorch, DCGAN, Generative AI, Minimax Optimization",
                        List.of(
                                new TestCase("Train DCGAN for 25 epochs", "Discriminator and Generator losses reach stable dynamic equilibrium", true, "GAN trained"),
                                new TestCase("Sample latent vector z ~ N(0, I)", "Generator outputs plausible synthetic image grid", false),
                                new TestCase("Check mode collapse", "Generated batch displays visual diversity across classes", false)
                        ),
                        "Use LeakyReLU in the Discriminator and avoid MaxPool (use strided convolutions instead)."
                )
        );
    }

    // =========================================================================
    // AI & MACHINE LEARNING - Computer Vision, OpenCV & Convolutional Networks (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildVisionProblems() {
        String topic = "vision";
        String topicTitle = "Computer Vision, OpenCV & Convolutional Networks";

        return List.of(
                new DsaProblem(
                        "CV-101",
                        topic,
                        topicTitle,
                        "A. Digital Image Processing & Filtering with OpenCV",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Implement foundational image manipulation and filtering algorithms with OpenCV.",
                        "OpenCV script performing: color space conversions (BGR to RGB/HSV/Grayscale), Gaussian blurring, Canny edge detection, morphological operations (dilation/erosion), and thresholding.",
                        "Comprehensive image preprocessing pipeline for computer vision applications.",
                        "Project Difficulty: Easy | Tech: Python, OpenCV, Image Processing, Canny",
                        List.of(
                                new TestCase("Load image and apply Canny(100, 200)", "Extracts clean binary edge map with sharp contours", true, "Edges detected"),
                                new TestCase("Filter color mask in HSV space", "Isolates specific color ranges (e.g. green screen removal)", false),
                                new TestCase("Apply Gaussian Blur (5x5 kernel)", "Reduces high-frequency noise while preserving key boundaries", false)
                        ),
                        "Remember that OpenCV loads images in BGR format by default, not RGB."
                ),
                new DsaProblem(
                        "CV-102",
                        topic,
                        topicTitle,
                        "B. Contour Detection & Object Measurement",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Build an automatic object detector and dimension measurement tool using OpenCV contours.",
                        "Tool detecting objects on uniform background: findContours, calculate contourArea, perimeter, bounding box (cv2.boundingRect), minimum area rectangle, and center of mass moments.",
                        "Computer vision tool identifying and calculating spatial dimensions of detected objects.",
                        "Project Difficulty: Easy | Tech: OpenCV, Contours, Spatial Geometry",
                        List.of(
                                new TestCase("Process image with 5 distinct coins", "Detects all 5 coin contours and draws bounding boxes", true, "Contours detected"),
                                new TestCase("Calculate area and circularity", "Accurately distinguishes circular coins from rectangular cards", false),
                                new TestCase("Compute centroid coordinates", "Moments formula (M10/M00, M01/M00) finds exact centers", false)
                        ),
                        "Sort contours by cv2.contourArea descending to easily isolate primary foreground objects."
                ),
                new DsaProblem(
                        "CV-103",
                        topic,
                        topicTitle,
                        "C. Real-Time Face & Landmark Detection with MediaPipe",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Implement real-time face detection and 468-point facial landmark mesh with MediaPipe.",
                        "Video processing pipeline using OpenCV and MediaPipe: detect faces in webcam stream, map 468 3D facial landmarks, calculate eye aspect ratio (EAR) for blink/drowsiness detection.",
                        "Real-time 60fps facial landmark tracking and drowsiness alerting system.",
                        "Project Difficulty: Medium | Tech: OpenCV, Google MediaPipe, Facial Landmarks, EAR",
                        List.of(
                                new TestCase("Stream webcam video to MediaPipe", "Tracks 468 facial landmark mesh coordinates in real-time", true, "Facial mesh active"),
                                new TestCase("Close eyes for > 2 seconds", "Eye Aspect Ratio drops below 0.2; triggers drowsiness alert", false),
                                new TestCase("Measure processing FPS", "Maintains > 30 FPS processing on standard CPU", false)
                        ),
                        "MediaPipe expects RGB image input; convert frame with cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)."
                ),
                new DsaProblem(
                        "CV-104",
                        topic,
                        topicTitle,
                        "D. Object Detection with Pre-Trained YOLOv8",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Deploy a real-time object detection and tracking system using Ultralytics YOLOv8.",
                        "Object detection pipeline: load YOLOv8 pre-trained weights, run inference on video streams, extract bounding box coordinates, class labels, confidence scores, and count specific object classes.",
                        "High-accuracy real-time multi-object detection and counting pipeline.",
                        "Project Difficulty: Medium | Tech: YOLOv8, Ultralytics, Object Detection, Deep Learning",
                        List.of(
                                new TestCase("Run YOLOv8 on traffic video", "Detects and draws bounding boxes around cars, trucks, pedestrians", true, "Objects detected"),
                                new TestCase("Filter detections with conf > 0.6", "Suppresses false positives; outputs clean detection list", false),
                                new TestCase("Count unique vehicles crossing line", "Spatial tracking counts vehicles crossing virtual tripwire", false)
                        ),
                        "Use YOLOv8n (nano) for lightweight mobile/edge deployment and YOLOv8x for maximum accuracy."
                ),
                new DsaProblem(
                        "CV-105",
                        topic,
                        topicTitle,
                        "E. Custom Object Detection: Dataset Annotation & YOLO Fine-Tuning",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Train a custom object detector by annotating data and fine-tuning YOLOv8.",
                        "Workflow: 1) Dataset collection & YOLO format annotation (images, labels TXT), 2) Data augmentation with Albumentations, 3) Fine-tuning YOLOv8 on custom class, 4) mAP@0.5 and mAP@0.5:0.95 evaluation.",
                        "Custom deep learning vision model trained and evaluated on proprietary domain dataset.",
                        "Project Difficulty: Hard | Tech: YOLOv8 Fine-Tuning, Albumentations, mAP Evaluation",
                        List.of(
                                new TestCase("Train custom YOLOv8 on 500 images", "Validation mAP@0.5 reaches > 0.90 after 30 epochs", true, "Model fine-tuned"),
                                new TestCase("Inference on unseen test image", "Detects custom object class with high confidence", false),
                                new TestCase("Export model to ONNX format", "Successfully exports for cross-platform edge inference", false)
                        ),
                        "Ensure label bounding box coordinates are normalized between 0.0 and 1.0 (x_center, y_center, w, h)."
                )
        );
    }

    // =========================================================================
    // AI & MACHINE LEARNING - Natural Language Processing & Transformers (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildNlpProblems() {
        String topic = "nlp";
        String topicTitle = "Natural Language Processing & Transformers";

        return List.of(
                new DsaProblem(
                        "NP-101",
                        topic,
                        topicTitle,
                        "A. Text Preprocessing Pipeline with NLTK & spaCy",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Build a comprehensive NLP text preprocessing and normalization pipeline.",
                        "Pipeline performing: sentence segmentation, tokenization, lowercasing, stopword removal, lemmatization with spaCy, RegEx punctuation stripping, and vocabulary frequency analysis.",
                        "Standardized text preprocessing module converting raw documents into clean token sequences.",
                        "Project Difficulty: Easy | Tech: Python, spaCy, NLTK, RegEx, Text Cleaning",
                        List.of(
                                new TestCase("Process raw messy text document", "Removes HTML/URLs, lemmatizes words, drops stopwords", true, "Text cleaned"),
                                new TestCase("Inspect lemmatization output", "Transforms 'running', 'ran', 'runs' to base lemma 'run'", false),
                                new TestCase("Extract top 10 keywords", "Frequency distribution identifies most significant terms", false)
                        ),
                        "Lemmatization considers part-of-speech context, making it superior to rule-based stemming."
                ),
                new DsaProblem(
                        "NP-102",
                        topic,
                        topicTitle,
                        "B. TF-IDF Text Classifier & Sentiment Analysis",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Build a text sentiment classifier using TF-IDF vectorization and Logistic Regression.",
                        "Sentiment model: TfidfVectorizer with n-grams (1, 2) and sublinear TF scaling, LogisticRegression classifier trained on customer reviews, confusion matrix and top predictive words analysis.",
                        "Fast, interpretable sentiment analysis model classifying text polarity with high accuracy.",
                        "Project Difficulty: Easy | Tech: scikit-learn, TF-IDF, Sentiment Analysis, N-Grams",
                        List.of(
                                new TestCase("Classify positive review", "Predicts 'Positive' class with > 90% confidence score", true, "Sentiment classified"),
                                new TestCase("Inspect top coefficients", "Words like 'excellent', 'fast' have highest positive weights", false),
                                new TestCase("Evaluate on test dataset", "Achieves > 88% accuracy on review classification", false)
                        ),
                        "Setting ngram_range=(1, 2) allows the model to capture negation phrases like 'not good'."
                ),
                new DsaProblem(
                        "NP-103",
                        topic,
                        topicTitle,
                        "C. Named Entity Recognition (NER) & Information Extraction",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Implement Named Entity Recognition and relation extraction using spaCy and Transformer models.",
                        "NER system: extract entities (PERSON, ORG, GPE, DATE, MONEY) from news articles, visualize entity spans with displaCy, and extract entity-relation triples (Subject-Verb-Object).",
                        "Information extraction engine converting unstructured news text into structured entity graphs.",
                        "Project Difficulty: Medium | Tech: spaCy, NER, Information Extraction, displaCy",
                        List.of(
                                new TestCase("Process article text", "Extracts 'Google' (ORG), 'Sundar Pichai' (PERSON), '$5B' (MONEY)", true, "Entities extracted"),
                                new TestCase("Render displaCy visualization", "Generates HTML with color-coded entity highlight spans", false),
                                new TestCase("Extract Subject-Verb-Object triples", "Dependency parser extracts '(Sundar Pichai, announced, investment)'", false)
                        ),
                        "Use spaCy's Transformer-based model (en_core_web_trf) for highest NER precision."
                ),
                new DsaProblem(
                        "NP-104",
                        topic,
                        topicTitle,
                        "D. BERT Fine-Tuning for Multi-Class Text Classification",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Fine-tune a pre-trained BERT transformer model using HuggingFace Transformers and PyTorch.",
                        "Fine-tuning workflow: AutoTokenizer (bert-base-uncased) with padding/truncation, AutoModelForSequenceClassification, HuggingFace Trainer API, and evaluation metrics (accuracy, F1).",
                        "Fine-tuned transformer model achieving state-of-the-art multi-class text categorization.",
                        "Project Difficulty: Medium | Tech: HuggingFace Transformers, BERT, PyTorch, Fine-Tuning",
                        List.of(
                                new TestCase("Fine-tune BERT for 3 epochs", "Validation loss decreases; classification accuracy > 93%", true, "BERT fine-tuned"),
                                new TestCase("Classify new customer inquiry", "Accurately routes text to correct support category", false),
                                new TestCase("Inspect attention weights", "Self-attention maps highlight keywords driving classification", false)
                        ),
                        "Use a low learning rate (2e-5 to 5e-5) when fine-tuning transformer models to avoid catastrophic forgetting."
                ),
                new DsaProblem(
                        "NP-105",
                        topic,
                        topicTitle,
                        "E. Document Question-Answering with RAG (Retrieval-Augmented Generation)",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Architect a full Retrieval-Augmented Generation (RAG) system for document question-answering.",
                        "RAG pipeline: 1) Document loading & chunking with RecursiveCharacterTextSplitter, 2) Vector embeddings with sentence-transformers, 3) ChromaDB vector storage, 4) Top-k similarity retrieval with re-ranking, 5) LLM answer generation with source citations.",
                        "Enterprise RAG pipeline generating accurate grounded answers with exact source page citations.",
                        "Project Difficulty: Hard | Tech: LangChain, ChromaDB, Sentence-Transformers, RAG, LLM",
                        List.of(
                                new TestCase("Ask question about uploaded PDF", "Retrieves top 3 relevant chunks and synthesizes grounded answer", true, "Answer generated"),
                                new TestCase("Check answer citations", "Includes exact document page number and excerpt references", false),
                                new TestCase("Ask out-of-domain question", "System correctly responds: 'Information not found in documents'", false)
                        ),
                        "Include chunk overlap (e.g. 1000 chunk size with 200 overlap) to preserve context across boundaries."
                )
        );
    }

    // =========================================================================
    // AI & MACHINE LEARNING - Generative AI, LLMs & Prompt Engineering (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildGenAiProblems() {
        String topic = "genai";
        String topicTitle = "Generative AI, LLMs & Prompt Engineering";

        return List.of(
                new DsaProblem(
                        "GA-101",
                        topic,
                        topicTitle,
                        "A. Prompt Engineering & Few-Shot Learning Suite",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Design and benchmark advanced prompt engineering strategies for LLMs.",
                        "Framework comparing: Zero-Shot, Few-Shot In-Context Learning, Chain-of-Thought (CoT) reasoning, and Structured JSON output formatting. Measure token efficiency and task accuracy.",
                        "Comprehensive prompt engineering library producing deterministic structured LLM outputs.",
                        "Project Difficulty: Easy | Tech: Prompt Engineering, Few-Shot Learning, Chain-of-Thought",
                        List.of(
                                new TestCase("Run Few-Shot prompt template", "LLM strictly matches expected formatting and style guidelines", true, "Prompt verified"),
                                new TestCase("Execute Chain-of-Thought math prompt", "LLM details intermediate reasoning steps before final answer", false),
                                new TestCase("Request Pydantic JSON schema", "LLM outputs 100% valid parseable JSON conforming to schema", false)
                        ),
                        "Specify system role instructions and output schema constraints explicitly for deterministic results."
                ),
                new DsaProblem(
                        "GA-102",
                        topic,
                        topicTitle,
                        "B. Streaming AI Chatbot with Memory & Tool Calling",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Build an interactive AI chatbot featuring conversation memory and function calling.",
                        "Chat application utilizing OpenAI / Gemini API: streaming token output in real-time, ConversationBufferWindowMemory retaining past 5 turns, and tool calling for weather/time queries.",
                        "Responsive AI conversational agent with context retention and external tool execution.",
                        "Project Difficulty: Easy | Tech: OpenAI/Gemini API, Streaming, Function Calling, Memory",
                        List.of(
                                new TestCase("Ask 'What is the weather in Tokyo?'", "LLM triggers get_weather tool call and formats result in response", true, "Tool executed"),
                                new TestCase("Follow up with 'And what should I pack?'", "Chatbot remembers Tokyo context and provides clothing tips", false),
                                new TestCase("Inspect streaming response", "Tokens render incrementally with zero perceptible latency", false)
                        ),
                        "Always handle tool calling in a loop as models may execute multiple tool calls sequentially."
                ),
                new DsaProblem(
                        "GA-103",
                        topic,
                        topicTitle,
                        "C. Semantic Search & Vector Database with FAISS",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Implement high-speed semantic search over 100,000 articles using vector embeddings and FAISS.",
                        "Semantic search engine: generate dense 384-dimensional embeddings using all-MiniLM-L6-v2, construct FAISS IndexFlatIP (cosine similarity) and IndexHNSWFlat (approximate nearest neighbor), benchmark retrieval speed.",
                        "Ultra-fast vector search system retrieving semantically related documents in sub-10ms.",
                        "Project Difficulty: Medium | Tech: FAISS, Sentence-Transformers, Vector Search, HNSW",
                        List.of(
                                new TestCase("Query 'healthy breakfast recipes'", "Returns articles about oatmeal, smoothies, eggs with high cosine similarity", true, "Semantic match verified"),
                                new TestCase("Benchmark IndexHNSWFlat on 100k vectors", "Performs k=5 nearest neighbor retrieval in < 3ms", false),
                                new TestCase("Normalize embeddings before indexing", "L2-normalized vectors allow fast dot-product cosine similarity", false)
                        ),
                        "Normalize embedding vectors using faiss.normalize_L2 before adding to IndexFlatIP."
                ),
                new DsaProblem(
                        "GA-104",
                        topic,
                        topicTitle,
                        "D. Parameter-Efficient Fine-Tuning (PEFT / QLoRA)",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Fine-tune an open-weights LLM (Llama 3 / Mistral) using QLoRA and PEFT.",
                        "QLoRA pipeline: 4-bit quantization with bitsandbytes (NF4), LoRA adapter injection (rank=16, alpha=32 on target modules), SFTTrainer with custom instruction dataset, merge and save adapters.",
                        "Domain-adapted LLM fine-tuned on single GPU with 90% reduced memory footprint.",
                        "Project Difficulty: Medium | Tech: HuggingFace PEFT, QLoRA, bitsandbytes, SFTTrainer",
                        List.of(
                                new TestCase("Fine-tune 8B LLM on consumer GPU", "Runs within 12GB VRAM using 4-bit quantization and LoRA", true, "QLoRA fine-tuned"),
                                new TestCase("Evaluate on domain instructions", "Fine-tuned model follows domain tone and formatting accurately", false),
                                new TestCase("Merge LoRA adapters", "Successfully merges adapter weights back into base model", false)
                        ),
                        "Target all linear projection layers (q_proj, k_proj, v_proj, o_proj) for best LoRA performance."
                ),
                new DsaProblem(
                        "GA-105",
                        topic,
                        topicTitle,
                        "E. Autonomous Multi-Agent Workflow with LangGraph",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Construct an autonomous multi-agent software engineering system with LangGraph.",
                        "Multi-agent state graph: 1) Architect Agent (creates technical spec), 2) Coder Agent (writes code), 3) Tester Agent (executes tests in sandbox), 4) Reviewer Agent (validates spec). Conditional cyclic routing on failure.",
                        "Self-correcting multi-agent AI system executing complex multi-step development tasks.",
                        "Project Difficulty: Hard | Tech: LangGraph, Multi-Agent Systems, StateGraph, Code Execution",
                        List.of(
                                new TestCase("Provide prompt 'Build a REST API in Flask'", "Agents collaborate: spec -> code -> test -> review -> finished", true, "Agents completed"),
                                new TestCase("Tester Agent detects test failure", "State graph loops back to Coder Agent with error diagnostics", false),
                                new TestCase("Inspect execution trace", "StateGraph manages shared state and message history across turns", false)
                        ),
                        "Define strict State typed dictionaries in LangGraph to maintain clear contract between agent nodes."
                )
        );
    }

    // =========================================================================
    // AI & MACHINE LEARNING - MLOps, Docker & Cloud Model Deployment (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildMlopsProblems() {
        String topic = "mlops";
        String topicTitle = "MLOps, Docker & Cloud Model Deployment";

        return List.of(
                new DsaProblem(
                        "MO-101",
                        topic,
                        topicTitle,
                        "A. MLflow Experiment Tracking & Metric Logging",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Integrate MLflow experiment tracking into machine learning training scripts.",
                        "MLflow setup: start run, log hyperparameters (learning_rate, epochs), log training/validation metrics per epoch, log confusion matrix artifact PNG, and register model to MLflow Model Registry.",
                        "Standardized experiment tracking dashboard comparing metrics across training runs.",
                        "Project Difficulty: Easy | Tech: Python, MLflow, Experiment Tracking, Model Registry",
                        List.of(
                                new TestCase("Execute training script with MLflow", "Run appears in MLflow UI with all logged params and metrics", true, "Experiment logged"),
                                new TestCase("Compare 3 training runs", "MLflow UI visualizes comparative loss and accuracy curves", false),
                                new TestCase("Register best run to Model Registry", "Model versioned and tagged as 'Staging'", false)
                        ),
                        "Use mlflow.autolog() for automatic metric and artifact capture across major ML frameworks."
                ),
                new DsaProblem(
                        "MO-102",
                        topic,
                        topicTitle,
                        "B. FastAPI High-Performance Model Serving Endpoint",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Deploy a trained ML model behind a high-throughput async FastAPI REST service.",
                        "FastAPI service: Pydantic request/response schema validation, model loaded in lifespan startup context, POST /predict endpoint returning predictions with latency headers, and GET /health check.",
                        "Production-ready model inference API handling concurrent asynchronous prediction requests.",
                        "Project Difficulty: Easy | Tech: FastAPI, Pydantic, Model Serving, Uvicorn, REST",
                        List.of(
                                new TestCase("Send POST /predict with valid feature JSON", "Returns 200 with prediction and confidence score in < 15ms", true, "Inference served"),
                                new TestCase("Send POST /predict with missing field", "Pydantic returns 422 Unprocessable Entity with error details", false),
                                new TestCase("GET /health endpoint", "Returns status: 'healthy' and model version metadata", false)
                        ),
                        "Load ML models once during application lifespan startup, never inside the route handler."
                ),
                new DsaProblem(
                        "MO-103",
                        topic,
                        topicTitle,
                        "C. Containerized ML Service with Multi-Stage Docker",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Containerize an ML inference service with Docker, optimized dependencies, and non-root user.",
                        "Dockerfile: lightweight python:3.11-slim base, multi-stage build caching wheels, non-root user execution, model weights baked or mounted via volume, healthcheck instruction, and gunicorn/uvicorn workers.",
                        "Minimal, secure Docker container (< 250MB) ready for Kubernetes or cloud deployment.",
                        "Project Difficulty: Medium | Tech: Docker, Multi-Stage Builds, Container Security",
                        List.of(
                                new TestCase("docker build -t ml-service:v1 .", "Builds clean container image with cached dependency layer", true, "Container built"),
                                new TestCase("docker run -p 8000:8000 ml-service:v1", "Service starts and successfully serves predictions", false),
                                new TestCase("Inspect container security", "Process runs as non-root user; no dev build tools in final image", false)
                        ),
                        "Pin exact dependency versions in requirements.txt for reproducible container builds."
                ),
                new DsaProblem(
                        "MO-104",
                        topic,
                        topicTitle,
                        "D. Automated Data & Model Drift Monitoring with Evidently AI",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build an automated data drift and model performance monitoring service.",
                        "Monitoring pipeline using Evidently AI: compare production inference feature distributions against reference training dataset using Kolmogorov-Smirnov and Wasserstein tests. Generate HTML drift reports.",
                        "Continuous monitoring pipeline detecting covariate shift and triggering retraining alerts.",
                        "Project Difficulty: Medium | Tech: Evidently AI, Data Drift, Statistical Testing, Monitoring",
                        List.of(
                                new TestCase("Run drift analysis on production batch", "Detects drift in 2 input features (p-value < 0.05)", true, "Drift detected"),
                                new TestCase("Generate Evidently HTML report", "Visualizes distribution shifts with side-by-side histograms", false),
                                new TestCase("Automated drift alert trigger", "Fires webhook notification when drift exceeds 20% of features", false)
                        ),
                        "Set drift detection thresholds based on statistical significance (e.g. KS-test p < 0.05)."
                ),
                new DsaProblem(
                        "MO-105",
                        topic,
                        topicTitle,
                        "E. Kubernetes Inference Deployment with Auto-Scaling (KEDA)",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Deploy an auto-scaling ML inference microservice to Kubernetes using KEDA.",
                        "Kubernetes manifests: Deployment (requests/limits, readiness probe), ClusterIP Service, and KEDA ScaledObject scaling pod replicas (from 1 to 10) based on Prometheus request latency / queue length.",
                        "Elastic cloud-native inference architecture auto-scaling smoothly with traffic bursts.",
                        "Project Difficulty: Hard | Tech: Kubernetes, KEDA, Prometheus, Horizontal Pod Auto-scaling",
                        List.of(
                                new TestCase("Apply Kubernetes manifests", "Inference pods deploy and pass readiness probes", true, "Pods running"),
                                new TestCase("Simulate traffic burst (500 req/sec)", "KEDA scales deployment from 1 to 8 pods automatically", false),
                                new TestCase("Traffic subsides to zero", "Gracefully scales down to minimum replica count", false)
                        ),
                        "Configure readiness probes to verify the ML model is fully loaded before routing traffic to pod."
                )
        );
    }

    // =========================================================================
    // DATA SCIENCE & ANALYTICS (8 Topics x 5 Exercises)
    // =========================================================================

    // =========================================================================
    // DATA SCIENCE & ANALYTICS - Numerical Computing with NumPy & SciPy (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildNumpyProblems() {
        String topic = "numpy";
        String topicTitle = "Numerical Computing with NumPy & SciPy";

        return List.of(
                new DsaProblem(
                        "NM-101",
                        topic,
                        topicTitle,
                        "A. Array Broadcasting & Vectorized Math Mastery",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Master NumPy multidimensional array manipulation and broadcasting rules.",
                        "NumPy script demonstrating: multi-dimensional indexing, slicing, boolean masking, array reshaping/flattening, broadcasting 2D matrix with 1D vector, and vectorized math replacing slow Python loops.",
                        "High-performance numerical module executing vectorized array computations.",
                        "Project Difficulty: Easy | Tech: Python, NumPy, Broadcasting, Vectorization",
                        List.of(
                                new TestCase("Broadcast (3, 1) array with (1, 4) array", "Produces (3, 4) result matrix matching broadcasting rules", true, "Broadcasting verified"),
                                new TestCase("Boolean mask array[array > 50]", "Extracts matching subset without for-loops", false),
                                new TestCase("Benchmark loop vs vectorized sum", "NumPy vectorized operations execute > 50x faster", false)
                        ),
                        "Two dimensions are compatible for broadcasting if they are equal, or one of them is 1."
                ),
                new DsaProblem(
                        "NM-102",
                        topic,
                        topicTitle,
                        "B. Matrix Inversion, SVD & Linear Systems",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Solve systems of linear equations and compute Singular Value Decomposition (SVD).",
                        "NumPy module solving Ax = b using np.linalg.solve, computing matrix condition numbers, and performing SVD (U, S, Vt) for low-rank matrix approximation and image compression.",
                        "Accurate linear algebra solver with verified SVD rank approximation.",
                        "Project Difficulty: Easy | Tech: NumPy, SVD, Linear Systems, Matrix Factorization",
                        List.of(
                                new TestCase("Solve 3x3 linear system Ax = b", "Finds exact solution vector x satisfying Ax == b", true, "System solved"),
                                new TestCase("Perform SVD on 100x100 matrix", "Reconstructs matrix with top 10 singular values", false),
                                new TestCase("Calculate condition number", "Identifies ill-conditioned matrices vulnerable to numerical instability", false)
                        ),
                        "Use np.linalg.solve(A, b) instead of computing inv(A) @ b for faster and more accurate results."
                ),
                new DsaProblem(
                        "NM-103",
                        topic,
                        topicTitle,
                        "C. Digital Signal Processing & Filtering with SciPy",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Implement digital signal filtering and Fast Fourier Transform (FFT) analysis.",
                        "SciPy module: 1) Generate noisy multi-frequency signal, 2) Compute FFT frequency spectrum with np.fft.rfft, 3) Design Butterworth bandpass filter with scipy.signal.butter, 4) Filter with scipy.signal.filtfilt.",
                        "Digital signal processing pipeline isolating target frequencies from noisy time-series.",
                        "Project Difficulty: Medium | Tech: SciPy, FFT, Butterworth Filter, Signal Processing",
                        List.of(
                                new TestCase("Apply FFT to combined 50Hz + 120Hz signal", "FFT spectrum displays sharp magnitude peaks at 50Hz and 120Hz", true, "FFT verified"),
                                new TestCase("Apply Butterworth lowpass filter (cutoff 60Hz)", "Suppresses 120Hz noise while preserving 50Hz fundamental", false),
                                new TestCase("Zero-phase filtering check", "filtfilt performs forward-backward filtering with zero phase distortion", false)
                        ),
                        "Always use filtfilt instead of lfilter when zero phase shift is required in offline processing."
                ),
                new DsaProblem(
                        "NM-104",
                        topic,
                        topicTitle,
                        "D. Numerical Optimization & Curve Fitting with SciPy",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Solve non-linear optimization and experimental curve fitting problems.",
                        "SciPy module: 1) Find minimum of multivariate non-convex functions with scipy.optimize.minimize (BFGS / Nelder-Mead), 2) Fit non-linear exponential decay parameters using scipy.optimize.curve_fit.",
                        "Numerical optimization toolkit finding function extrema and fitting experimental data.",
                        "Project Difficulty: Medium | Tech: SciPy, scipy.optimize, Curve Fitting, BFGS",
                        List.of(
                                new TestCase("Minimize Rosenbrock function", "Finds global minimum at [1.0, 1.0] within 1e-4 tolerance", true, "Optimization verified"),
                                new TestCase("Fit exponential decay y = a * exp(-b * t) + c", "curve_fit recovers true parameters (a, b, c) from noisy data", false),
                                new TestCase("Inspect parameter covariance matrix", "Extracts standard errors for all estimated parameters", false)
                        ),
                        "Provide reasonable initial guesses (p0) to curve_fit to prevent convergence to local minima."
                ),
                new DsaProblem(
                        "NM-105",
                        topic,
                        topicTitle,
                        "E. High-Performance Monte Carlo Simulation Engine",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Build a vectorized Monte Carlo simulation engine for financial risk and probability estimation.",
                        "Simulation engine: simulate 1,000,000 stochastic price paths using Geometric Brownian Motion (GBM), calculate Value at Risk (VaR 95% / 99%), compute option pricing, and plot confidence intervals.",
                        "Vectorized Monte Carlo engine running 1 million path simulations in under 1 second.",
                        "Project Difficulty: Hard | Tech: NumPy, Monte Carlo, Vectorization, Geometric Brownian Motion",
                        List.of(
                                new TestCase("Simulate 1,000,000 asset paths", "Vectorized simulation completes in < 800ms", true, "Simulation complete"),
                                new TestCase("Calculate 99% 1-day Value at Risk (VaR)", "Computes 1st percentile of terminal return distribution", false),
                                new TestCase("Price European Call Option", "Monte Carlo price matches Black-Scholes formula within 0.5%", false)
                        ),
                        "Avoid nested loops by generating full (n_steps, n_paths) random normal matrices in one NumPy call."
                )
        );
    }

    // =========================================================================
    // DATA SCIENCE & ANALYTICS - Data Wrangling & Analysis with Pandas (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildPandasProblems() {
        String topic = "pandas";
        String topicTitle = "Data Wrangling & Analysis with Pandas";

        return List.of(
                new DsaProblem(
                        "PD-101",
                        topic,
                        topicTitle,
                        "A. Data Ingestion, Cleaning & Type Casting",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Clean and standardize a raw messy multi-format dataset using Pandas.",
                        "Pandas pipeline: read CSV/JSON with custom parse_dates, handle missing values (fillna, interpolate, dropna), strip whitespace from strings, cast numeric types, and validate schema integrity.",
                        "Clean, typed DataFrame ready for exploratory analysis with zero missing value anomalies.",
                        "Project Difficulty: Easy | Tech: Python, Pandas, Data Cleaning, Datetime Parsing",
                        List.of(
                                new TestCase("Load messy raw dataset", "Correctly parses mixed date formats and currency strings ($1,234.50)", true, "Data loaded and cleaned"),
                                new TestCase("Handle missing values", "Imputes missing numerical values with median grouped by category", false),
                                new TestCase("Check df.info()", "All columns have proper dtypes (float64, int64, datetime64[ns])", false)
                        ),
                        "Use pd.to_datetime(errors='coerce') to safely handle corrupted date entries."
                ),
                new DsaProblem(
                        "PD-102",
                        topic,
                        topicTitle,
                        "B. GroupBy Aggregations & Pivot Tables",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Perform multi-level grouping, transformations, and pivot tables in Pandas.",
                        "Pandas operations: multi-column groupby with named aggregations (.agg(mean_val=('val', 'mean'), count_val=('val', 'count'))), group-level transform for normalization, and pivot_table with margins.",
                        "Aggregated summary tables and group-transformed metrics answering business questions.",
                        "Project Difficulty: Easy | Tech: Pandas, GroupBy, Pivot Tables, Window Transforms",
                        List.of(
                                new TestCase("Execute multi-column GroupBy", "Computes grouped sales totals and averages across region & year", true, "Aggregation complete"),
                                new TestCase("Calculate group percentage with transform", "Computes each transaction percentage of its category total", false),
                                new TestCase("Generate Pivot Table with margins=True", "Produces cross-tabulation table with row and column totals", false)
                        ),
                        "Named aggregation syntax .agg(col_name=('source_col', 'agg_func')) provides clean column names."
                ),
                new DsaProblem(
                        "PD-103",
                        topic,
                        topicTitle,
                        "C. Relational Data Merging & Reconciliation",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Merge, join, and reconcile multi-table relational data using Pandas.",
                        "Operations: pd.merge (inner, left, right, outer) with indicator=True, merge_asof for nearest-timestamp trade execution matching, and data reconciliation audit identifying unmatched records.",
                        "Reconciled relational dataset with comprehensive audit trail of merged records.",
                        "Project Difficulty: Medium | Tech: Pandas, pd.merge, merge_asof, Data Reconciliation",
                        List.of(
                                new TestCase("Execute merge with indicator=True", "Identifies records present in 'both', 'left_only', and 'right_only'", true, "Merge verified"),
                                new TestCase("Execute merge_asof on timestamped quotes", "Matches trades to most recent preceding quote within 50ms tolerance", false),
                                new TestCase("Reconciliation summary", "Outputs report of orphan foreign keys and mismatched quantities", false)
                        ),
                        "Ensure data is sorted by timestamp before executing pd.merge_asof."
                ),
                new DsaProblem(
                        "PD-104",
                        topic,
                        topicTitle,
                        "D. Time-Series Resampling & Rolling Windows",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Analyze financial time-series using Pandas DateTimeIndex, resampling, and rolling windows.",
                        "Time-series module: set DateTimeIndex, resample from minute to hourly OHLCV bars (Open, High, Low, Close, Volume), compute 20-day Exponential Moving Average (EMA), and calculate rolling volatility.",
                        "Clean financial time-series pipeline with multi-timeframe resampling and technical indicators.",
                        "Project Difficulty: Medium | Tech: Pandas, Time Series, Resampling, Rolling Statistics",
                        List.of(
                                new TestCase("Resample 1-minute data to 1-hour bars", "Generates accurate OHLCV bars with custom aggregation dictionary", true, "Resampling verified"),
                                new TestCase("Compute 20-period rolling standard deviation", "Calculates rolling volatility without lookahead bias", false),
                                new TestCase("Handle holiday gaps", "Applies forward fill (.ffill()) across market closure gaps", false)
                        ),
                        "Use .shift(1) when calculating rolling indicators to prevent lookahead data leakage."
                ),
                new DsaProblem(
                        "PD-105",
                        topic,
                        topicTitle,
                        "E. High-Performance Large Dataset Processing with Polars & Parquet",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Optimize large-scale DataFrame processing (10M+ rows) using Parquet and Polars.",
                        "Data processing pipeline: 1) Convert 5GB CSV to snappy-compressed Parquet, 2) Benchmark filtering and groupby using Pandas vs Polars with LazyFrame execution, 3) Chunked processing with memory profiling.",
                        "Ultra-fast DataFrame pipeline executing queries 10x faster with 70% reduced memory footprint.",
                        "Project Difficulty: Hard | Tech: Pandas, Polars, Apache Parquet, Lazy Evaluation",
                        List.of(
                                new TestCase("Convert CSV to Parquet", "File size shrinks from 5GB to 800MB with column metadata", true, "Parquet converted"),
                                new TestCase("Execute Polars LazyFrame query", "Completes complex groupby query on 10M rows in < 1.5s", false),
                                new TestCase("Memory profiling comparison", "Polars consumes 80% less peak RAM than eager Pandas execution", false)
                        ),
                        "Use category / enum dtypes for low-cardinality string columns to drastically cut memory usage."
                )
        );
    }

    // =========================================================================
    // DATA SCIENCE & ANALYTICS - Exploratory Data Analysis & Visualization (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildEdaProblems() {
        String topic = "eda";
        String topicTitle = "Exploratory Data Analysis & Visualization";

        return List.of(
                new DsaProblem(
                        "ED-101",
                        topic,
                        topicTitle,
                        "A. Statistical Visualizations with Matplotlib & Seaborn",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Create a multi-panel exploratory data visualization dashboard using Matplotlib and Seaborn.",
                        "Visual dashboard (2x2 grid): 1) Histogram with KDE distribution plot, 2) Boxplot comparing distributions across categories with outlier points, 3) Scatter plot with hue and size encodings, 4) Annotated correlation heatmap.",
                        "Polished publication-ready statistical figures visualizing data distributions.",
                        "Project Difficulty: Easy | Tech: Python, Matplotlib, Seaborn, Statistical Plots",
                        List.of(
                                new TestCase("Generate 2x2 subplot figure", "Renders distribution, boxplot, scatter, and heatmap cleanly", true, "Visualizations generated"),
                                new TestCase("Inspect correlation heatmap", "Displays annotated correlation coefficients with custom diverging palette", false),
                                new TestCase("Save high-res figure", "plt.savefig('eda.png', dpi=300, bbox_inches='tight') saves crisp image", false)
                        ),
                        "Set sns.set_theme(style='whitegrid') at the start for clean aesthetic chart defaults."
                ),
                new DsaProblem(
                        "ED-102",
                        topic,
                        topicTitle,
                        "B. Interactive Multi-Chart Dashboard with Plotly Express",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Build an interactive web-based exploratory visualization dashboard using Plotly.",
                        "Interactive dashboard: dynamic scatter plot with hover tooltips, multi-series line chart with range slider, animated bubble chart over time, and choropleth map. Export as standalone interactive HTML.",
                        "Interactive Plotly dashboard enabling zoom, pan, hover tooltips, and category filtering.",
                        "Project Difficulty: Easy | Tech: Plotly Express, Interactive Visualization, HTML Export",
                        List.of(
                                new TestCase("Open Plotly HTML in browser", "Interactive charts render with responsive hover data cards", true, "Interactive dashboard active"),
                                new TestCase("Use time slider animation", "Bubbles animate across years showing metric progression", false),
                                new TestCase("Click legend item", "Toggles category visibility dynamically without server reload", false)
                        ),
                        "Use plotly.express for rapid charting and plotly.graph_objects for complex custom layouts."
                ),
                new DsaProblem(
                        "ED-103",
                        topic,
                        topicTitle,
                        "C. Outlier Detection: IQR, Z-Score & Isolation Forest",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Implement and compare statistical and machine learning outlier detection methods.",
                        "Outlier pipeline: 1) Tukey's Interquartile Range (IQR) method (1.5 * IQR bounds), 2) Z-score method (|z| > 3), 3) Scikit-learn Isolation Forest algorithm. Visualize detected outliers on scatter plots.",
                        "Comprehensive outlier analysis module flagging univariate and multivariate anomalies.",
                        "Project Difficulty: Medium | Tech: Outlier Detection, IQR, Z-Score, Isolation Forest",
                        List.of(
                                new TestCase("Execute IQR detection", "Flags values outside [Q1 - 1.5*IQR, Q3 + 1.5*IQR] as outliers", true, "Outliers detected"),
                                new TestCase("Execute Isolation Forest", "Identifies non-linear multivariate anomaly clusters", false),
                                new TestCase("Compare method agreement", "Generates Venn diagram of anomalies identified across all 3 methods", false)
                        ),
                        "Use robust statistics (median, IQR) rather than mean/std when data contains extreme outliers."
                ),
                new DsaProblem(
                        "ED-104",
                        topic,
                        topicTitle,
                        "D. Automated Exploratory Data Analysis & Quality Reporting",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build an automated EDA and data profiling report generator in Python.",
                        "Automated reporting script: calculate summary stats, detect missing value patterns with missingno, assess skewness/kurtosis, compute VIF multicollinearity, and generate an automated HTML data audit summary.",
                        "Automated data quality report diagnosing anomalies, skewness, and correlations in seconds.",
                        "Project Difficulty: Medium | Tech: Python, missingno, Automated EDA, Data Profiling",
                        List.of(
                                new TestCase("Run profiler on new dataset", "Generates comprehensive HTML summary report automatically", true, "Report generated"),
                                new TestCase("Inspect missingno matrix", "Visualizes correlation patterns between missing fields", false),
                                new TestCase("Check multicollinearity table", "Flags features with Variance Inflation Factor (VIF) > 10", false)
                        ),
                        "Examine missing data patterns to determine if values are MCAR, MAR, or MNAR."
                ),
                new DsaProblem(
                        "ED-105",
                        topic,
                        topicTitle,
                        "E. Dimensionality Reduction Visualization: t-SNE & UMAP",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Visualize high-dimensional data clusters using t-SNE and UMAP algorithms.",
                        "High-dimensional visualization workflow: 1) Preprocess high-dimensional features (100+ dims), 2) Reduce dimensionality using PCA, t-SNE (tuning perplexity), and UMAP (tuning n_neighbors, min_dist), 3) Plot 2D cluster projections.",
                        "2D manifold projection revealing intricate non-linear cluster structures in complex data.",
                        "Project Difficulty: Hard | Tech: UMAP, t-SNE, Manifold Learning, High-Dim Visualization",
                        List.of(
                                new TestCase("Run UMAP on high-dimensional dataset", "Projects to 2D in < 5 seconds preserving local and global structure", true, "UMAP projection verified"),
                                new TestCase("Compare t-SNE vs UMAP projections", "Both clearly separate distinct class clusters in 2D space", false),
                                new TestCase("Tune perplexity parameter in t-SNE", "Analyzes how perplexity affects cluster density and separation", false)
                        ),
                        "Always apply PCA first to reduce to ~50 dimensions before running t-SNE for faster convergence."
                )
        );
    }

    // =========================================================================
    // DATA SCIENCE & ANALYTICS - Applied Statistics & Hypothesis Testing (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildStatisticsProblems() {
        String topic = "statistics";
        String topicTitle = "Applied Statistics & Hypothesis Testing";

        return List.of(
                new DsaProblem(
                        "ST-101",
                        topic,
                        topicTitle,
                        "A. Probability Distributions & Central Limit Theorem",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Simulate probability distributions and demonstrate the Central Limit Theorem (CLT).",
                        "Simulation script: sample from Normal, Binomial, Poisson, and Exponential distributions using scipy.stats. Compute PDF, CDF, PPF, and demonstrate sampling distribution of means converges to Normal.",
                        "Statistical module verifying theoretical distribution properties and CLT convergence.",
                        "Project Difficulty: Easy | Tech: Python, scipy.stats, Probability Distributions, CLT",
                        List.of(
                                new TestCase("Sample 1,000 means from non-normal Exponential distribution", "Histogram of sample means forms perfect Gaussian bell curve", true, "CLT demonstrated"),
                                new TestCase("Calculate CDF value P(X <= 2) for Poisson(3)", "Computes exact theoretical cumulative probability", false),
                                new TestCase("Generate Q-Q Plot", "Points align along reference line verifying normality", false)
                        ),
                        "The Central Limit Theorem holds regardless of the underlying distribution if sample size n >= 30."
                ),
                new DsaProblem(
                        "ST-102",
                        topic,
                        topicTitle,
                        "B. Parametric Hypothesis Testing: t-Tests & ANOVA",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Conduct parametric hypothesis tests and calculate effect sizes (Cohen's d).",
                        "Hypothesis testing suite: 1) One-sample t-test, 2) Two-sample independent t-test (Welch's t-test), 3) Paired samples t-test, 4) One-way ANOVA (scipy.stats.f_oneway) with Tukey HSD post-hoc test.",
                        "Rigorous hypothesis testing module returning test statistics, p-values, and effect sizes.",
                        "Project Difficulty: Easy | Tech: scipy.stats, Hypothesis Testing, t-Test, ANOVA, Effect Size",
                        List.of(
                                new TestCase("Execute Welch's two-sample t-test", "Reports t-statistic, two-tailed p-value, and Cohen's d effect size", true, "t-Test complete"),
                                new TestCase("Evaluate p-value < alpha (0.05)", "Correctly rejects null hypothesis when significant difference exists", false),
                                new TestCase("Execute One-way ANOVA across 4 groups", "f_oneway detects significant variance; Tukey HSD isolates pairs", false)
                        ),
                        "Always report effect sizes (Cohen's d or eta-squared) alongside p-values for practical significance."
                ),
                new DsaProblem(
                        "ST-103",
                        topic,
                        topicTitle,
                        "C. A/B Testing Engine with Power Analysis & Sample Size",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Design and analyze an online A/B conversion experiment with statistical power analysis.",
                        "A/B test workflow: 1) Calculate minimum sample size using statsmodels power analysis (alpha=0.05, power=0.80, MDE=2%), 2) Two-proportion z-test on conversion rates, 3) Confidence intervals for relative lift.",
                        "Production A/B testing statistical calculator ensuring experimental rigor and valid conclusions.",
                        "Project Difficulty: Medium | Tech: statsmodels, A/B Testing, Power Analysis, Proportion Test",
                        List.of(
                                new TestCase("Calculate required sample size for 5% to 6% lift", "Determines required sample size of ~15,000 users per variant", true, "Sample size calculated"),
                                new TestCase("Execute two-proportion z-test on results", "Calculates z-score, p-value, and 95% confidence interval for lift", false),
                                new TestCase("Check sample ratio mismatch (SRM)", "Chi-square goodness-of-fit test confirms 50/50 traffic split integrity", false)
                        ),
                        "Never stop an A/B test early based on interim p-values without sequential testing corrections."
                ),
                new DsaProblem(
                        "ST-104",
                        topic,
                        topicTitle,
                        "D. Non-Parametric Hypothesis Tests: Chi-Square & Mann-Whitney",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Perform non-parametric hypothesis tests for non-normal and categorical distributions.",
                        "Non-parametric suite: 1) Chi-Square Test of Independence for contingency tables, 2) Mann-Whitney U Test (Wilcoxon rank-sum), 3) Kruskal-Wallis Test for multi-group comparisons.",
                        "Robust non-parametric testing pipeline for skewed, ranked, or categorical data.",
                        "Project Difficulty: Medium | Tech: scipy.stats, Chi-Square Test, Mann-Whitney U, Non-Parametric",
                        List.of(
                                new TestCase("Run Chi-Square test on 2x2 table", "Calculates chi2 statistic, p-value, and expected frequencies table", true, "Chi-Square verified"),
                                new TestCase("Execute Mann-Whitney U test on skewed data", "Compares median distributions without assuming normality", false),
                                new TestCase("Check Chi-Square assumptions", "Verifies expected frequency >= 5 in all contingency table cells", false)
                        ),
                        "Use non-parametric tests when data violates the normality assumption or contains severe outliers."
                ),
                new DsaProblem(
                        "ST-105",
                        topic,
                        topicTitle,
                        "E. Bayesian A/B Testing with PyMC & Beta-Binomial Conjugacy",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Build a Bayesian A/B testing engine using Beta-Binomial conjugate priors and MCMC.",
                        "Bayesian engine: 1) Analytical Beta-Binomial posterior updates for conversion rates, 2) Compute Probability of Being Best P(B > A), Expected Loss, and Credible Intervals, 3) PyMC MCMC sampling for complex metrics.",
                        "Bayesian experimental platform providing intuitive probabilities and decision thresholds.",
                        "Project Difficulty: Hard | Tech: Bayesian Statistics, PyMC, Beta Distribution, MCMC",
                        List.of(
                                new TestCase("Update Beta priors with A/B trial data", "Calculates exact posterior distributions Beta(alpha, beta)", true, "Posterior calculated"),
                                new TestCase("Compute P(B > A)", "Determines variant B has 97.4% probability of outperforming variant A", false),
                                new TestCase("Calculate 95% Highest Density Interval (HDI)", "Extracts Bayesian credible interval for absolute lift", false)
                        ),
                        "Bayesian A/B testing allows continuous monitoring without inflating false positive rates."
                )
        );
    }

    // =========================================================================
    // DATA SCIENCE & ANALYTICS - Feature Engineering & Dimensionality Reduction (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildFeatureEngProblems() {
        String topic = "feature_eng";
        String topicTitle = "Feature Engineering & Dimensionality Reduction";

        return List.of(
                new DsaProblem(
                        "FE-101",
                        topic,
                        topicTitle,
                        "A. Feature Scaling & Mathematical Transformations",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Implement and compare numerical feature scaling and normalization techniques.",
                        "Transformer suite: 1) StandardScaler (z-score), 2) MinMaxScaler (0-1 range), 3) RobustScaler (median and IQR), 4) Log1p and Box-Cox / Yeo-Johnson power transformations for skewed distributions.",
                        "Feature normalization module transforming skewed distributions into Gaussian-like shapes.",
                        "Project Difficulty: Easy | Tech: scikit-learn, PowerTransformer, Feature Scaling",
                        List.of(
                                new TestCase("Apply RobustScaler to data with extreme outliers", "Scales features robustly without outlier distortion", true, "Scaling verified"),
                                new TestCase("Apply Yeo-Johnson PowerTransformer", "Reduces skewness from +3.5 to < 0.2", false),
                                new TestCase("Inverse transform scaled predictions", "Reconstructs original data scale accurately", false)
                        ),
                        "Use Yeo-Johnson over Box-Cox when features contain zero or negative values."
                ),
                new DsaProblem(
                        "FE-102",
                        topic,
                        topicTitle,
                        "B. Categorical Encoding Strategies",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Implement and evaluate diverse categorical feature encoding strategies.",
                        "Encoding suite: 1) One-Hot Encoding with infrequent category pooling, 2) Ordinal Encoding for ranked variables, 3) Target / Mean Encoding with smoothing regularization, 4) Frequency / Count Encoding.",
                        "Comprehensive categorical encoding pipeline preventing high-cardinality dimensionality explosion.",
                        "Project Difficulty: Easy | Tech: scikit-learn, category_encoders, Target Encoding",
                        List.of(
                                new TestCase("Target encode high-cardinality feature (100 categories)", "Encodes to single informative column with m-estimate smoothing", true, "Target encoded"),
                                new TestCase("One-hot encode with max_categories=5", "Keeps top 5 categories and groups remaining in 'other'", false),
                                new TestCase("Evaluate out-of-fold target encoding", "Prevents target leakage during training", false)
                        ),
                        "Always use smoothing or out-of-fold calculation with target encoding to avoid overfitting."
                ),
                new DsaProblem(
                        "FE-103",
                        topic,
                        topicTitle,
                        "C. Domain-Specific Feature Extraction: Datetime & Geospatial",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Engineer informative domain features from timestamps and geographic coordinates.",
                        "Feature extractor: 1) Datetime features (dayofweek, hour, is_weekend, cyclical sine/cosine encodings), 2) Geospatial features (Haversine distance to city center, Manhattan distance, spatial clustering).",
                        "Rich domain feature generator expanding raw timestamps and coordinates into predictive signals.",
                        "Project Difficulty: Medium | Tech: Feature Engineering, Cyclical Encodings, Haversine Distance",
                        List.of(
                                new TestCase("Apply cyclical sin/cos transform to hour (0-23)", "Encodes 23:00 and 00:00 as adjacent points in 2D space", true, "Cyclical features created"),
                                new TestCase("Compute Haversine distance between coordinates", "Calculates accurate great-circle distance in kilometers", false),
                                new TestCase("Extract holiday flags", "Uses Python holidays package to flag national holidays", false)
                        ),
                        "Transform cyclical time features with sin(2*pi*x/T) and cos(2*pi*x/T) to preserve continuity across boundaries."
                ),
                new DsaProblem(
                        "FE-104",
                        topic,
                        topicTitle,
                        "D. Automated Feature Generation with Featuretools",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build an automated feature engineering pipeline using Deep Feature Synthesis (DFS).",
                        "Featuretools pipeline: create EntitySet with relational tables (Customers, Orders, OrderItems), define 1:M relationships, run Deep Feature Synthesis to automatically engineer 100+ aggregated features.",
                        "Automated relational feature engineering engine generating multi-hop aggregated features.",
                        "Project Difficulty: Medium | Tech: Featuretools, Deep Feature Synthesis, Relational Data",
                        List.of(
                                new TestCase("Run ft.dfs on relational EntitySet", "Automatically generates 120+ aggregated statistical features", true, "DFS executed"),
                                new TestCase("Inspect generated feature definitions", "Creates multi-hop aggregations (e.g. Mean order amount per customer)", false),
                                new TestCase("Evaluate feature importance", "Selected automated features boost model AUC by 4%", false)
                        ),
                        "Define cutoff times in Featuretools to prevent lookahead data leakage in time-dependent datasets."
                ),
                new DsaProblem(
                        "FE-105",
                        topic,
                        topicTitle,
                        "E. Advanced Feature Selection with Boruta & SHAP",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Implement advanced all-relevant feature selection using Boruta and SHAP values.",
                        "Feature selection system: 1) Boruta algorithm generating shadow features to test true significance against random noise, 2) SHAP (SHapley Additive exPlanations) tree explainer computing global and local importance.",
                        "State-of-the-art feature selection pipeline eliminating noise with mathematical rigor.",
                        "Project Difficulty: Hard | Tech: Boruta, SHAP, TreeExplainer, Feature Selection",
                        List.of(
                                new TestCase("Execute Boruta feature selection", "Separates confirmed predictive features from rejected noise", true, "Boruta completed"),
                                new TestCase("Compute SHAP values on test set", "SHAP summary plot reveals non-linear feature impacts and directions", false),
                                new TestCase("Generate SHAP waterfall plot for individual prediction", "Explains exact feature contributions driving single prediction", false)
                        ),
                        "Boruta finds all relevant features, unlike step-wise selection which finds only a minimal subset."
                )
        );
    }

    // =========================================================================
    // DATA SCIENCE & ANALYTICS - Big Data Analytics with Apache Spark & PySpark (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildBigDataProblems() {
        String topic = "bigdata";
        String topicTitle = "Big Data Analytics with Apache Spark & PySpark";

        return List.of(
                new DsaProblem(
                        "BD-101",
                        topic,
                        topicTitle,
                        "A. PySpark DataFrame Fundamentals & Transformations",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Initialize a SparkSession and execute distributed transformations with PySpark.",
                        "PySpark script: create SparkSession, read CSV/Parquet with explicit StructType schema, perform select, filter, withColumn, and groupBy aggregations. Inspect execution plan with df.explain().",
                        "Distributed data processing script executing transformations on Spark DataFrames.",
                        "Project Difficulty: Easy | Tech: PySpark, SparkSession, DataFrame API, Catalyst Optimizer",
                        List.of(
                                new TestCase("Load CSV with defined StructType schema", "Creates distributed DataFrame with correct column dtypes", true, "PySpark initialized"),
                                new TestCase("Filter and aggregate dataset", "Executes lazy transformations and computes aggregated metrics", false),
                                new TestCase("Inspect df.explain(extended=True)", "Visualizes Catalyst logical and physical execution plans", false)
                        ),
                        "Always provide an explicit StructType schema instead of inferSchema=True for faster ingestion."
                ),
                new DsaProblem(
                        "BD-102",
                        topic,
                        topicTitle,
                        "B. Distributed Log Processing & Window Functions",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Process large-scale web server access logs using PySpark and window functions.",
                        "PySpark pipeline: parse web access logs with regular expressions (regexp_extract), filter HTTP error codes, and compute window statistics using Window.partitionBy('user_id').orderBy('timestamp').",
                        "Scalable log analytics pipeline computing running session totals and lead/lag intervals.",
                        "Project Difficulty: Easy | Tech: PySpark, Window Functions, Regex, Log Analytics",
                        List.of(
                                new TestCase("Parse 1M log lines with regex", "Extracts IP, timestamp, method, path, status into typed columns", true, "Logs parsed"),
                                new TestCase("Execute Window function query", "Calculates cumulative request count and duration between user visits", false),
                                new TestCase("Identify top 10 requesting IPs", "Aggregates and sorts traffic origins with distributed execution", false)
                        ),
                        "Window functions without partitionBy force all data to a single partition; always partition when possible."
                ),
                new DsaProblem(
                        "BD-103",
                        topic,
                        topicTitle,
                        "C. Distributed Machine Learning Pipeline with PySpark MLlib",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build and tune a distributed ML pipeline using PySpark MLlib.",
                        "MLlib pipeline: StringIndexer, OneHotEncoder, VectorAssembler, LogisticRegression / GBTClassifier, and CrossValidator with ParamGridBuilder for distributed hyperparameter tuning.",
                        "Production distributed ML pipeline capable of training on terabyte-scale datasets.",
                        "Project Difficulty: Medium | Tech: PySpark MLlib, ML Pipeline, CrossValidator, VectorAssembler",
                        List.of(
                                new TestCase("Fit PySpark ML Pipeline", "Chains feature transformations and model training in single pipeline", true, "MLlib pipeline trained"),
                                new TestCase("Execute distributed CrossValidator", "Evaluates parameter grid concurrently across Spark cluster workers", false),
                                new TestCase("Evaluate with BinaryClassificationEvaluator", "Computes distributed ROC-AUC metric on test DataFrame", false)
                        ),
                        "Cache intermediate DataFrames with df.cache() before running iterative ML training loops."
                ),
                new DsaProblem(
                        "BD-104",
                        topic,
                        topicTitle,
                        "D. Spark Structured Streaming with Sliding Windows",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build a real-time event streaming analytics pipeline using Spark Structured Streaming.",
                        "Streaming application: readStream from socket/Kafka, parse JSON event stream, compute 5-minute sliding window aggregations with 1-minute slide, define 2-minute watermark, and writeStream to console/memory.",
                        "Low-latency streaming pipeline processing continuous event streams with late-data handling.",
                        "Project Difficulty: Medium | Tech: PySpark, Structured Streaming, Watermarking, Sliding Windows",
                        List.of(
                                new TestCase("Ingest streaming event data", "Processes micro-batches incrementally with low latency", true, "Streaming active"),
                                new TestCase("Evaluate sliding window aggregation", "Computes event counts per 5-minute window with 1-minute slide", false),
                                new TestCase("Simulate 3-minute late event", "Watermark drops data exceeding 2-minute lateness threshold", false)
                        ),
                        "Watermarking is essential in Structured Streaming to bound the state store memory size."
                ),
                new DsaProblem(
                        "BD-105",
                        topic,
                        topicTitle,
                        "E. Lakehouse Architecture with Delta Lake & ACID Transactions",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Implement a Bronze-Silver-Gold lakehouse architecture using Delta Lake and PySpark.",
                        "Delta Lake pipeline: 1) Bronze (raw append ingestion), 2) Silver (deduplicated clean data with Delta MERGE upserts), 3) Gold (aggregated business metrics with Z-ORDER optimization), 4) Time-travel audit queries.",
                        "Enterprise ACID-compliant Lakehouse pipeline supporting upserts, deletes, and time-travel.",
                        "Project Difficulty: Hard | Tech: Delta Lake, Apache Spark, Lakehouse, ACID, Time Travel",
                        List.of(
                                new TestCase("Execute Delta MERGE (upsert) operation", "Updates existing records and inserts new rows atomically", true, "Delta MERGE verified"),
                                new TestCase("Query historical snapshot via time-travel", "Reads table as of version 1 or timestamp accurately", false),
                                new TestCase("Run OPTIMIZE delta_table ZORDER BY (user_id)", "Compacts small files and co-locates data for fast query pruning", false)
                        ),
                        "Delta Lake ACID transactions eliminate partial write corruption during cluster failures."
                )
        );
    }

    // =========================================================================
    // DATA SCIENCE & ANALYTICS - Advanced SQL for Analytics & Data Warehouses (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildSqlAnalyticsProblems() {
        String topic = "sql_analytics";
        String topicTitle = "Advanced SQL for Analytics & Data Warehouses";

        return List.of(
                new DsaProblem(
                        "SA-101",
                        topic,
                        topicTitle,
                        "A. Advanced Window Functions: Ranking & Offsets",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Master analytical SQL window functions for ranking, offsets, and running totals.",
                        "SQL queries using: ROW_NUMBER(), RANK(), DENSE_RANK() partition comparisons, LAG() and LEAD() for period-over-period differences, and SUM() OVER (ORDER BY date ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW).",
                        "Advanced SQL analytical queries computing rolling calculations without self-joins.",
                        "Project Difficulty: Easy | Tech: PostgreSQL, SQL Window Functions, Ranking",
                        List.of(
                                new TestCase("Execute RANK() vs DENSE_RANK() query", "Handles ties with gap (RANK) and without gap (DENSE_RANK)", true, "Ranking verified"),
                                new TestCase("Calculate month-over-month revenue change", "LAG(revenue, 1) computes previous month delta accurately", false),
                                new TestCase("Calculate cumulative running total", "Window SUM accumulates revenue in chronological sequence", false)
                        ),
                        "Differentiate between ROWS and RANGE in window frames when handling duplicate timestamps."
                ),
                new DsaProblem(
                        "SA-102",
                        topic,
                        topicTitle,
                        "B. Recursive Common Table Expressions (CTEs)",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Implement recursive Common Table Expressions for hierarchical and graph data traversal.",
                        "Recursive SQL: 1) Traverse employee-manager organizational hierarchy with depth levels, 2) Generate date series table on the fly without calendar tables, 3) Find all connected flight route hops.",
                        "Recursive SQL query traversing arbitrary-depth hierarchical trees efficiently.",
                        "Project Difficulty: Easy | Tech: SQL, Recursive CTE, Hierarchical Queries, PostgreSQL",
                        List.of(
                                new TestCase("Execute organizational hierarchy CTE", "Traverses CEO down to junior staff with indentation depth level", true, "Hierarchy traversed"),
                                new TestCase("Generate 365-day calendar series", "WITH RECURSIVE dates AS (...) generates full year date sequence", false),
                                new TestCase("Prevent infinite loops", "Termination condition (WHERE level < 10) prevents unbounded recursion", false)
                        ),
                        "Ensure the recursive step contains a valid termination condition to prevent infinite loops."
                ),
                new DsaProblem(
                        "SA-103",
                        topic,
                        topicTitle,
                        "C. Dimensional Modeling: Star & Snowflake Schema",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Design and query a Star Schema dimensional data warehouse.",
                        "Schema design in PostgreSQL: Fact table (fact_sales with grain=order_item), Dimension tables (dim_customer, dim_product, dim_date, dim_store). Write queries for drill-down and roll-up across dimensions.",
                        "Normalized dimensional model optimized for analytical business intelligence queries.",
                        "Project Difficulty: Medium | Tech: Data Warehousing, Star Schema, Dimensional Modeling",
                        List.of(
                                new TestCase("Execute monthly sales roll-up by region", "Joins fact_sales with dim_date and dim_store with low query cost", true, "Roll-up verified"),
                                new TestCase("Drill down from category to product level", "Navigates dimension hierarchy seamlessly", false),
                                new TestCase("Inspect surrogate keys", "Dimension tables use integer surrogate keys (date_key=20260916)", false)
                        ),
                        "Use integer surrogate keys (YYYYMMDD) for date dimensions rather than native timestamp types."
                ),
                new DsaProblem(
                        "SA-104",
                        topic,
                        topicTitle,
                        "D. Analytics Engineering with dbt (data build tool)",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build a modular analytics transformation pipeline using dbt.",
                        "dbt project: 1) Staging models (stg_orders with renaming & casting), 2) Intermediate models with joins, 3) Mart dimension & fact models, 4) Generic tests (unique, not_null, accepted_values), 5) dbt documentation.",
                        "Modular, tested, and documented analytics engineering pipeline with lineage graph.",
                        "Project Difficulty: Medium | Tech: dbt Core, SQL Modeling, Data Testing, Jinja",
                        List.of(
                                new TestCase("Run 'dbt build'", "Compiles and executes all models and tests in DAG order", true, "dbt models built"),
                                new TestCase("Execute 'dbt test'", "Passes all unique, not_null, and relationship integrity tests", false),
                                new TestCase("Generate dbt docs", "Produces interactive documentation and visual model lineage DAG", false)
                        ),
                        "Use the {{ ref('model_name') }} function in dbt to automatically construct the dependency DAG."
                ),
                new DsaProblem(
                        "SA-105",
                        topic,
                        topicTitle,
                        "E. Cloud Data Warehouse Optimization (BigQuery / Snowflake)",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Optimize analytical query performance and cost on BigQuery / Snowflake.",
                        "Optimization strategies: 1) Partitioning by date, 2) Clustering on high-cardinality filter columns, 3) Materialized Views for pre-aggregated metrics, 4) QUALIFY clause with window functions, 5) Query cost analysis.",
                        "High-performance analytical queries scanning 90% fewer bytes with sub-second execution.",
                        "Project Difficulty: Hard | Tech: Google BigQuery, Snowflake, Partitioning, Query Optimization",
                        List.of(
                                new TestCase("Query partitioned & clustered table", "Scans 50MB instead of full 50GB table scan (99% cost reduction)", true, "Query optimized"),
                                new TestCase("Use QUALIFY clause for deduplication", "QUALIFY ROW_NUMBER() OVER (...) = 1 eliminates subqueries", false),
                                new TestCase("Query Materialized View", "Optimizer automatically rewrites query to use pre-aggregated view", false)
                        ),
                        "In BigQuery, partition by date and cluster on up to 4 frequently filtered dimension columns."
                )
        );
    }

    // =========================================================================
    // DATA SCIENCE & ANALYTICS - Business Intelligence & Streamlit Dashboards (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildBiDashboardsProblems() {
        String topic = "bi_dashboards";
        String topicTitle = "Business Intelligence & Streamlit Dashboards";

        return List.of(
                new DsaProblem(
                        "BI-101",
                        topic,
                        topicTitle,
                        "A. Interactive Business KPI Dashboard with Streamlit",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Build an executive KPI analytics dashboard using Streamlit and Python.",
                        "Streamlit app: st.metric widgets showing revenue/conversions with delta indicators, date range slider, categorical multi-select filters, st.dataframe with formatting, and CSV download button.",
                        "Responsive web dashboard displaying real-time metrics with intuitive filter controls.",
                        "Project Difficulty: Easy | Tech: Python, Streamlit, Data Visualization, UI",
                        List.of(
                                new TestCase("Launch Streamlit app", "Renders KPI metric cards with green/red trend delta badges", true, "Dashboard loaded"),
                                new TestCase("Select date range filter", "All metrics and charts re-calculate reactively", false),
                                new TestCase("Click 'Download Report' button", "Exports filtered dataset to CSV file immediately", false)
                        ),
                        "Use st.cache_data decorator on data loading functions to prevent re-fetching on every interaction."
                ),
                new DsaProblem(
                        "BI-102",
                        topic,
                        topicTitle,
                        "B. Dynamic Multi-Tab Analytical App with Plotly Dash",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Create a multi-tab web analytics application using Plotly Dash.",
                        "Dash application: dcc.Tabs layout (Overview, Product Performance, Customer Demographics), interactive callbacks connecting dropdowns to dcc.Graph figures, and responsive Bootstrap grid.",
                        "Production-ready Dash application featuring cross-filtering and reactive charting.",
                        "Project Difficulty: Easy | Tech: Plotly Dash, Callbacks, dash-bootstrap-components",
                        List.of(
                                new TestCase("Select product category from dropdown", "Dash callback updates line chart and bar chart simultaneously", true, "Callback executed"),
                                new TestCase("Switch between tabs", "Loads distinct analytical views without losing filter state", false),
                                new TestCase("Hover over data points", "Custom tooltip displays detailed breakdown card", false)
                        ),
                        "Use dash.callback_context to determine which input component triggered the callback."
                ),
                new DsaProblem(
                        "BI-103",
                        topic,
                        topicTitle,
                        "C. Real-Time Streaming Telemetry Dashboard",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build a real-time updating live telemetry monitoring dashboard in Streamlit.",
                        "Live monitoring app: simulate streaming IoT/financial metrics, use st.empty() placeholder containers for zero-flicker live chart updates, trigger alert banners when thresholds breach, and track system status.",
                        "Real-time live updating dashboard monitoring high-frequency data streams.",
                        "Project Difficulty: Medium | Tech: Streamlit, Real-Time Streaming, Async UI",
                        List.of(
                                new TestCase("Stream live sensor data at 10Hz", "Line charts update smoothly without full page reload", true, "Live stream active"),
                                new TestCase("Simulate metric breach (> 90C)", "Renders prominent st.error alert banner immediately", false),
                                new TestCase("Toggle pause stream", "Freezes live update loop to allow detailed chart inspection", false)
                        ),
                        "Use st.empty() containers to overwrite and update specific visual elements in place."
                ),
                new DsaProblem(
                        "BI-104",
                        topic,
                        topicTitle,
                        "D. Executive BI Dashboard in Power BI / Tableau",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Design an executive sales analytics dashboard in Power BI / Tableau with drill-through.",
                        "BI dashboard: Executive Summary, Sales Funnel visual, decomposition tree for root-cause analysis, drill-through from continent to city level, DAX measures (YoY Growth %, YTD Total), and RLS security.",
                        "Enterprise BI dashboard with hierarchical drill-through and calculated business measures.",
                        "Project Difficulty: Medium | Tech: Power BI, DAX, Tableau, Business Intelligence",
                        List.of(
                                new TestCase("Click continent on map", "Cross-filters all revenue visuals and drill-through to store level", true, "Drill-through verified"),
                                new TestCase("Inspect YoY Growth DAX measure", "CALCULATE(SUM(Sales), SAMEPERIODLASTYEAR(Date)) computes correctly", false),
                                new TestCase("Test Row-Level Security (RLS)", "Regional managers view only their assigned territory data", false)
                        ),
                        "Create a dedicated Date table marked as Date Table in Power BI for time intelligence DAX functions."
                ),
                new DsaProblem(
                        "BI-105",
                        topic,
                        topicTitle,
                        "E. Full-Stack BI Platform with Apache Superset & PostgreSQL",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Deploy and configure an enterprise open-source BI platform using Apache Superset and Docker.",
                        "BI platform setup: 1) Docker Compose multi-container stack (Superset, PostgreSQL, Redis, Celery workers), 2) Connect data sources, 3) Create virtual SQL datasets, 4) Author interactive dashboard with cross-filters.",
                        "Production-ready open-source BI platform with scheduled email/Slack reporting.",
                        "Project Difficulty: Hard | Tech: Apache Superset, Docker Compose, SQL Lab, Redis, Celery",
                        List.of(
                                new TestCase("Launch Superset stack via docker-compose", "All containers (web, worker, beat, redis, db) start healthy", true, "Stack deployed"),
                                new TestCase("Build interactive dashboard in Superset", "Creates 8 interconnected charts with global cross-filter bar", false),
                                new TestCase("Configure scheduled report", "Celery worker automatically captures dashboard PDF and sends email", false)
                        ),
                        "Configure Celery background workers in Superset to handle long-running queries asynchronously."
                )
        );
    }

    // =========================================================================
    // GAME DEVELOPMENT & GRAPHICS (8 Topics x 5 Exercises)
    // =========================================================================

    // =========================================================================
    // GAME DEVELOPMENT & GRAPHICS - Game Math, Vectors & Core Game Loop Architecture (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildMathGamesProblems() {
        String topic = "math_games";
        String topicTitle = "Game Math, Vectors & Core Game Loop Architecture";

        return List.of(
                new DsaProblem(
                        "MG-101",
                        topic,
                        topicTitle,
                        "A. 2D Vector Math Engine from Scratch",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Implement a complete 2D Vector mathematical class for game physics and motion.",
                        "Vector2 class in Python/C#: vector addition, subtraction, scalar multiplication, dot product, cross product magnitude, length/magnitude, normalize (unit vector), distance, angle, and linear interpolation (lerp).",
                        "Robust 2D vector mathematics library supporting essential game calculations.",
                        "Project Difficulty: Easy | Tech: Python, Math, Game Physics, Vector Math",
                        List.of(
                                new TestCase("Calculate dot product of two orthogonal vectors", "Returns 0.0 confirming perpendicular angle", true, "Dot product verified"),
                                new TestCase("Normalize vector (3, 4)", "Returns unit vector (0.6, 0.8) with length 1.0", false),
                                new TestCase("Lerp between (0, 0) and (10, 20) with t=0.5", "Returns midpoint vector (5.0, 10.0)", false)
                        ),
                        "Normalize checks for zero-length vectors to prevent division-by-zero runtime exceptions."
                ),
                new DsaProblem(
                        "MG-102",
                        topic,
                        topicTitle,
                        "B. Fixed Timestep Game Loop with Interpolation",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Build a deterministic fixed-timestep game loop architecture.",
                        "Game loop implementation: fixed physics timestep (e.g. 1/60s = 16.67ms) with accumulator pattern, variable rendering framerate, and alpha interpolation factor for smooth visual rendering between physics ticks.",
                        "Deterministic game loop decoupling physics simulation from display refresh rate.",
                        "Project Difficulty: Easy | Tech: Python, Game Architecture, Delta Time, Game Loop",
                        List.of(
                                new TestCase("Simulate slow render frame (50ms)", "Loop executes exactly 3 physics steps to catch up", true, "Physics catch-up verified"),
                                new TestCase("Simulate fast 144Hz monitor", "Physics runs at exact 60Hz; renders smoothly with alpha interpolation", false),
                                new TestCase("Measure physics consistency", "Object trajectories remain identical regardless of framerate fluctuations", false)
                        ),
                        "Cap max frame time (e.g. 250ms) to prevent the 'spiral of death' when frames lag severely."
                ),
                new DsaProblem(
                        "MG-103",
                        topic,
                        topicTitle,
                        "C. 2D Transformation Matrices & Hierarchical Transforms",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Implement 3x3 affine transformation matrices for game scene graphs.",
                        "Matrix3x3 class: Translation(tx, ty), Rotation(theta), Scale(sx, sy), Matrix Multiplication (A * B), TransformPoint(p), and hierarchical scene graph node updating local-to-world matrices.",
                        "Mathematical transformation engine supporting parent-child object hierarchies.",
                        "Project Difficulty: Medium | Tech: 2D Matrices, Linear Algebra, Scene Graph, Affine Transforms",
                        List.of(
                                new TestCase("Combine Scale -> Rotate -> Translate", "Matrix multiplication produces correct composite affine matrix", true, "Transforms verified"),
                                new TestCase("Rotate parent node by 90 degrees", "Child nodes orbit parent while maintaining relative local offsets", false),
                                new TestCase("Compute Inverse Transformation", "Inverse matrix converts world coordinates back to local space", false)
                        ),
                        "Matrix multiplication is non-commutative; the order of operations (TRS vs SRT) matters."
                ),
                new DsaProblem(
                        "MG-104",
                        topic,
                        topicTitle,
                        "D. 2D Camera Controller with Deadzone & Smooth Damping",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build a 2D camera system with deadzone, smooth damping (lerp), and camera shake.",
                        "Camera2D controller: target tracking with smooth exponential damping, rectangular deadzone boundary (camera moves only when player exits box), screen bounds clamping, and decaying trauma camera shake.",
                        "Polished 2D game camera delivering smooth following and impactful screen shake.",
                        "Project Difficulty: Medium | Tech: Game Camera, Lerp, Deadzone, Screen Shake",
                        List.of(
                                new TestCase("Player moves inside deadzone box", "Camera remains stationary with zero jitter", true, "Deadzone verified"),
                                new TestCase("Player exits deadzone boundary", "Camera smoothly accelerates and catches up to player", false),
                                new TestCase("Trigger explosion trauma shake", "Camera shakes with decaying oscillating offset", false)
                        ),
                        "Use non-linear trauma^2 or trauma^3 decay for punchier and more natural camera shake."
                ),
                new DsaProblem(
                        "MG-105",
                        topic,
                        topicTitle,
                        "E. Spatial Partitioning: Quadtree for Collision Optimization",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Implement a 2D Quadtree spatial partitioning structure for O(N log N) collision queries.",
                        "Quadtree data structure: recursive node subdivision (NW, NE, SW, SE) when capacity > 4, insert(entity), retrieve(boundary) returning candidate collision pairs, and benchmark against brute-force O(N^2).",
                        "High-performance spatial index accelerating collision checks for thousands of active entities.",
                        "Project Difficulty: Hard | Tech: Quadtree, Spatial Partitioning, Collision Detection, O(N log N)",
                        List.of(
                                new TestCase("Insert 2,000 moving entities into Quadtree", "Quadtree dynamically subdivides regions with high entity density", true, "Quadtree built"),
                                new TestCase("Query potential collisions", "Reduces pairwise checks from 2,000,000 to < 15,000 per frame", false),
                                new TestCase("Benchmark against brute-force", "Maintains 60 FPS where brute-force drops to < 5 FPS", false)
                        ),
                        "Clear and rebuild the Quadtree each frame for dynamic moving entities in 2D games."
                )
        );
    }

    // =========================================================================
    // GAME DEVELOPMENT & GRAPHICS - 2D Game Engineering with Pygame & Python (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildPygameProblems() {
        String topic = "pygame";
        String topicTitle = "2D Game Engineering with Pygame & Python";

        return List.of(
                new DsaProblem(
                        "PG-101",
                        topic,
                        topicTitle,
                        "A. Complete Retro Breakout / Arkanoid Game",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Build a complete Breakout brick-breaker game in Pygame from scratch.",
                        "Pygame game: Paddle controlled by keyboard/mouse, bouncing Ball with vector reflection, colorful grid of breakable Bricks, score counter, lives system (3 lives), sound effects, and win/loss states.",
                        "Playable 60fps Breakout arcade game with full game loop and collision mechanics.",
                        "Project Difficulty: Easy | Tech: Python, Pygame, 2D Collisions, Sound FX",
                        List.of(
                                new TestCase("Launch Pygame Breakout", "Renders paddle, ball, bricks grid, score and lives UI", true, "Game initialized"),
                                new TestCase("Ball collides with brick", "Brick destroyed, score increments, ball velocity reflects", false),
                                new TestCase("Ball passes below paddle", "Loses 1 life; triggers respawn; game over at 0 lives", false)
                        ),
                        "Adjust ball reflection angle based on where it strikes the paddle for skilled gameplay."
                ),
                new DsaProblem(
                        "PG-102",
                        topic,
                        topicTitle,
                        "B. Top-Down Space Shooter with Sprite Groups",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Create a top-down vertical arcade space shooter in Pygame.",
                        "Space shooter: Player spaceship with keyboard movement and shooting, enemy waves spawning from top, laser projectiles, pygame.sprite.Group collision detection, particle explosions, and scrolling starfield.",
                        "Engaging 2D arcade shooter with sprite management and animated starfield.",
                        "Project Difficulty: Easy | Tech: Pygame, Sprite Groups, Collision Detection, Particle Systems",
                        List.of(
                                new TestCase("Hold spacebar to shoot", "Fires laser projectiles managed in pygame.sprite.Group", true, "Shooting active"),
                                new TestCase("Laser collides with enemy", "pygame.sprite.groupcollide detects hit, spawns explosion particles", false),
                                new TestCase("Scrolling background", "Dual-layer parallax starfield scrolls continuously", false)
                        ),
                        "Call sprite.kill() on off-screen projectiles and destroyed enemies to prevent memory leaks."
                ),
                new DsaProblem(
                        "PG-103",
                        topic,
                        topicTitle,
                        "C. 2D Platformer Physics: Gravity, Jumping & Tiles",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Implement 2D platformer character physics with tilemap collision in Pygame.",
                        "Platformer mechanics: gravity acceleration, horizontal friction/acceleration, variable height jump, wall/floor collision with tilemap, coyote time (jump shortly after leaving ledge), and jump buffering.",
                        "Responsive, satisfying platformer movement physics without getting stuck in walls.",
                        "Project Difficulty: Medium | Tech: Pygame, Platformer Physics, Tilemaps, AABB Collision",
                        List.of(
                                new TestCase("Player runs and jumps", "Smooth parabolic jump arc with variable jump height on key release", true, "Jump physics verified"),
                                new TestCase("Walk off platform ledge", "Coyote time allows valid jump for 100ms after falling off", false),
                                new TestCase("Collide with solid tilemap wall", "AABB collision resolution stops player without snagging", false)
                        ),
                        "Separate horizontal and vertical movement and collision checks to avoid corner snagging."
                ),
                new DsaProblem(
                        "PG-104",
                        topic,
                        topicTitle,
                        "D. Finite State Machine (FSM) Enemy AI & Pathfinding",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build an intelligent enemy AI with Finite State Machine and grid pathfinding.",
                        "Enemy AI states: Patrol (moves between waypoints), Chase (tracks player using A* or Breadth-First Search when in line-of-sight), Attack (strikes within melee range), and Flee (when health < 25%).",
                        "Autonomous enemy AI demonstrating intelligent behavior transitions and obstacle avoidance.",
                        "Project Difficulty: Medium | Tech: Pygame, FSM, Enemy AI, A* Pathfinding",
                        List.of(
                                new TestCase("Player enters detection radius", "Enemy transitions from Patrol state to Chase state", true, "FSM transition verified"),
                                new TestCase("Place obstacle between enemy & player", "A* algorithm computes shortest path around obstacles", false),
                                new TestCase("Enemy health drops below 25%", "Transitions to Flee state and retreats in opposite direction", false)
                        ),
                        "Use a visual debug toggle in Pygame to render enemy FOV cones and current AI state text."
                ),
                new DsaProblem(
                        "PG-105",
                        topic,
                        topicTitle,
                        "E. Complete 2D Action RPG Demo with Inventory & Audio",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Develop a full 2D action RPG demo in Pygame with tilemaps, combat, and inventory.",
                        "Action RPG: Tiled map loading (.tmx) with camera scrolling, animated character spritesheets (idle, walk, attack in 4 directions), melee combat with hitboxes, item pickup and grid inventory UI, BGM and SFX.",
                        "Complete mini action RPG featuring combat, inventory, animated sprites, and audio.",
                        "Project Difficulty: Hard | Tech: Pygame, TMX Tilemaps, Spritesheet Animation, RPG Inventory",
                        List.of(
                                new TestCase("Walk across large map", "Camera smoothly follows player across multi-room tilemap", true, "RPG world active"),
                                new TestCase("Press attack button", "Plays 4-frame attack animation, checks sword hitbox against enemies", false),
                                new TestCase("Open Inventory (press 'I')", "Renders grid inventory; drag/drop or click items to equip/use", false)
                        ),
                        "Organize Pygame code using an App/State Manager (MenuState, PlayState, PauseState)."
                )
        );
    }

    // =========================================================================
    // GAME DEVELOPMENT & GRAPHICS - Unity Engine Foundations & C# Scripting (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildUnityBasicsProblems() {
        String topic = "unity_basics";
        String topicTitle = "Unity Engine Foundations & C# Scripting";

        return List.of(
                new DsaProblem(
                        "UB-101",
                        topic,
                        topicTitle,
                        "A. Unity 3D Scene Setup & Material Configuration",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Set up a complete Unity 3D environment with lighting, materials, and physics colliders.",
                        "Unity 3D project: configure scene hierarchy, Directional Light with soft shadows, custom Skybox, ground plane with PBR material (Albedo, Smoothness), player cube with Rigidbody, and collectible spheres.",
                        "Properly configured Unity 3D scene with accurate lighting and physics colliders.",
                        "Project Difficulty: Easy | Tech: Unity 3D, C#, PBR Materials, Physics Colliders",
                        List.of(
                                new TestCase("Press Play in Unity Editor", "Player cube drops with physics and lands on ground collider", true, "Physics verified"),
                                new TestCase("Inspect scene lighting", "Directional light casts realistic soft shadows on floor", false),
                                new TestCase("Inspect materials", "Collectible spheres render shiny gold metallic PBR material", false)
                        ),
                        "Freeze rotation on Rigidbody X and Z axes for upright character controller physics."
                ),
                new DsaProblem(
                        "UB-102",
                        topic,
                        topicTitle,
                        "B. C# Character Controller & Physics Movement",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Write a smooth C# character controller script using Unity Input System and Rigidbody.",
                        "PlayerMovement.cs: read WASD / Arrow input, Rigidbody.MovePosition or AddForce with smooth acceleration, ground check using Physics.Raycast or OverlapSphere, and jumping with ForceMode.Impulse.",
                        "Responsive, robust 3D character controller with reliable ground detection.",
                        "Project Difficulty: Easy | Tech: Unity C#, Rigidbody, Physics.Raycast, Input System",
                        List.of(
                                new TestCase("Press WASD keys", "Player moves smoothly in camera-relative direction", true, "Movement verified"),
                                new TestCase("Press Spacebar while on ground", "Executes jump with ForceMode.Impulse", false),
                                new TestCase("Press Spacebar while in mid-air", "Raycast ground check returns false; prevents infinite air jumping", false)
                        ),
                        "Always update physics movement inside FixedUpdate(), never in Update()."
                ),
                new DsaProblem(
                        "UB-103",
                        topic,
                        topicTitle,
                        "C. Prefab Spawner with Object Pooling Pattern",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Implement high-performance enemy spawning in Unity using the Object Pooling design pattern.",
                        "ObjectPooler.cs: pre-instantiate a pool of 30 enemy prefabs on Awake. SpawnManager activates enemies from pool with SpawnFromPool() and deactivates on death, eliminating GC stutter.",
                        "Zero-allocation Unity object pooling system maintaining stable 60+ FPS.",
                        "Project Difficulty: Medium | Tech: Unity C#, Object Pooling, Memory Management, Coroutines",
                        List.of(
                                new TestCase("Spawn 50 consecutive enemies", "Re-uses pre-allocated pool objects with zero Instantiate() GC lag", true, "Pool verified"),
                                new TestCase("Enemy reaches goal or dies", "Deactivates and returns to pool Queue<GameObject>", false),
                                new TestCase("Inspect Unity Profiler", "Memory allocations (GC.Alloc) remain completely flat during spawning", false)
                        ),
                        "Use Queue<GameObject> inside the object pool for O(1) dequeue and enqueue operations."
                ),
                new DsaProblem(
                        "UB-104",
                        topic,
                        topicTitle,
                        "D. Interactive Health Bar UI with Animations & Canvas",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build a dynamic health and stamina UI system using Unity Canvas and TextMeshPro.",
                        "PlayerHealth.cs and HealthBarUI.cs: Screen Space - Overlay Canvas, Slider health bar with smooth Mathf.Lerp fill animation, damage flash screen overlay (Coroutine), and TextMeshPro numeric display.",
                        "Polished game UI featuring animated health bars and visual damage feedback.",
                        "Project Difficulty: Medium | Tech: Unity UI, Canvas, TextMeshPro, Coroutines, Mathf.Lerp",
                        List.of(
                                new TestCase("Player takes 25 damage", "Health bar smoothly animates down from 100 to 75", true, "Damage UI verified"),
                                new TestCase("Damage flash effect", "Screen briefly flashes red vignette overlay via Coroutine", false),
                                new TestCase("Health reaches 0", "Fires onPlayerDied event; activates Game Over modal panel", false)
                        ),
                        "Use CanvasScaler set to 'Scale With Screen Size' to ensure UI adapts across resolutions."
                ),
                new DsaProblem(
                        "UB-105",
                        topic,
                        topicTitle,
                        "E. Complete 3D Ball Rolling Arcade Mini-Game",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Develop a complete 3D marble arcade mini-game in Unity with audio and save data.",
                        "Complete mini-game: physics ball rolling with torque, collectible coins with particle effects, moving obstacle hazards, Cinemachine camera tracking, AudioSource SFX/BGM, and PlayerPrefs high score save.",
                        "Fully playable 3D arcade game with audio, physics, score tracking, and persistent saves.",
                        "Project Difficulty: Hard | Tech: Unity, C#, Cinemachine, AudioSource, PlayerPrefs",
                        List.of(
                                new TestCase("Roll ball to collect coins", "Coins vanish with particle burst, play chime SFX, update score", true, "Mini-game verified"),
                                new TestCase("Collide with moving hazard", "Resets ball to last checkpoint with hit sound effect", false),
                                new TestCase("Complete level", "Saves high score to PlayerPrefs; displays Victory screen with final time", false)
                        ),
                        "Use Cinemachine Virtual Camera for smooth camera following with zero custom math scripts."
                )
        );
    }

    // =========================================================================
    // GAME DEVELOPMENT & GRAPHICS - Unity 3D Lighting, Materials & Level Design (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildUnity3dProblems() {
        String topic = "unity_3d";
        String topicTitle = "Unity 3D Lighting, Materials & Level Design";

        return List.of(
                new DsaProblem(
                        "U3-101",
                        topic,
                        topicTitle,
                        "A. Custom Shaders with Unity URP Shader Graph",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Author custom visual shaders in Unity Universal Render Pipeline (URP) using Shader Graph.",
                        "Shader Graph shaders: 1) Dissolve effect using 3D Simplex Noise, Step node, and glowing orange emission edge, 2) Hologram scanline shader with Fresnel Effect and scrolling sine waves.",
                        "Dynamic visual shaders with exposed material properties for runtime tweaking.",
                        "Project Difficulty: Easy | Tech: Unity URP, Shader Graph, Fresnel, Dissolve Shader",
                        List.of(
                                new TestCase("Animate Dissolve material parameter from 0 to 1", "3D mesh dissolves into glowing embers and vanishes", true, "Shader verified"),
                                new TestCase("Inspect Hologram material", "Outer silhouette glows with Fresnel edge and scrolling scanlines", false),
                                new TestCase("Expose properties to Inspector", "Material properties (Color, Speed, Threshold) adjustable in real-time", false)
                        ),
                        "Use the Alpha Clip Threshold node in Shader Graph for clean binary dissolve clipping."
                ),
                new DsaProblem(
                        "U3-102",
                        topic,
                        topicTitle,
                        "B. 3D Environment Design with Unity Terrain Tools",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Sculpt and paint a detailed 3D natural landscape using Unity Terrain tools.",
                        "Terrain setup: sculpted mountains/valleys with height brushes, multi-texture painting (grass, rock on steep slopes, sand), procedural grass detail meshes, tree placement with Wind Zone swaying.",
                        "Expansive 3D natural environment with realistic textures and wind-animated vegetation.",
                        "Project Difficulty: Easy | Tech: Unity Terrain, Heightmaps, Wind Zones, Foliage",
                        List.of(
                                new TestCase("Inspect sculpted terrain", "Mountain peaks have rock textures; flat valleys have lush grass", true, "Terrain sculpted"),
                                new TestCase("Add Wind Zone", "Foliage and tree branches sway dynamically in response to wind", false),
                                new TestCase("Navigate player across terrain", "TerrainCollider provides seamless collision across hills", false)
                        ),
                        "Set texture layer slope angles so cliffs automatically display rock textures instead of grass."
                ),
                new DsaProblem(
                        "U3-103",
                        topic,
                        topicTitle,
                        "C. Advanced Baked & Real-Time Lighting with Light Probes",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Configure professional lighting in Unity using Baked Global Illumination and Light Probes.",
                        "Lighting setup: Mixed Lighting Mode, static geometry lightmap baking with GPU Progressive Lightmapper, Reflection Probes, and 3D Light Probe Group providing indirect light to dynamic objects.",
                        "Visually stunning scene with realistic soft shadows and indirect global illumination.",
                        "Project Difficulty: Medium | Tech: Unity Lighting, Lightmapping, Light Probes, Reflection Probes",
                        List.of(
                                new TestCase("Bake lighting with Progressive Lightmapper", "Generates high-res baked lightmaps for static scenery", true, "Lighting baked"),
                                new TestCase("Move dynamic player through shadow and light", "Light Probe Group smoothly interpolates indirect lighting onto player", false),
                                new TestCase("Inspect metallic surfaces", "Reflection Probes cast accurate local reflections", false)
                        ),
                        "Mark all non-moving scenery as 'Contribute GI' (Static) to include them in lightmap baking."
                ),
                new DsaProblem(
                        "U3-104",
                        topic,
                        topicTitle,
                        "D. Post-Processing Stack & Volume Profiles",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Enhance visual fidelity using Unity Post-Processing Volumes in URP.",
                        "Post-processing volume profile: Tonemapping (ACES), Bloom (with threshold and intensity), Color Adjustments, Vignette, Depth of Field focusing on player, and Motion Blur.",
                        "Cinematic visual presentation matching AAA game rendering standards.",
                        "Project Difficulty: Medium | Tech: Unity URP, Post-Processing, ACES Tonemapping, Bloom",
                        List.of(
                                new TestCase("Enable ACES Tonemapping", "Dynamic range expands with cinematic contrast and color grading", true, "Post-processing active"),
                                new TestCase("Inspect emissive materials with Bloom", "Glowing lights produce realistic radiant light bleeds", false),
                                new TestCase("Adjust Depth of Field", "Background blurs smoothly based on camera focal distance", false)
                        ),
                        "Ensure 'Post Processing' is enabled on the Main Camera component in URP."
                ),
                new DsaProblem(
                        "U3-105",
                        topic,
                        topicTitle,
                        "E. Level Streaming & Occlusion Culling Optimization",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Optimize large open-world level performance using Occlusion Culling and Scene Streaming.",
                        "Optimization system: 1) Bake Occlusion Culling to hide off-screen geometry, 2) Asynchronous multi-scene additive loading (SceneManager.LoadSceneAsync), 3) LOD Groups reducing mesh complexity with distance.",
                        "High-performance open-world level maintaining 60 FPS with rapid scene streaming.",
                        "Project Difficulty: Hard | Tech: Unity, Occlusion Culling, Scene Streaming, LOD Groups",
                        List.of(
                                new TestCase("Bake Occlusion Culling in Occlusion window", "Camera frustum visualizer culls occluded geometry in real-time", true, "Occlusion culled"),
                                new TestCase("Walk between world zones", "Additive scene loading streams next zone asynchronously in background", false),
                                new TestCase("Inspect mesh LOD Group", "Mesh switches between LOD 0 (high-poly) and LOD 2 (low-poly) based on distance", false)
                        ),
                        "Combine small static meshes using Static Batching or Mesh.CombineMeshes to reduce draw calls."
                )
        );
    }

    // =========================================================================
    // GAME DEVELOPMENT & GRAPHICS - Unreal Engine 5 Foundations & Visual Blueprints (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildUnrealProblems() {
        String topic = "unreal";
        String topicTitle = "Unreal Engine 5 Foundations & Visual Blueprints";

        return List.of(
                new DsaProblem(
                        "UR-101",
                        topic,
                        topicTitle,
                        "A. Unreal Engine 5 Project Setup with Nanite & Lumen",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Initialize an Unreal Engine 5 project and configure Nanite and Lumen technologies.",
                        "UE5 configuration: Third Person template, enable Nanite virtualized geometry on high-poly static meshes, configure Lumen dynamic Global Illumination and Reflections, and setup Level Blueprint.",
                        "Photorealistic UE5 scene utilizing next-generation real-time geometry and lighting.",
                        "Project Difficulty: Easy | Tech: Unreal Engine 5, Nanite, Lumen, Level Blueprint",
                        List.of(
                                new TestCase("Import 2M polygon mesh with Nanite enabled", "Renders seamlessly with zero LOD pop and high performance", true, "Nanite active"),
                                new TestCase("Move light source in real-time", "Lumen bounces indirect global illumination dynamically across surfaces", false),
                                new TestCase("Level Blueprint BeginPlay", "Executes initialization logic and prints debug confirmation string", false)
                        ),
                        "Nanite eliminates the need for manual LOD generation on opaque static meshes."
                ),
                new DsaProblem(
                        "UR-102",
                        topic,
                        topicTitle,
                        "B. Blueprint Character Movement & Enhanced Input System",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Implement 3D character movement in UE5 using the Enhanced Input System in Blueprints.",
                        "Character Blueprint: Input Mapping Context (IMC), Input Actions (IA_Move, IA_Look, IA_Jump, IA_Sprint), CharacterMovementComponent tuning (Max Walk Speed, Jump Z-Velocity, Air Control).",
                        "Fluid, responsive 3D character controller implementing modern UE5 Enhanced Input.",
                        "Project Difficulty: Easy | Tech: Unreal Engine 5, Blueprints, Enhanced Input System",
                        List.of(
                                new TestCase("Press WASD keys", "IA_Move triggers Add Movement Input in camera-relative direction", true, "Movement verified"),
                                new TestCase("Press Shift to Sprint", "Increases Max Walk Speed from 600 to 950 with smooth acceleration", false),
                                new TestCase("Mouse look input", "IA_Look adds Controller Pitch and Yaw input with configurable sensitivity", false)
                        ),
                        "Use Enhanced Input Mapping Contexts to easily swap input configurations (e.g. driving vs on-foot)."
                ),
                new DsaProblem(
                        "UR-103",
                        topic,
                        topicTitle,
                        "C. Interactive Blueprint Item & Inventory System",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build an interactive item pickup and inventory system in Unreal Engine 5 Blueprints.",
                        "Blueprint architecture: BPI_Interactable Blueprint Interface, Master Item Pickup Actor with rotating mesh and sphere trigger, ActorComponent Inventory with item structs array, and UMG inventory UI.",
                        "Modular inventory system with decoupled interface-driven object interaction.",
                        "Project Difficulty: Medium | Tech: UE5 Blueprints, Blueprint Interfaces, UMG UI, Structs",
                        List.of(
                                new TestCase("Look at item and press 'E'", "BPI_Interactable interface triggers; item added to player inventory", true, "Item collected"),
                                new TestCase("Open Inventory Widget (press 'Tab')", "UMG ScrollBox dynamically renders collected item icons and counts", false),
                                new TestCase("Drop item from inventory", "Spawns item actor back into world at player location", false)
                        ),
                        "Use Blueprint Interfaces to allow interaction with any actor without direct casting dependencies."
                ),
                new DsaProblem(
                        "UR-104",
                        topic,
                        topicTitle,
                        "D. Enemy AI with Behavior Trees & Perception",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Implement intelligent enemy AI in UE5 using Behavior Trees, Blackboards, and AI Perception.",
                        "AI setup: AIController with AIPerception component (Sight, Hearing), Blackboard asset (TargetActor, State), Behavior Tree with Selector/Sequence nodes, NavMeshBounds, and custom BTTasks for attacking.",
                        "Autonomous enemy AI perceiving player, navigating NavMesh, and executing state trees.",
                        "Project Difficulty: Medium | Tech: UE5 AI, Behavior Trees, Blackboard, AIPerception",
                        List.of(
                                new TestCase("Walk into enemy Sight cone", "AIPerception fires; Blackboard sets TargetActor and begins Chase", true, "AI detected player"),
                                new TestCase("Break line of sight behind wall", "AI navigates to last known location, searches area, then resumes Patrol", false),
                                new TestCase("Reach melee attack range", "BTTask_Attack triggers combat animation and applies damage", false)
                        ),
                        "Always place a NavMeshBoundsVolume over the play area and press 'P' to visualize navigable surfaces."
                ),
                new DsaProblem(
                        "UR-105",
                        topic,
                        topicTitle,
                        "E. Action Combat System with Animation Montages & Niagara",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Develop a third-person melee combat system with Animation Montages, combos, and Niagara VFX.",
                        "Combat system: Combo attack system with AnimMontage branching via AnimNotifies, Weapon Collision trace using AnimNotifyState, hit reaction flinch animations, and Niagara particle impact sparks.",
                        "High-impact responsive third-person melee combat with combo windows and particle feedback.",
                        "Project Difficulty: Hard | Tech: UE5 Combat, Animation Montages, AnimNotifies, Niagara VFX",
                        List.of(
                                new TestCase("Press Attack button sequentially", "Executes 3-hit combo sequence with dynamic combo windows", true, "Combo verified"),
                                new TestCase("Weapon hits enemy during attack window", "Box trace triggers hit sound, flinch animation, and Niagara sparks", false),
                                new TestCase("Dodge / Roll action", "Cancels attack recovery and grants temporary invulnerability frames (i-frames)", false)
                        ),
                        "Use AnimNotifyState to define the exact window during an animation where weapon hitboxes are active."
                )
        );
    }

    // =========================================================================
    // GAME DEVELOPMENT & GRAPHICS - Game Physics, Rigidbodies, Collisions & Ragdolls (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildGamePhysicsProblems() {
        String topic = "game_physics";
        String topicTitle = "Game Physics, Rigidbodies, Collisions & Ragdolls";

        return List.of(
                new DsaProblem(
                        "GP-101",
                        topic,
                        topicTitle,
                        "A. 2D Verlet Integration Particle Physics",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Implement a 2D particle physics simulation using Verlet integration from scratch.",
                        "Verlet physics engine: Particle class (position, old_position, acceleration), Verlet integration formula (pos += pos - old_pos + acc * dt^2), gravity, boundary constraint box, and elastic particle collisions.",
                        "Stable, energy-conserving physics simulator capable of simulating hundreds of bouncing particles.",
                        "Project Difficulty: Easy | Tech: Python, Verlet Integration, Physics Simulation",
                        List.of(
                                new TestCase("Simulate 500 bouncing particles", "Verlet integration conserves energy without numerical explosion", true, "Simulation active"),
                                new TestCase("Particle strikes container wall", "Boundary constraint reflects velocity with restitution dampening", false),
                                new TestCase("Compare against Euler integration", "Verlet remains stable where explicit Euler introduces artificial energy", false)
                        ),
                        "Verlet integration implicitly stores velocity as (position - old_position) / dt."
                ),
                new DsaProblem(
                        "GP-102",
                        topic,
                        topicTitle,
                        "B. 3D Projectile Trajectory Prediction & Raycasting",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Implement realistic 3D projectile ballistic motion with visual trajectory arc prediction.",
                        "Physics system: launch projectile with initial velocity vector and gravity, calculate ballistic trajectory points using kinematic equations, and render preview arc using LineRenderer / debug drawing.",
                        "Accurate projectile trajectory predictor calculating exact impact coordinates.",
                        "Project Difficulty: Easy | Tech: Game Physics, Kinematics, Trajectory Prediction, Ballistics",
                        List.of(
                                new TestCase("Aim launcher at 45 degree angle", "LineRenderer draws parabolic trajectory arc matching actual flight", true, "Trajectory predicted"),
                                new TestCase("Fire projectile", "Physical projectile travels precisely along predicted trajectory curve", false),
                                new TestCase("Raycast predicts impact point", "Detects target obstacle collision point before projectile launch", false)
                        ),
                        "Include drag / air resistance in trajectory calculations for realistic high-speed bullet physics."
                ),
                new DsaProblem(
                        "GP-103",
                        topic,
                        topicTitle,
                        "C. Constraint Physics: Verlet Cloth & Rope Simulation",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Build a 2D/3D cloth and rope physics simulation using Distance Constraints.",
                        "Cloth simulation: grid of Verlet particles connected by distance constraints (structural, shear, flexion springs). Iterate constraint satisfaction (Relaxation / Jacobi solver) 5 times per frame.",
                        "Fluid, responsive cloth and rope simulation reacting realistically to gravity, wind, and tears.",
                        "Project Difficulty: Medium | Tech: Verlet Physics, Distance Constraints, Cloth Simulation",
                        List.of(
                                new TestCase("Pin top two corners of cloth grid", "Cloth drapes realistically under gravity with natural folds", true, "Cloth simulated"),
                                new TestCase("Apply directional wind force", "Cloth ripples and flutters in simulated wind stream", false),
                                new TestCase("Cut constraint links (scissor tool)", "Cloth tears and splits into separate realistic hanging fragments", false)
                        ),
                        "Running 3 to 8 constraint relaxation iterations per frame strikes the sweet spot between stiffness and speed."
                ),
                new DsaProblem(
                        "GP-104",
                        topic,
                        topicTitle,
                        "D. Character Ragdoll Physics System",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Implement seamless character transition from skeletal animation to active Ragdoll physics.",
                        "Ragdoll system: humanoid bone hierarchy with Rigidbodies, CapsuleColliders, and ConfigurableJoints with angular limits. Toggle from Animator to physical ragdoll on death, and apply directional hit impulse.",
                        "Realistic death ragdoll physics with smooth animation blending.",
                        "Project Difficulty: Medium | Tech: Unity/UE5 Physics, Ragdoll, ConfigurableJoints, Skeletal Physics",
                        List.of(
                                new TestCase("Character receives fatal hit", "Disables Animator and activates all bone Rigidbodies simultaneously", true, "Ragdoll activated"),
                                new TestCase("Apply directional hit force to head bone", "Body collapses realistically in direction of impact momentum", false),
                                new TestCase("Joint limits inspection", "ConfigurableJoint angular limits prevent unnatural joint hyperextension", false)
                        ),
                        "Set Rigidbody.interpolation = Interpolate on ragdoll bones to prevent visual physics jitter."
                ),
                new DsaProblem(
                        "GP-105",
                        topic,
                        topicTitle,
                        "E. Custom 2D Rigid Body Physics Engine with SAT Collision",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Build a custom 2D rigid body physics engine with Separating Axis Theorem (SAT) and impulse resolution.",
                        "Engine components: 1) Polygon & Circle shapes with mass and inertia tensor, 2) SAT collision detection returning penetration depth and contact normal, 3) Sequential impulse solver resolving contact forces and friction.",
                        "Complete custom 2D physics engine simulating stacking boxes, friction, and rotation.",
                        "Project Difficulty: Hard | Tech: Physics Engine, SAT Collision, Impulse Resolution, Rotational Inertia",
                        List.of(
                                new TestCase("Drop stack of 5 polygonal boxes", "SAT detects multi-point contacts; impulse solver creates stable stack", true, "Boxes stacked"),
                                new TestCase("Angled box collision with friction", "Computes torque and rotational velocity using moment of inertia", false),
                                new TestCase("Measure energy conservation", "Restitution coefficient e correctly controls elastic vs inelastic bounces", false)
                        ),
                        "The Separating Axis Theorem states that two convex shapes do not overlap if a separating axis exists."
                )
        );
    }

    // =========================================================================
    // GAME DEVELOPMENT & GRAPHICS - Game Audio, Shaders & Visual Effects (VFX) (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildAudioVfxProblems() {
        String topic = "audio_vfx";
        String topicTitle = "Game Audio, Shaders & Visual Effects (VFX)";

        return List.of(
                new DsaProblem(
                        "AV-101",
                        topic,
                        topicTitle,
                        "A. Spatial 3D Audio & Dynamic AudioMixer Architecture",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Implement immersive 3D spatial audio and dynamic mixing in a game engine.",
                        "Audio architecture: AudioMixer with Master, Music, SFX, and Ambience groups. Spatial 3D sound with logarithmic distance rolloff, lowpass filter occlusion behind walls, and pitch randomization per sound effect.",
                        "Immersive spatial audio system with dynamic volume mixing and sound variation.",
                        "Project Difficulty: Easy | Tech: Game Audio, AudioMixer, 3D Spatial Audio, Sound Design",
                        List.of(
                                new TestCase("Walk away from 3D sound source", "Volume attenuates realistically based on distance rolloff curve", true, "3D audio verified"),
                                new TestCase("Walk behind solid concrete wall", "Lowpass audio filter activates, muffling high frequencies", false),
                                new TestCase("Rapidly play footstep SFX", "Randomizes pitch between 0.9x and 1.1x eliminating repetitive audio fatigue", false)
                        ),
                        "Always vary pitch by +/- 5-10% on repetitive sound effects like footsteps and gunshots."
                ),
                new DsaProblem(
                        "AV-102",
                        topic,
                        topicTitle,
                        "B. Particle Visual Effects with Niagara / Unity Particle System",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Create dynamic game visual effects: Fire, Explosions, and Magical Projectile Trails.",
                        "Particle systems: 1) Explosion (burst emission, velocity over lifetime, size curve, sub-emitter sparks), 2) Campfire (turbulent noise, color over lifetime gradient), 3) Magic projectile ribbon trail.",
                        "High-impact particle effects ready for integration with game actions and spells.",
                        "Project Difficulty: Easy | Tech: Particle Systems, Niagara, Visual Effects, Shaders",
                        List.of(
                                new TestCase("Trigger explosion effect", "Instantly emits fiery burst, shockwave ring, and flying spark debris", true, "VFX triggered"),
                                new TestCase("Inspect campfire particle", "Flame particles swirl upward with organic curl noise turbulence", false),
                                new TestCase("Move magic projectile", "Leaves smooth luminous ribbon trail following projectile path", false)
                        ),
                        "Use GPU particle emitters when simulating large quantities (> 1,000) of particles."
                ),
                new DsaProblem(
                        "AV-103",
                        topic,
                        topicTitle,
                        "C. Interactive Water & Ripple Shader",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Develop an interactive stylized water surface shader with depth fading and surface ripples.",
                        "Water shader: vertex displacement using scrolling Gerstner wave equations, depth fade transparency based on camera depth texture, Fresnel edge foam, and dynamic ripple normal map distortion.",
                        "Stylized water shader with realistic shoreline foam and vertex wave motion.",
                        "Project Difficulty: Medium | Tech: Shader Graph / HLSL, Gerstner Waves, Depth Fade, Normal Maps",
                        List.of(
                                new TestCase("Submerge object near shoreline", "Water depth fade creates transparent gradient with shoreline foam", true, "Water shader active"),
                                new TestCase("Inspect ocean surface", "Gerstner wave math creates natural rolling peaks and valleys", false),
                                new TestCase("Shoot water surface", "Spawns concentric expanding ripple ring normal distortion", false)
                        ),
                        "Sample the scene depth buffer (Scene Depth) to calculate water transparency near geometry."
                ),
                new DsaProblem(
                        "AV-104",
                        topic,
                        topicTitle,
                        "D. Screen-Space Visual Effects: Glitch & Damage Vignette",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Implement screen-space post-processing shaders for camera glitch and damage feedback.",
                        "Post-process effects: 1) Digital chromatic aberration glitch shader (RGB split + horizontal slice displacement), 2) Pulsing low-health red vignette with heartbeat audio synchronization.",
                        "Dramatic screen-space post-processing feedback heightening player immersion.",
                        "Project Difficulty: Medium | Tech: Post-Processing Shaders, Fullscreen Passes, Glitch FX",
                        List.of(
                                new TestCase("Trigger glitch effect on EMP attack", "Screen splits into RGB chromatic fringe with horizontal displacement", true, "Glitch FX active"),
                                new TestCase("Player health drops below 20%", "Pulsing blood-red vignette fades in synchronized with heartbeat SFX", false),
                                new TestCase("Inspect rendering performance", "Fullscreen blit pass executes in under 0.5ms on modern GPU", false)
                        ),
                        "Use a custom Render Pass in URP or Post-Process Material in UE5 for fullscreen shader effects."
                ),
                new DsaProblem(
                        "AV-105",
                        topic,
                        topicTitle,
                        "E. Complete Game Juice & Combat Feedback Architecture",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Architect a comprehensive 'Game Juice' feedback system orchestrating all audiovisual impact.",
                        "Feedback controller: coordinate simultaneous impact effects on hit: 1) Frame freeze / Hitstop (3 frames), 2) Directional camera shake, 3) Particle burst, 4) Floating damage text with DOTween animation, 5) Punchy audio SFX.",
                        "Visually visceral combat feedback system maximizing tactile 'game feel' and player satisfaction.",
                        "Project Difficulty: Hard | Tech: Game Feel, Hitstop, Camera Shake, Damage Text, Audiovisual Polish",
                        List.of(
                                new TestCase("Land heavy sword attack on enemy", "Hitstop pauses frame for 50ms; camera shakes; sparks fly; damage text pops", true, "Game juice verified"),
                                new TestCase("Inspect enemy reaction", "Material flashes white for 1 frame (hit flash) while flinching", false),
                                new TestCase("Defeat final boss enemy", "Triggers dynamic slow-motion (0.2x time scale) with bass drop audio filter", false)
                        ),
                        "Hitstop (pausing animations for a couple frames on impact) dramatically increases perceived attack weight."
                )
        );
    }

    // =========================================================================
    // GAME DEVELOPMENT & GRAPHICS - Game Optimization, Profiling & Store Launch (5 Exercises)
    // =========================================================================
    private static List<DsaProblem> buildGamePublishProblems() {
        String topic = "game_publish";
        String topicTitle = "Game Optimization, Profiling & Store Launch";

        return List.of(
                new DsaProblem(
                        "GP2-101",
                        topic,
                        topicTitle,
                        "A. Game Profiling & Frame Rate Bottleneck Analysis",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Profile a game project to diagnose CPU vs GPU bottlenecks and memory allocations.",
                        "Profiling workflow: use Unity Profiler / Unreal Insights to inspect CPU main thread spikes (scripts, physics), GPU render times (shadows, overdraw), Garbage Collection allocations (GC.Alloc), and target stable 60 FPS.",
                        "Comprehensive performance audit report isolating script bottlenecks and memory leaks.",
                        "Project Difficulty: Easy | Tech: Game Profiling, Unity Profiler, Unreal Insights, Optimization",
                        List.of(
                                new TestCase("Profile dense gameplay scene", "Identifies specific C# script causing 8ms CPU spike per frame", true, "Bottleneck diagnosed"),
                                new TestCase("Inspect Memory allocations", "Flags string concatenations in Update() generating continuous GC garbage", false),
                                new TestCase("Analyze Frame Debugger", "Inspects render passes and identifies redundant shadow map redraws", false)
                        ),
                        "Zero GC.Alloc in gameplay loops is the gold standard for avoiding hitching and stutter."
                ),
                new DsaProblem(
                        "GP2-102",
                        topic,
                        topicTitle,
                        "B. Draw Call Reduction: Batching, GPU Instancing & Atlasing",
                        Difficulty.EASY,
                        2000,
                        256,
                        "Optimize rendering draw calls by implementing static batching, GPU instancing, and texture atlasing.",
                        "Optimization pipeline: 1) Combine material textures into Sprite Atlas / Texture Atlas, 2) Enable GPU Instancing on shared mesh materials, 3) Configure Static Batching for environment props. Reduce draw calls by 70%.",
                        "Optimized render pipeline reducing draw calls from 1,000+ to under 150 per frame.",
                        "Project Difficulty: Easy | Tech: Draw Call Batching, GPU Instancing, Texture Atlases, Rendering",
                        List.of(
                                new TestCase("Enable GPU Instancing on 500 foliage meshes", "Draw calls drop from 500 to 4 batches", true, "GPU instancing active"),
                                new TestCase("Pack 30 UI textures into Sprite Atlas", "UI rendering draws in single batch with zero texture swapping", false),
                                new TestCase("Inspect Frame Debugger before vs after", "Total scene draw calls drop by over 75%", false)
                        ),
                        "GPU instancing requires identical mesh geometry and material shaders across rendered instances."
                ),
                new DsaProblem(
                        "GP2-103",
                        topic,
                        topicTitle,
                        "C. Memory Management, AssetBundles & Addressables",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Implement asynchronous asset loading and memory management using Addressables.",
                        "Addressables architecture: mark game assets as Addressable, load assets asynchronously on demand with Addressables.LoadAssetAsync<T>(), release memory when leaving levels, and manage downloadable content (DLC).",
                        "Scalable asset management system keeping app install size and runtime RAM usage minimal.",
                        "Project Difficulty: Medium | Tech: Unity Addressables, AssetBundles, Memory Management, Async Loading",
                        List.of(
                                new TestCase("Request enemy prefab via Addressables", "Loads asset asynchronously in background with progress callback", true, "Addressables loaded"),
                                new TestCase("Transition to next level", "Addressables.Release() clears unused assets from memory completely", false),
                                new TestCase("Inspect app install size", "Heavy audio and video assets separated into remote downloadable bundles", false)
                        ),
                        "Always release loaded Addressable handles to prevent memory leaks in long gameplay sessions."
                ),
                new DsaProblem(
                        "GP2-104",
                        topic,
                        topicTitle,
                        "D. Steamworks API Integration: Achievements & Cloud Saves",
                        Difficulty.MEDIUM,
                        2000,
                        256,
                        "Integrate the Steamworks SDK for Steam Achievements, Leaderboards, and Steam Cloud saves.",
                        "Steamworks integration (Steamworks.NET / Facepunch.Steamworks): 1) Initialize SteamAPI, 2) Unlock Steam Achievements on milestones, 3) Download and upload Leaderboard high scores, 4) Synchronize save games with Steam Cloud.",
                        "Full Steamworks integration providing seamless achievements and cloud saves for Steam players.",
                        "Project Difficulty: Medium | Tech: Steamworks API, Steam Achievements, Cloud Saves, Leaderboards",
                        List.of(
                                new TestCase("Defeat first boss in game", "Steam overlay displays achievement unlocked notification popup", true, "Achievement unlocked"),
                                new TestCase("Submit high score to Leaderboard", "Uploads score and retrieves top 10 global player rankings", false),
                                new TestCase("Launch game on secondary PC", "Steam Cloud automatically downloads and loads latest save file", false)
                        ),
                        "Call SteamAPI.RunCallbacks() once per frame in Update() to process Steam events."
                ),
                new DsaProblem(
                        "GP2-105",
                        topic,
                        topicTitle,
                        "E. Multi-Platform Build Pipeline & Store Launch Checklist",
                        Difficulty.HARD,
                        2000,
                        256,
                        "Configure automated multi-platform game builds and author store release packages.",
                        "Build pipeline: 1) Automated headless builds for Windows, macOS, Linux, and Mobile using CLI, 2) IL2CPP AOT compilation with bytecode stripping, 3) SteamPipe depot upload script, 4) Game store marketing materials & trailers.",
                        "Complete automated build and store deployment pipeline ready for commercial game launch.",
                        "Project Difficulty: Hard | Tech: Game Build Pipeline, IL2CPP, SteamPipe, CI/CD, Game Publishing",
                        List.of(
                                new TestCase("Execute automated build script", "Compiles optimized standalone 64-bit binaries across platforms", true, "Builds completed"),
                                new TestCase("Run SteamPipe build script", "Uploads content depots and sets live build branch on Steam backend", false),
                                new TestCase("Inspect game launch checklist", "All store capsules, screenshots, ESRB ratings, and privacy policies verified", false)
                        ),
                        "Use IL2CPP compilation for production mobile and console builds for maximum execution performance."
                )
        );
    }

}
