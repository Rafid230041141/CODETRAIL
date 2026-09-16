package application.client.dsa.judge;

import java.util.*;

/**
 * Intelligent multi-language code completion engine for the CodeTrail DSA Arena.
 * Provides curated keywords, types, standard library functions, I/O streams, and snippets
 * for all 6 supported competitive programming languages.
 *
 * Guarantees that typing "cou" suggests "cout" for all programming languages.
 */
public final class CodeCompletionEngine {

    public record CompletionItem(
            String label,
            String insertText,
            String category, // "I/O", "KEY", "TYPE", "FUNC", "SNIP", "CONST"
            String description
    ) {
        @Override
        public String toString() {
            return label;
        }
    }

    private static final Map<ProgrammingLanguage, List<CompletionItem>> DICTIONARIES = new EnumMap<>(ProgrammingLanguage.class);

    static {
        initCpp();
        initC();
        initPython();
        initJava();
        initCSharp();
        initJavaScript();
    }

    private CodeCompletionEngine() {}

    private static void initCpp() {
        List<CompletionItem> list = new ArrayList<>();
        // I/O & Streams
        list.add(new CompletionItem("cout", "cout << ", "I/O", "Standard character output stream"));
        list.add(new CompletionItem("cin", "cin >> ", "I/O", "Standard character input stream"));
        list.add(new CompletionItem("endl", "endl", "I/O", "End line and flush output stream"));
        list.add(new CompletionItem("ios_base", "ios_base::sync_with_stdio(false); cin.tie(NULL);", "SNIP", "Fast C++ I/O optimization"));
        
        // STL Containers & Types
        list.add(new CompletionItem("vector", "vector<int> ", "TYPE", "Dynamic sequential array"));
        list.add(new CompletionItem("string", "string ", "TYPE", "Standard string class"));
        list.add(new CompletionItem("pair", "pair<int, int> ", "TYPE", "Pair of heterogeneous objects"));
        list.add(new CompletionItem("map", "map<", "TYPE", "Red-black tree key-value map"));
        list.add(new CompletionItem("unordered_map", "unordered_map<", "TYPE", "Hash-table key-value map"));
        list.add(new CompletionItem("set", "set<", "TYPE", "Sorted unique elements collection"));
        list.add(new CompletionItem("unordered_set", "unordered_set<", "TYPE", "Hash-table unique elements set"));
        list.add(new CompletionItem("queue", "queue<int> ", "TYPE", "FIFO queue"));
        list.add(new CompletionItem("deque", "deque<int> ", "TYPE", "Double-ended queue"));
        list.add(new CompletionItem("stack", "stack<int> ", "TYPE", "LIFO stack"));
        list.add(new CompletionItem("priority_queue", "priority_queue<int> ", "TYPE", "Max-heap priority queue"));
        list.add(new CompletionItem("priority_queue_min", "priority_queue<int, vector<int>, greater<int>> ", "SNIP", "Min-heap priority queue"));

        // Algorithms & Functions
        list.add(new CompletionItem("sort", "sort(v.begin(), v.end());", "FUNC", "Sort elements in range"));
        list.add(new CompletionItem("reverse", "reverse(v.begin(), v.end());", "FUNC", "Reverse elements in range"));
        list.add(new CompletionItem("count", "count(v.begin(), v.end(), val);", "FUNC", "Count occurrences in range"));
        list.add(new CompletionItem("push_back", "push_back(", "FUNC", "Add element to end of vector"));
        list.add(new CompletionItem("pop_back", "pop_back()", "FUNC", "Remove last element"));
        list.add(new CompletionItem("make_pair", "make_pair(", "FUNC", "Construct a pair"));
        list.add(new CompletionItem("lower_bound", "lower_bound(v.begin(), v.end(), val);", "FUNC", "Binary search first element >= val"));
        list.add(new CompletionItem("upper_bound", "upper_bound(v.begin(), v.end(), val);", "FUNC", "Binary search first element > val"));
        list.add(new CompletionItem("binary_search", "binary_search(v.begin(), v.end(), val);", "FUNC", "Test element existence in sorted range"));
        list.add(new CompletionItem("accumulate", "accumulate(v.begin(), v.end(), 0LL);", "FUNC", "Sum elements in range"));
        list.add(new CompletionItem("max", "max(", "FUNC", "Maximum of two values"));
        list.add(new CompletionItem("min", "min(", "FUNC", "Minimum of two values"));
        list.add(new CompletionItem("swap", "swap(", "FUNC", "Swap two values"));

        // Keywords & Control Flow
        list.add(new CompletionItem("include", "#include <iostream>\n", "KEY", "Precompiled header inclusion"));
        list.add(new CompletionItem("using", "using namespace std;\n", "KEY", "Import standard namespace"));
        list.add(new CompletionItem("int", "int ", "TYPE", "32-bit signed integer"));
        list.add(new CompletionItem("long long", "long long ", "TYPE", "64-bit signed integer"));
        list.add(new CompletionItem("double", "double ", "TYPE", "Double precision float"));
        list.add(new CompletionItem("bool", "bool ", "TYPE", "Boolean type"));
        list.add(new CompletionItem("void", "void ", "TYPE", "Empty return type"));
        list.add(new CompletionItem("auto", "auto ", "KEY", "Automatic type deduction"));
        list.add(new CompletionItem("const", "const ", "KEY", "Constant qualifier"));
        list.add(new CompletionItem("for", "for (int i = 0; i < n; i++) {\n    \n}", "SNIP", "Indexed for loop"));
        list.add(new CompletionItem("while", "while () {\n    \n}", "SNIP", "While loop"));
        list.add(new CompletionItem("if", "if () {\n    \n}", "SNIP", "If conditional block"));
        list.add(new CompletionItem("else", "else {\n    \n}", "SNIP", "Else conditional block"));
        list.add(new CompletionItem("return", "return ", "KEY", "Return value from function"));

        // Constants
        list.add(new CompletionItem("INT_MAX", "INT_MAX", "CONST", "Maximum 32-bit int (2147483647)"));
        list.add(new CompletionItem("INT_MIN", "INT_MIN", "CONST", "Minimum 32-bit int (-2147483648)"));
        list.add(new CompletionItem("LLONG_MAX", "LLONG_MAX", "CONST", "Maximum 64-bit int"));
        list.add(new CompletionItem("LLONG_MIN", "LLONG_MIN", "CONST", "Minimum 64-bit int"));

        DICTIONARIES.put(ProgrammingLanguage.CPP, list);
    }

    private static void initC() {
        List<CompletionItem> list = new ArrayList<>();
        // Cross-language I/O: typing "cou" suggests "cout"
        list.add(new CompletionItem("cout", "printf(\"", "I/O", "Print to stdout (C equivalent of C++ cout)"));
        list.add(new CompletionItem("count", "int count = 0;", "SNIP", "Counter variable declaration"));

        // Standard C I/O
        list.add(new CompletionItem("printf", "printf(\"%d\\n\", );", "I/O", "Formatted print to stdout"));
        list.add(new CompletionItem("scanf", "scanf(\"%d\", &);", "I/O", "Formatted read from stdin"));
        list.add(new CompletionItem("include", "#include <stdio.h>\n#include <stdlib.h>\n#include <string.h>\n", "KEY", "Include standard headers"));
        
        // Memory & Functions
        list.add(new CompletionItem("malloc", "malloc(", "FUNC", "Allocate dynamic memory block"));
        list.add(new CompletionItem("calloc", "calloc(", "FUNC", "Allocate zero-initialized memory"));
        list.add(new CompletionItem("free", "free(", "FUNC", "Deallocate dynamic memory"));
        list.add(new CompletionItem("sizeof", "sizeof(", "KEY", "Size of expression/type in bytes"));
        list.add(new CompletionItem("strlen", "strlen(", "FUNC", "Length of null-terminated string"));
        list.add(new CompletionItem("strcmp", "strcmp(", "FUNC", "Compare two strings"));
        list.add(new CompletionItem("strcpy", "strcpy(", "FUNC", "Copy string"));
        list.add(new CompletionItem("memset", "memset(", "FUNC", "Fill memory block with byte"));
        list.add(new CompletionItem("memcpy", "memcpy(", "FUNC", "Copy memory block"));
        list.add(new CompletionItem("qsort", "qsort(arr, n, sizeof(int), cmp);", "FUNC", "Standard quicksort"));

        // Types & Keywords
        list.add(new CompletionItem("int", "int ", "TYPE", "32-bit signed integer"));
        list.add(new CompletionItem("long", "long long ", "TYPE", "64-bit signed integer"));
        list.add(new CompletionItem("char", "char ", "TYPE", "Character type"));
        list.add(new CompletionItem("void", "void ", "TYPE", "Void type"));
        list.add(new CompletionItem("double", "double ", "TYPE", "Double precision float"));
        list.add(new CompletionItem("struct", "struct ", "KEY", "Structure declaration"));
        list.add(new CompletionItem("typedef", "typedef ", "KEY", "Type definition alias"));
        list.add(new CompletionItem("for", "for (int i = 0; i < n; i++) {\n    \n}", "SNIP", "Indexed for loop"));
        list.add(new CompletionItem("while", "while () {\n    \n}", "SNIP", "While loop"));
        list.add(new CompletionItem("if", "if () {\n    \n}", "SNIP", "If conditional block"));
        list.add(new CompletionItem("else", "else {\n    \n}", "SNIP", "Else block"));
        list.add(new CompletionItem("return", "return 0;", "KEY", "Return statement"));
        list.add(new CompletionItem("NULL", "NULL", "CONST", "Null pointer constant"));

        DICTIONARIES.put(ProgrammingLanguage.C, list);
    }

    private static void initPython() {
        List<CompletionItem> list = new ArrayList<>();
        // Cross-language I/O: typing "cou" suggests "cout"
        list.add(new CompletionItem("cout", "print(", "I/O", "Print to stdout (Python equivalent of C++ cout)"));
        list.add(new CompletionItem("count", "count = 0", "SNIP", "Counter variable declaration"));

        // Standard I/O & Functions
        list.add(new CompletionItem("print", "print(", "I/O", "Print output to stdout"));
        list.add(new CompletionItem("input", "input()", "I/O", "Read string line from stdin"));
        list.add(new CompletionItem("def", "def solve():\n    ", "SNIP", "Function definition"));
        list.add(new CompletionItem("range", "range(", "FUNC", "Generate sequence of numbers"));
        list.add(new CompletionItem("len", "len(", "FUNC", "Length of sequence or collection"));
        list.add(new CompletionItem("append", "append(", "FUNC", "Append item to list"));
        list.add(new CompletionItem("pop", "pop()", "FUNC", "Remove and return item"));
        list.add(new CompletionItem("split", "split()", "FUNC", "Split string into list of tokens"));
        list.add(new CompletionItem("join", "' '.join(", "FUNC", "Concatenate list of strings"));
        list.add(new CompletionItem("sort", "sort()", "FUNC", "Sort list in-place"));
        list.add(new CompletionItem("sorted", "sorted(", "FUNC", "Return new sorted list"));
        list.add(new CompletionItem("map", "map(int, input().split())", "SNIP", "Fast multiple integer parser"));
        list.add(new CompletionItem("enumerate", "enumerate(", "FUNC", "Iterate with (index, value) pairs"));
        list.add(new CompletionItem("zip", "zip(", "FUNC", "Combine iterables pairwise"));
        list.add(new CompletionItem("max", "max(", "FUNC", "Maximum element"));
        list.add(new CompletionItem("min", "min(", "FUNC", "Minimum element"));
        list.add(new CompletionItem("sum", "sum(", "FUNC", "Sum of elements in iterable"));
        list.add(new CompletionItem("abs", "abs(", "FUNC", "Absolute value"));

        // Modules & Imports
        list.add(new CompletionItem("sys", "import sys\ninput = sys.stdin.readline", "SNIP", "Fast stdin input import"));
        list.add(new CompletionItem("collections", "from collections import deque, Counter, defaultdict", "SNIP", "Specialized container datatypes"));
        list.add(new CompletionItem("heapq", "import heapq\n# heapq.heappush(h, val); heapq.heappop(h)", "SNIP", "Heap queue algorithm"));
        list.add(new CompletionItem("math", "import math", "KEY", "Mathematical functions module"));

        // Keywords & Flow
        list.add(new CompletionItem("for", "for i in range(n):\n    ", "SNIP", "For loop with range"));
        list.add(new CompletionItem("while", "while :\n    ", "SNIP", "While loop"));
        list.add(new CompletionItem("if", "if :\n    ", "KEY", "If condition"));
        list.add(new CompletionItem("elif", "elif :\n    ", "KEY", "Else-if condition"));
        list.add(new CompletionItem("else", "else:\n    ", "KEY", "Else block"));
        list.add(new CompletionItem("return", "return ", "KEY", "Return from function"));
        list.add(new CompletionItem("True", "True", "CONST", "Boolean true literal"));
        list.add(new CompletionItem("False", "False", "CONST", "Boolean false literal"));
        list.add(new CompletionItem("None", "None", "CONST", "None singleton object"));
        list.add(new CompletionItem("lambda", "lambda x: ", "KEY", "Anonymous inline function"));

        DICTIONARIES.put(ProgrammingLanguage.PYTHON, list);
    }

    private static void initJava() {
        List<CompletionItem> list = new ArrayList<>();
        // Cross-language I/O: typing "cou" suggests "cout"
        list.add(new CompletionItem("cout", "System.out.println(", "I/O", "Print to stdout (Java equivalent of C++ cout)"));
        list.add(new CompletionItem("count", "int count = 0;", "SNIP", "Counter variable declaration"));

        // Java I/O
        list.add(new CompletionItem("System.out.println", "System.out.println(", "I/O", "Print line to stdout"));
        list.add(new CompletionItem("System.out.print", "System.out.print(", "I/O", "Print without newline to stdout"));
        list.add(new CompletionItem("Scanner", "Scanner sc = new Scanner(System.in);", "SNIP", "Scanner for stdin input"));
        list.add(new CompletionItem("BufferedReader", "BufferedReader br = new BufferedReader(new InputStreamReader(System.in));", "SNIP", "Fast buffered character reader"));
        list.add(new CompletionItem("StringTokenizer", "StringTokenizer st = new StringTokenizer(br.readLine());", "SNIP", "Fast string tokenizer"));
        list.add(new CompletionItem("StringBuilder", "StringBuilder sb = new StringBuilder();", "SNIP", "Efficient mutable character sequence"));

        // Collections & Types
        list.add(new CompletionItem("List", "List<Integer> list = new ArrayList<>();", "SNIP", "Ordered sequence collection"));
        list.add(new CompletionItem("ArrayList", "ArrayList<Integer> ", "TYPE", "Resizable array implementation"));
        list.add(new CompletionItem("Map", "Map<Integer, Integer> map = new HashMap<>();", "SNIP", "Key-value mapping collection"));
        list.add(new CompletionItem("HashMap", "HashMap<Integer, Integer> ", "TYPE", "Hash-table based map"));
        list.add(new CompletionItem("Set", "Set<Integer> set = new HashSet<>();", "SNIP", "Unique element set"));
        list.add(new CompletionItem("HashSet", "HashSet<Integer> ", "TYPE", "Hash-table based set"));
        list.add(new CompletionItem("Queue", "Queue<Integer> q = new LinkedList<>();", "SNIP", "FIFO queue collection"));
        list.add(new CompletionItem("PriorityQueue", "PriorityQueue<Integer> pq = new PriorityQueue<>();", "SNIP", "Min-heap priority queue"));
        list.add(new CompletionItem("Stack", "Stack<Integer> st = new Stack<>();", "SNIP", "LIFO stack collection"));

        // Methods & Utilities
        list.add(new CompletionItem("Arrays.sort", "Arrays.sort(", "FUNC", "Sort array elements"));
        list.add(new CompletionItem("Collections.sort", "Collections.sort(", "FUNC", "Sort list elements"));
        list.add(new CompletionItem("Math.max", "Math.max(", "FUNC", "Maximum of two numbers"));
        list.add(new CompletionItem("Math.min", "Math.min(", "FUNC", "Minimum of two numbers"));
        list.add(new CompletionItem("Math.abs", "Math.abs(", "FUNC", "Absolute value"));
        list.add(new CompletionItem("Integer.parseInt", "Integer.parseInt(", "FUNC", "Parse string to int"));
        list.add(new CompletionItem("Long.parseLong", "Long.parseLong(", "FUNC", "Parse string to long"));

        // Keywords & Control Flow
        list.add(new CompletionItem("public", "public ", "KEY", "Public access modifier"));
        list.add(new CompletionItem("private", "private ", "KEY", "Private access modifier"));
        list.add(new CompletionItem("static", "static ", "KEY", "Static member modifier"));
        list.add(new CompletionItem("final", "final ", "KEY", "Final non-modifiable modifier"));
        list.add(new CompletionItem("void", "void ", "TYPE", "Void return type"));
        list.add(new CompletionItem("int", "int ", "TYPE", "32-bit signed integer primitive"));
        list.add(new CompletionItem("long", "long ", "TYPE", "64-bit signed integer primitive"));
        list.add(new CompletionItem("String", "String ", "TYPE", "Immutable string object"));
        list.add(new CompletionItem("for", "for (int i = 0; i < n; i++) {\n    \n}", "SNIP", "Indexed for loop"));
        list.add(new CompletionItem("while", "while () {\n    \n}", "SNIP", "While loop"));
        list.add(new CompletionItem("if", "if () {\n    \n}", "SNIP", "If conditional block"));
        list.add(new CompletionItem("else", "else {\n    \n}", "SNIP", "Else conditional block"));
        list.add(new CompletionItem("return", "return ", "KEY", "Return statement"));

        DICTIONARIES.put(ProgrammingLanguage.JAVA, list);
    }

    private static void initCSharp() {
        List<CompletionItem> list = new ArrayList<>();
        // Cross-language I/O: typing "cou" suggests "cout"
        list.add(new CompletionItem("cout", "Console.WriteLine(", "I/O", "Print to stdout (C# equivalent of C++ cout)"));
        list.add(new CompletionItem("Count", "Count", "TYPE", "Collection item count property"));

        // C# I/O
        list.add(new CompletionItem("Console.WriteLine", "Console.WriteLine(", "I/O", "Write line to standard output"));
        list.add(new CompletionItem("Console.ReadLine", "Console.ReadLine()", "I/O", "Read line from standard input"));
        list.add(new CompletionItem("Console.Write", "Console.Write(", "I/O", "Write without newline to stdout"));

        // Collections & Types
        list.add(new CompletionItem("List", "List<int> list = new List<int>();", "SNIP", "Generic resizable list"));
        list.add(new CompletionItem("Dictionary", "Dictionary<int, int> dict = new Dictionary<int, int>();", "SNIP", "Generic key-value dictionary"));
        list.add(new CompletionItem("HashSet", "HashSet<int> set = new HashSet<int>();", "SNIP", "Generic unique hash set"));
        list.add(new CompletionItem("Queue", "Queue<int> q = new Queue<int>();", "SNIP", "Generic FIFO queue"));
        list.add(new CompletionItem("Stack", "Stack<int> s = new Stack<int>();", "SNIP", "Generic LIFO stack"));

        // Functions
        list.add(new CompletionItem("Array.Sort", "Array.Sort(", "FUNC", "Sort elements in one-dimensional array"));
        list.add(new CompletionItem("Math.Max", "Math.Max(", "FUNC", "Maximum of two values"));
        list.add(new CompletionItem("Math.Min", "Math.Min(", "FUNC", "Minimum of two values"));
        list.add(new CompletionItem("Math.Abs", "Math.Abs(", "FUNC", "Absolute value"));
        list.add(new CompletionItem("int.Parse", "int.Parse(", "FUNC", "Parse string to 32-bit int"));
        list.add(new CompletionItem("long.Parse", "long.Parse(", "FUNC", "Parse string to 64-bit int"));

        // Keywords & Control Flow
        list.add(new CompletionItem("using", "using System;\nusing System.Collections.Generic;\n", "KEY", "Import namespace directive"));
        list.add(new CompletionItem("namespace", "namespace Solution {\n    \n}", "SNIP", "Namespace declaration"));
        list.add(new CompletionItem("class", "class Program {\n    \n}", "SNIP", "Class declaration"));
        list.add(new CompletionItem("var", "var ", "KEY", "Implicitly typed local variable"));
        list.add(new CompletionItem("int", "int ", "TYPE", "32-bit signed integer"));
        list.add(new CompletionItem("long", "long ", "TYPE", "64-bit signed integer"));
        list.add(new CompletionItem("string", "string ", "TYPE", "UTF-16 string sequence"));
        list.add(new CompletionItem("bool", "bool ", "TYPE", "Boolean true/false type"));
        list.add(new CompletionItem("for", "for (int i = 0; i < n; i++) {\n    \n}", "SNIP", "Indexed for loop"));
        list.add(new CompletionItem("foreach", "foreach (var item in collection) {\n    \n}", "SNIP", "Foreach iteration loop"));
        list.add(new CompletionItem("while", "while () {\n    \n}", "SNIP", "While loop"));
        list.add(new CompletionItem("if", "if () {\n    \n}", "SNIP", "If conditional block"));
        list.add(new CompletionItem("else", "else {\n    \n}", "SNIP", "Else block"));
        list.add(new CompletionItem("return", "return ", "KEY", "Return statement"));

        DICTIONARIES.put(ProgrammingLanguage.CSHARP, list);
    }

    private static void initJavaScript() {
        List<CompletionItem> list = new ArrayList<>();
        // Cross-language I/O: typing "cou" suggests "cout"
        list.add(new CompletionItem("cout", "console.log(", "I/O", "Print to stdout (JS equivalent of C++ cout)"));
        list.add(new CompletionItem("count", "let count = 0;", "SNIP", "Counter variable declaration"));

        // JS I/O
        list.add(new CompletionItem("console.log", "console.log(", "I/O", "Print output to standard log"));
        list.add(new CompletionItem("console.error", "console.error(", "I/O", "Print output to standard error"));

        // Keywords & Declarations
        list.add(new CompletionItem("const", "const ", "KEY", "Block-scoped immutable binding"));
        list.add(new CompletionItem("let", "let ", "KEY", "Block-scoped mutable variable"));
        list.add(new CompletionItem("var", "var ", "KEY", "Function-scoped variable"));
        list.add(new CompletionItem("function", "function solve() {\n    \n}", "SNIP", "Function declaration"));
        list.add(new CompletionItem("return", "return ", "KEY", "Return value from function"));

        // Loops & Control Flow
        list.add(new CompletionItem("for", "for (let i = 0; i < n; i++) {\n    \n}", "SNIP", "Indexed for loop"));
        list.add(new CompletionItem("for_of", "for (const item of arr) {\n    \n}", "SNIP", "Iterate elements of iterable"));
        list.add(new CompletionItem("while", "while () {\n    \n}", "SNIP", "While loop"));
        list.add(new CompletionItem("if", "if () {\n    \n}", "SNIP", "If conditional block"));
        list.add(new CompletionItem("else", "else {\n    \n}", "SNIP", "Else block"));

        // Array & Object Methods
        list.add(new CompletionItem("push", "push(", "FUNC", "Append elements to array"));
        list.add(new CompletionItem("pop", "pop()", "FUNC", "Remove and return last element"));
        list.add(new CompletionItem("shift", "shift()", "FUNC", "Remove and return first element"));
        list.add(new CompletionItem("unshift", "unshift(", "FUNC", "Prepend elements to array"));
        list.add(new CompletionItem("map", "map(x => )", "FUNC", "Transform array elements"));
        list.add(new CompletionItem("filter", "filter(x => )", "FUNC", "Filter array elements by predicate"));
        list.add(new CompletionItem("reduce", "reduce((acc, x) => acc + x, 0)", "FUNC", "Reduce array to single value"));
        list.add(new CompletionItem("sort", "sort((a, b) => a - b)", "SNIP", "Ascending numerical sort"));
        list.add(new CompletionItem("split", "split(' ')", "FUNC", "Split string into substrings"));
        list.add(new CompletionItem("join", "join(' ')", "FUNC", "Join array elements into string"));
        list.add(new CompletionItem("slice", "slice(", "FUNC", "Extract portion of array or string"));
        list.add(new CompletionItem("parseInt", "parseInt(", "FUNC", "Parse string to integer"));
        list.add(new CompletionItem("parseFloat", "parseFloat(", "FUNC", "Parse string to float"));
        list.add(new CompletionItem("Math.max", "Math.max(...arr)", "FUNC", "Maximum value in numbers"));
        list.add(new CompletionItem("Math.min", "Math.min(...arr)", "FUNC", "Minimum value in numbers"));
        list.add(new CompletionItem("Math.abs", "Math.abs(", "FUNC", "Absolute value"));
        list.add(new CompletionItem("Math.floor", "Math.floor(", "FUNC", "Round down to integer"));

        DICTIONARIES.put(ProgrammingLanguage.JAVASCRIPT, list);
    }

    /**
     * Retrieves autocomplete suggestions for the given language and typing prefix.
     * Matches prefix case-insensitively, prioritizing exact/prefix matches over substring matches.
     * Guarantees "cout" is suggested when typing "cou" across ALL languages.
     */
    public static List<CompletionItem> getSuggestions(ProgrammingLanguage language, String prefix) {
        if (language == null) language = ProgrammingLanguage.CPP;
        List<CompletionItem> dictionary = DICTIONARIES.getOrDefault(language, Collections.emptyList());
        
        if (prefix == null || prefix.trim().isEmpty()) {
            // Return top suggestions
            return new ArrayList<>(dictionary.subList(0, Math.min(10, dictionary.size())));
        }

        String query = prefix.trim().toLowerCase(Locale.ROOT);

        List<CompletionItem> exactMatches = new ArrayList<>();
        List<CompletionItem> prefixMatches = new ArrayList<>();
        List<CompletionItem> containsMatches = new ArrayList<>();

        for (CompletionItem item : dictionary) {
            String lowerLabel = item.label().toLowerCase(Locale.ROOT);
            if (lowerLabel.equals(query)) {
                exactMatches.add(item);
            } else if (lowerLabel.startsWith(query)) {
                prefixMatches.add(item);
            } else if (lowerLabel.contains(query)) {
                containsMatches.add(item);
            }
        }

        // Always guarantee "cout" is included if typing starts with "cou"
        if ("cout".startsWith(query) && exactMatches.stream().noneMatch(i -> i.label().equalsIgnoreCase("cout"))
                && prefixMatches.stream().noneMatch(i -> i.label().equalsIgnoreCase("cout"))) {
            CompletionItem coutItem = switch (language) {
                case CPP -> new CompletionItem("cout", "cout << ", "I/O", "Standard character output stream");
                case C -> new CompletionItem("cout", "printf(\"", "I/O", "Print to stdout (C equivalent of C++ cout)");
                case PYTHON -> new CompletionItem("cout", "print(", "I/O", "Print to stdout (Python equivalent of C++ cout)");
                case JAVA -> new CompletionItem("cout", "System.out.println(", "I/O", "Print to stdout (Java equivalent of C++ cout)");
                case CSHARP -> new CompletionItem("cout", "Console.WriteLine(", "I/O", "Print to stdout (C# equivalent of C++ cout)");
                case JAVASCRIPT -> new CompletionItem("cout", "console.log(", "I/O", "Print to stdout (JS equivalent of C++ cout)");
            };
            prefixMatches.add(0, coutItem);
        }

        List<CompletionItem> results = new ArrayList<>();
        results.addAll(exactMatches);
        results.addAll(prefixMatches);
        results.addAll(containsMatches);

        if (results.size() > 10) {
            return results.subList(0, 10);
        }
        return results;
    }
}
