package application.backend.seed;

import java.util.*;

public final class QuizBankSeeder {

    public record QuestionSpec(String prompt, String a, String b, String c, String d, int correct, String explanation) {}

    public static List<QuestionSpec> getQuestionsForLesson(String topic, String module, String submodule, String title) {
        String key = slug(title);
        List<QuestionSpec> specific = getSpecificQuestions(key, topic, module, title);
        if (specific != null && specific.size() >= 8) {
            return specific.subList(0, 8);
        }
        return buildFallbackQuestions(topic, module, submodule, title, specific);
    }

    private static String slug(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replace("c++", "cpp")
                .replace("c#", "csharp")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private static List<QuestionSpec> getSpecificQuestions(String key, String topic, String module, String title) {
        // Match specific lesson or module keywords
        String lowerMod = module == null ? "" : module.toLowerCase(Locale.ROOT);
        String lowerTitle = title == null ? "" : title.toLowerCase(Locale.ROOT);

        // 1. DATA STRUCTURES
        if (key.contains("array")) {
            return List.of(
                new QuestionSpec("What is the primary characteristic of an array in memory?",
                    "Contiguous memory allocation with constant-time index access", "Non-contiguous node pointers", "Hash-bucketed linked list", "Dynamic tree hierarchy", 0, "Arrays allocate elements in contiguous memory blocks, allowing direct O(1) index calculation."),
                new QuestionSpec("What is the time complexity of accessing an element in an array by its index?",
                    "O(1)", "O(n)", "O(log n)", "O(n²)", 0, "Array indexing uses base address + (index * element size) for O(1) random access."),
                new QuestionSpec("What is the worst-case time complexity of inserting an element at index 0 of an array of size n?",
                    "O(n)", "O(1)", "O(log n)", "O(n log n)", 0, "Inserting at index 0 requires shifting all n existing elements rightward by one position."),
                new QuestionSpec("In 0-indexed arrays of length n, what is the valid index range?",
                    "0 to n - 1", "1 to n", "0 to n", "-1 to n - 1", 0, "Zero-based indexing starts at offset 0 and ends at offset n - 1."),
                new QuestionSpec("What occurs when trying to access index n in an array of size n in Java/C++?",
                    "IndexOutOfBoundsException or undefined memory access", "Returns null", "Automatically resizes array", "Returns element 0", 0, "Accessing an index >= length violates bounds and triggers an out of bounds error."),
                new QuestionSpec("Which operation does a dynamic array perform when its underlying capacity is exceeded?",
                    "Allocates a new larger array (typically 1.5x or 2x) and copies elements", "Overwrites oldest elements", "Throws a fatal compile error", "Converts itself into a linked list", 0, "Dynamic arrays achieve O(1) amortized append by doubling capacity when full."),
                new QuestionSpec("What is the space complexity of allocating an array of n 32-bit integers?",
                    "O(n) - exactly 4n bytes", "O(1)", "O(n²)", "O(log n)", 0, "An array of n integers requires contiguous storage proportional to n elements."),
                new QuestionSpec("Which cache benefit makes arrays significantly faster than linked lists for sequential iteration?",
                    "High spatial locality and CPU cache prefetching", "Virtual thread scheduling", "Garbage collector pinning", "Hash table deduplication", 0, "Contiguous memory layout maximizes CPU L1/L2 cache line hits and hardware prefetching.")
            );
        }

        if (key.contains("link")) {
            return List.of(
                new QuestionSpec("What does each node in a singly linked list contain?",
                    "Data value and a pointer/reference to the next node", "Only an integer array index", "Two sibling nodes and a root", "Key-value hash pair", 0, "A singly linked node encapsulates its payload and a reference to the next node."),
                new QuestionSpec("What is the time complexity to insert a new node at the head of a linked list when given the head pointer?",
                    "O(1)", "O(n)", "O(log n)", "O(n²)", 0, "Head insertion only updates the new node next pointer and head reference, taking constant time."),
                new QuestionSpec("What is the time complexity to access the k-th element in a linked list?",
                    "O(k)", "O(1)", "O(log k)", "O(k²)", 0, "Linked lists do not support random access; traversal must step through nodes sequentially."),
                new QuestionSpec("What represents the end of a standard singly linked list?",
                    "A node pointing to null", "A node pointing back to head", "A sentinel value -1", "An empty array", 0, "The tail node terminates the list by having its next reference set to null."),
                new QuestionSpec("What additional pointer does each node maintain in a doubly linked list?",
                    "A pointer to the previous node", "A pointer to the root", "A pointer to the middle node", "A hash digest", 0, "Doubly linked lists store both next and previous pointers, allowing bidirectional traversal."),
                new QuestionSpec("Which classic two-pointer algorithm detects a cycle in a linked list in O(n) time and O(1) memory?",
                    "Floyd\'s Tortoise and Hare algorithm", "Dijkstra\'s algorithm", "Kruskal\'s algorithm", "Binary search", 0, "Floyd\'s cycle-finding algorithm uses a slow pointer (1 step) and fast pointer (2 steps)."),
                new QuestionSpec("What is a primary advantage of linked lists over fixed-size arrays?",
                    "Dynamic size without contiguous memory pre-allocation", "Faster cache locality", "O(1) random indexing", "Smaller per-element memory overhead", 0, "Linked lists grow dynamically node-by-node without needing pre-allocated memory contiguous blocks."),
                new QuestionSpec("What is the memory trade-off of a linked list compared to a raw array of numbers?",
                    "Extra pointer/reference overhead per node (4 or 8 bytes per link)", "Zero additional overhead", "Linked lists use half the memory", "Extra heap compression headers", 0, "Each node incurs memory overhead for reference pointers in addition to data storage.")
            );
        }

        if (key.contains("stack")) {
            return List.of(
                new QuestionSpec("What access order defines the Stack data structure?",
                    "LIFO (Last In, First Out)", "FIFO (First In, First Out)", "Random access", "Sorted order", 0, "Stacks operate strictly on a Last In, First Out principle."),
                new QuestionSpec("Which stack operation adds an element to the top?",
                    "push", "pop", "peek", "enqueue", 0, "Push places a new element onto the top of the stack."),
                new QuestionSpec("Which stack operation removes and returns the topmost element?",
                    "pop", "push", "peek", "dequeue", 0, "Pop removes and yields the topmost element."),
                new QuestionSpec("What is the time complexity of push, pop, and peek operations on a stack?",
                    "O(1)", "O(n)", "O(log n)", "O(n log n)", 0, "Stack top operations operate in constant O(1) time."),
                new QuestionSpec("What error occurs when attempting to pop from an empty stack?",
                    "Stack Underflow", "Stack Overflow", "Out of Memory", "Null Pointer Dereference", 0, "Attempting to retrieve from an empty stack causes a stack underflow."),
                new QuestionSpec("Which problem is canonically solved using a stack?",
                    "Matching balanced parentheses / delimiters", "Shortest path in weighted graph", "Sorting floating point numbers", "Bipartite graph coloring", 0, "Stacks naturally match opening and closing delimiters in nested structures."),
                new QuestionSpec("How does a call stack function during recursive function calls?",
                    "Pushes active stack frames on call and pops on return", "Queues calls in FIFO order", "Sorts frames by name", "Compresses local variables", 0, "The runtime call stack manages function activation frames in LIFO order."),
                new QuestionSpec("What data structure is used to implement a Monotonic Stack?",
                    "A stack maintaining elements in strictly increasing or decreasing order", "A binary heap", "A circular queue", "A hash set", 0, "Monotonic stacks maintain sorted invariant to solve Next Greater Element in O(n).")
            );
        }

        if (key.contains("queue")) {
            return List.of(
                new QuestionSpec("What access policy defines a standard Queue?",
                    "FIFO (First In, First Out)", "LIFO (Last In, First Out)", "Random priority", "Heuristic ranking", 0, "Queues process items in First In, First Out arrival order."),
                new QuestionSpec("Which operation inserts an item at the rear/back of a queue?",
                    "enqueue", "dequeue", "peek", "pop", 0, "Enqueue inserts an element at the tail/back of the queue."),
                new QuestionSpec("Which operation removes the front item from a queue?",
                    "dequeue", "enqueue", "push", "insert", 0, "Dequeue removes the item at the head/front of the queue."),
                new QuestionSpec("What is the time complexity of standard enqueue and dequeue in a circular array or linked queue?",
                    "O(1)", "O(n)", "O(log n)", "O(n²)", 0, "Both operations update boundary pointers in constant O(1) time."),
                new QuestionSpec("Which algorithm traversal relies centrally on a FIFO queue?",
                    "Breadth-First Search (BFS)", "Depth-First Search (DFS)", "Binary search", "QuickSort", 0, "BFS uses a FIFO queue to explore nodes level-by-level."),
                new QuestionSpec("Why is a circular queue buffer advantageous over a simple linear array queue?",
                    "Reuses dequeued space using modulo arithmetic without shifting elements", "Eliminates memory limits", "Sorts data automatically", "Provides O(log n) search", 0, "Circular buffers wrap around with (tail + 1) % capacity to avoid shifting elements."),
                new QuestionSpec("What is a Deque (Double-Ended Queue)?",
                    "A sequence allowing efficient insertion and deletion at both ends", "A queue with priority", "A queue of queues", "A read-only queue", 0, "Deques support push/pop at both front and back in O(1) time."),
                new QuestionSpec("What queue variation retrieves items based on value rather than arrival order?",
                    "Priority Queue (Heap)", "Circular queue", "FIFO stream", "Deque", 0, "Priority queues order removals by priority/key using a heap.")
            );
        }

        if (key.contains("hash") || key.contains("map")) {
            return List.of(
                new QuestionSpec("What is the average time complexity of get and put operations in a Hash Map?",
                    "O(1)", "O(n)", "O(log n)", "O(n log n)", 0, "Under a uniform hash distribution, hash map lookups and inserts take O(1) average time."),
                new QuestionSpec("What occurs when two distinct keys produce the same hash bucket index?",
                    "Hash collision", "Stack overflow", "Memory corruption", "Buffer truncation", 0, "A hash collision happens when hash(k1) % capacity == hash(k2) % capacity."),
                new QuestionSpec("Which collision resolution technique chains multiple entries into a linked list or tree at each bucket?",
                    "Separate Chaining", "Open Addressing", "Linear Probing", "Double Hashing", 0, "Separate chaining stores collisions in bucket chains (linked lists or balanced trees)."),
                new QuestionSpec("Which collision technique probes sequential buckets in the table array?",
                    "Linear Probing", "Separate Chaining", "Barycenter heuristic", "Radix distribution", 0, "Open addressing with linear probing checks index (h + i) % capacity."),
                new QuestionSpec("What ratio defines the Load Factor of a hash table with n elements and m buckets?",
                    "n / m", "m / n", "n * m", "log(n) / m", 0, "Load factor alpha = n / m measures table occupancy to determine when to rehash."),
                new QuestionSpec("Why must objects used as HashMap keys override both equals() and hashCode() consistently?",
                    "Equal objects must produce identical hash codes to locate the same bucket", "To sort the keys", "To allow binary search", "To enable garbage collection", 0, "In Java and other OOP languages, if a.equals(b), then a.hashCode() == b.hashCode()."),
                new QuestionSpec("What is the worst-case time complexity of a hash map lookup when all keys collide?",
                    "O(n)", "O(1)", "O(n²)", "O(log n)", 0, "If all keys map to one bucket without treeification, lookup degrades to a linear scan O(n)."),
                new QuestionSpec("What operation does a hash map perform when load factor exceeds its threshold?",
                    "Rehashing: allocates a larger table and re-indexes all entries", "Drops oldest entries", "Converts to an array", "Throws an exception", 0, "Rehashing doubles bucket capacity and re-distributes entries to maintain O(1) average time.")
            );
        }

        // 2. WEB DEVELOPMENT
        if (key.contains("html") || lowerMod.contains("html")) {
            return List.of(
                new QuestionSpec("What does the <!DOCTYPE html> declaration specify at the start of a web document?",
                    "Instructs the browser to render the page in HTML5 standard standards mode", "Declares an XML namespace", "Loads CSS styles", "Initializes JavaScript runtime", 0, "<!DOCTYPE html> prevents browsers from falling back into legacy quirks mode."),
                new QuestionSpec("Which semantic HTML5 element represents the primary, unique content of a document?",
                    "<main>", "<section>", "<div>", "<body>", 0, "<main> contains content central to the document, excluding headers, footers, and nav."),
                new QuestionSpec("Why are semantic elements like <header>, <nav>, and <article> preferred over generic <div> tags?",
                    "They enhance accessibility (screen readers) and SEO search engine indexability", "They execute faster in JavaScript", "They auto-center content on mobile", "They enforce strict type safety", 0, "Semantic tags convey structural meaning to assistive technologies and search crawlers."),
                new QuestionSpec("What attribute on an <img> element provides critical accessibility descriptions?",
                    "alt", "title", "aria-hidden", "src", 0, "The alt attribute gives screen readers an alternate description when images fail to load."),
                new QuestionSpec("Which HTML5 form input type provides built-in email pattern validation on mobile devices?",
                    "<input type=\"email\">", "<input type=\"text\">", "<input type=\"address\">", "<input type=\"regex\">", 0, "type='email' triggers mobile email keyboards and browser constraint validation."),
                new QuestionSpec("What does the required attribute enforce on an HTML form element?",
                    "Prevents form submission until the input field is filled", "Hides the input field", "Disables editing", "Encrypts the value", 0, "HTML5 constraint validation prevents form submission if required fields are empty."),
                new QuestionSpec("Which HTML element is used to associate human-readable text labels with form controls?",
                    "<label for=\"...\">", "<span>", "<legend>", "<p>", 0, "<label for='id'> links the caption to the input, improving touch target area and accessibility."),
                new QuestionSpec("What does the viewport meta tag <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"> do?",
                    "Sets the viewport width to device screen width for responsive mobile rendering", "Zooms in by default", "Disables landscape orientation", "Enforces desktop layout on mobile", 0, "It directs mobile browsers to scale the CSS layout viewport to the physical device width.")
            );
        }

        if (key.contains("css") || key.contains("flex") || key.contains("grid") || lowerMod.contains("css")) {
            return List.of(
                new QuestionSpec("What four concentric layers comprise the standard CSS Box Model?",
                    "Content, Padding, Border, Margin", "Margin, Border, Line, Text", "Font, Color, Shadow, Outline", "Width, Height, Top, Left", 0, "From inside out: Content, Padding, Border, and Margin define element geometry."),
                new QuestionSpec("What is the effect of setting box-sizing: border-box on an element?",
                    "Padding and border are included inside the declared width and height", "Margins are eliminated", "Element is hidden", "Border becomes transparent", 0, "border-box ensures width: 300px with 20px padding remains exactly 300px total width."),
                new QuestionSpec("In CSS Flexbox, which property aligns items along the primary main axis?",
                    "justify-content", "align-items", "flex-direction", "align-content", 0, "justify-content distributes space along the main axis (e.g. flex-start, center, space-between)."),
                new QuestionSpec("In CSS Flexbox, which property aligns items along the perpendicular cross axis?",
                    "align-items", "justify-content", "flex-wrap", "order", 0, "align-items controls cross-axis alignment of items in the current flex line."),
                new QuestionSpec("What is the fundamental difference between CSS Flexbox and CSS Grid?",
                    "Flexbox is primarily one-dimensional (row or column); Grid is two-dimensional (rows and columns)", "Grid is only for tables", "Flexbox does not work on mobile", "Grid requires JavaScript", 0, "Flexbox designs 1D linear layouts, whereas CSS Grid orchestrates 2D rows and columns simultaneously."),
                new QuestionSpec("What does the CSS Grid unit 1fr represent?",
                    "One fraction of the available free space in the grid container", "One fixed frame rate", "One fixed rem font unit", "One physical millimeter", 0, "The fr unit distributes flexible proportions of remaining container space."),
                new QuestionSpec("Which CSS position property positions an element relative to the browser viewport, remaining in place during scroll?",
                    "position: fixed", "position: absolute", "position: relative", "position: static", 0, "Fixed positioning removes the element from document flow and pins it relative to the viewport."),
                new QuestionSpec("How do CSS Media Queries (@media) facilitate Responsive Web Design?",
                    "They apply specific CSS rules conditionally based on viewport width, device orientation, and resolution", "They stream video media", "They compress images", "They detect operating system version", 0, "@media rules adapt layouts dynamically across mobile, tablet, and desktop viewports.")
            );
        }

        if (key.contains("react") || lowerMod.contains("react")) {
            return List.of(
                new QuestionSpec("What is JSX in React?",
                    "A syntax extension allowing HTML-like tags written directly inside JavaScript", "A separate programming language", "A JSON data parser", "A database query dialect", 0, "JSX compiles down to React.createElement() method calls."),
                new QuestionSpec("How does React achieve fast UI updates without direct DOM manipulation on every change?",
                    "Virtual DOM reconciliation and diffing algorithm", "Direct web assembly bytecode", "Kernel thread pinning", "GPU shader rendering", 0, "React compares in-memory Virtual DOM trees and batches minimal updates to the real browser DOM."),
                new QuestionSpec("What is the primary rule regarding React Component Props?",
                    "Props are read-only and immutable by the receiving child component", "Props can be modified freely", "Props must only be integers", "Props trigger browser reloads", 0, "Components must act as pure functions with respect to their received props."),
                new QuestionSpec("Which React Hook declares a state variable and its setter in functional components?",
                    "useState", "useEffect", "useMemo", "useCallback", 0, "const [state, setState] = useState(initialValue) initializes reactive local state."),
                new QuestionSpec("When does the callback inside useEffect(() => {...}, []) execute?",
                    "Once, after the initial component mount", "On every single re-render", "Before DOM painting", "Never", 0, "An empty dependency array [] runs the effect once after the component mounts."),
                new QuestionSpec("Why must list items rendered in React include a unique key prop?",
                    "To help React identify which items have changed, been added, or removed during diffing", "To apply CSS styles", "To sort the array", "To encrypt list data", 0, "Keys provide stable item identity so React can reorder items efficiently rather than re-creating them."),
                new QuestionSpec("What is the purpose of React Router in Single Page Applications (SPAs)?",
                    "To enable client-side route navigation without full page browser reloads", "To query backend REST endpoints", "To bundle webpack assets", "To manage SQL transactions", 0, "React Router intercepts browser URL changes and swaps component views client-side."),
                new QuestionSpec("What is the recommended approach for lifting state up in React?",
                    "Move shared state to the closest common parent component and pass it down via props", "Use global window variables", "Store state in the browser cookies", "Mutate DOM elements directly", 0, "Lifting state up shares single-source-of-truth state across sibling components.")
            );
        }

        // 3. PROGRAMMING LANGUAGES (C++, Python, Java, C)
        if (key.contains("cpp") || lowerMod.contains("c++")) {
            return List.of(
                new QuestionSpec("What does RAII (Resource Acquisition Is Initialization) dictate in C++?",
                    "Resource allocation in constructor and deterministic release in destructor", "Manual free calls at program exit", "Garbage collection cycles", "Global variable pooling", 0, "RAII binds resource lifetime to object scope, guaranteeing automatic cleanup even during exceptions."),
                new QuestionSpec("What is the difference between a pointer and a reference in C++?",
                    "Pointers can be reassigned and null; references must be initialized and cannot be null", "References have memory addresses, pointers do not", "Pointers are faster than references", "References can point to void", 0, "References act as non-null aliases, whereas pointers store mutable memory addresses."),
                new QuestionSpec("Which modern C++ smart pointer represents exclusive ownership of a dynamically allocated object?",
                    "std::unique_ptr", "std::shared_ptr", "std::weak_ptr", "raw pointer *", 0, "unique_ptr prevents copying and frees its managed resource automatically when it goes out of scope."),
                new QuestionSpec("What does std::move do in C++11 and later?",
                    "Casts an lvalue to an rvalue reference to enable move semantics without deep copying", "Physically moves memory bytes", "Deletes the source object immediately", "Creates a thread", 0, "std::move enables transferring resource ownership (pointers, buffers) without expensive allocations."),
                new QuestionSpec("What is the average lookup time of std::unordered_map vs std::map in C++ STL?",
                    "unordered_map is O(1) average (hash table); map is O(log n) (Red-Black tree)", "Both are O(1)", "Both are O(n)", "map is faster than unordered_map", 0, "unordered_map uses hashing; map maintains ordered keys via a self-balancing red-black tree."),
                new QuestionSpec("What does the const keyword on a C++ member function indicate (e.g. int size() const)?",
                    "The function promises not to modify any non-mutable member variables of the class", "The return value cannot be changed", "The function is static", "The function cannot be overridden", 0, "const member functions enforce read-only access to object state."),
                new QuestionSpec("How do C++ templates achieve generic programming without runtime overhead?",
                    "Compile-time monomorphization (generating specialized code for each used type)", "Runtime reflection", "Dynamic type casting", "Boxing primitive objects", 0, "The C++ compiler instantiates concrete type-specialized versions of template functions and classes at compile time."),
                new QuestionSpec("What is a virtual destructor required for in C++ polymorphic base classes?",
                    "To ensure derived class destructors are called when deleting via a base pointer", "To speed up destruction", "To allow multiple inheritance", "To prevent stack allocation", 0, "Without a virtual destructor, deleting a derived instance via a Base* leads to undefined behavior and leaks.")
            );
        }

        if (key.contains("python") || lowerMod.contains("python")) {
            return List.of(
                new QuestionSpec("What is the mutability distinction between Python lists and tuples?",
                    "Lists are mutable; tuples are immutable", "Lists are immutable; tuples are mutable", "Both are immutable", "Both are mutable", 0, "Lists can be modified in-place; tuples cannot have elements added, removed, or changed once created."),
                new QuestionSpec("What does a Python list comprehension [x**2 for x in range(5) if x % 2 == 0] produce?",
                    "[0, 4, 16]", "[0, 1, 4, 9, 16]", "[1, 9]", "[4, 16]", 0, "Values 0, 2, 4 squared yield [0, 4, 16]."),
                new QuestionSpec("What is the purpose of the Global Interpreter Lock (GIL) in CPython?",
                    "Prevents multiple native threads from executing Python bytecode simultaneously", "Accelerates GPU compute", "Encrypts memory allocations", "Eliminates syntax errors", 0, "The GIL protects CPython memory management and reference counts from concurrent race conditions."),
                new QuestionSpec("How do Python generators differ from regular functions?",
                    "Generators use yield to produce values lazily one at a time, preserving execution state", "Generators return all values in a pre-allocated array", "Generators run in separate OS processes", "Generators cannot take arguments", 0, "yield suspends function execution and resumes on next(), achieving O(1) memory iteration."),
                new QuestionSpec("What does the *args and **kwargs syntax enable in Python function signatures?",
                    "Accepting variable positional arguments as a tuple and keyword arguments as a dictionary", "Pointer dereferencing", "Exponentiation math", "Type annotation validation", 0, "*args captures arbitrary positional arguments, while **kwargs captures named keyword arguments."),
                new QuestionSpec("What does the with statement guarantee when opening files in Python (with open(...) as f:)?",
                    "Deterministic file closure even if an unhandled exception occurs", "Faster disk write speed", "Automatic file encryption", "Read-only access", 0, "The context manager enters and exits, ensuring f.close() is executed in all exit paths."),
                new QuestionSpec("What is the time complexity of checking key membership (if key in d:) in a Python dictionary?",
                    "O(1) average time", "O(n)", "O(log n)", "O(n²)", 0, "Python dictionaries use optimized open-addressing hash tables for constant time lookups."),
                new QuestionSpec("What is the function of a Python decorator (@decorator)?",
                    "A callable that takes a function as argument and returns an enhanced wrapper function", "A syntax highlighter", "A database index", "A thread lock", 0, "Decorators wrap functions to add logging, caching, authentication, or timing cleanly.")
            );
        }

        // 4. AI & MACHINE LEARNING
        if (key.contains("ai") || key.contains("learn") || lowerMod.contains("learning") || lowerMod.contains("neural")) {
            return List.of(
                new QuestionSpec("What is the core difference between Supervised and Unsupervised learning?",
                    "Supervised learning trains on labeled target data; unsupervised finds patterns in unlabeled data", "Supervised requires GPUs; unsupervised does not", "Unsupervised uses regression only", "Supervised has no loss function", 0, "Supervised learning maps features X to known targets y; unsupervised discovers latent structure in X."),
                new QuestionSpec("What does Gradient Descent optimize during machine learning model training?",
                    "Minimizes the loss/cost function by updating model weights in the direction of steepest descent", "Maximizes execution speed", "Increases model parameters", "Compresses dataset size", 0, "Gradient descent takes iterative steps proportional to negative gradient to find loss minima."),
                new QuestionSpec("What does Overfitting indicate about a machine learning model?",
                    "The model memorized training noise and performs poorly on unseen validation data", "The model is too simple", "The training loss is too high", "The learning rate is zero", 0, "Overfitting features very low training error but high generalization error on new test sets."),
                new QuestionSpec("Which activation function is most widely used in hidden layers of deep neural networks to mitigate vanishing gradients?",
                    "ReLU (Rectified Linear Unit: f(x) = max(0, x))", "Sigmoid", "Step function", "Tanh", 0, "ReLU provides constant gradient 1 for positive inputs, accelerating convergence in deep architectures."),
                new QuestionSpec("What algorithm computes gradients across layers in deep learning via the chain rule of calculus?",
                    "Backpropagation", "Forward pass", "K-Means", "Principal Component Analysis", 0, "Backpropagation calculates partial derivatives of the loss with respect to all network weights backwards."),
                new QuestionSpec("What is the purpose of Dropout regularization in deep neural networks?",
                    "Randomly deactivates neurons during training to prevent co-adaptation of weights", "Deletes unused layers", "Increases learning rate", "Normalizes inputs", 0, "Dropout forces the network to learn redundant, robust feature representations across ensemble sub-networks."),
                new QuestionSpec("What is the self-attention mechanism in the Transformer architecture designed to do?",
                    "Compute dynamic affinity weights between all pairs of tokens in a sequence simultaneously", "Process tokens strictly one-by-one sequentially", "Compress audio waves", "Sort vocabulary words", 0, "Self-attention computes Q*K^T / sqrt(d_k) to allow tokens to attend to distant contextual tokens directly."),
                new QuestionSpec("What does RAG (Retrieval-Augmented Generation) accomplish when deploying Large Language Models (LLMs)?",
                    "Retrieves relevant enterprise documents from a vector database and injects them into the LLM prompt context", "Retrains the base foundation model", "Generates random text", "Removes tokens", 0, "RAG grounds LLM responses on up-to-date, factual proprietary knowledge without expensive fine-tuning.")
            );
        }

        // 5. DATA SCIENCE
        if (key.contains("data") || key.contains("pandas") || key.contains("numpy") || lowerMod.contains("data")) {
            return List.of(
                new QuestionSpec("What is the primary performance benefit of NumPy ndarrays over standard Python lists?",
                    "Contiguous memory layout in C with vectorized SIMD array math without Python interpreter loop overhead", "NumPy arrays can hold any type dynamically", "NumPy does not allocate RAM", "NumPy uses text files", 0, "Homogeneous memory layout enables compiled C vectorization and CPU cache optimization."),
                new QuestionSpec("What does Broadcasting in NumPy allow?",
                    "Performing arithmetic operations on arrays of different compatible shapes without copying data", "Transmitting arrays over Wi-Fi", "Printing arrays to console", "Sorting multidimensional matrices", 0, "Broadcasting stretches smaller dimensions along trailing axes to match larger array dimensions."),
                new QuestionSpec("In Pandas, what is the difference between a Series and a DataFrame?",
                    "A Series is 1D with an index; a DataFrame is a 2D tabular structure of rows and columns", "A Series has no index", "A DataFrame cannot store numbers", "Both are identical", 0, "A DataFrame represents a spreadsheet-like table where each column is a Pandas Series sharing a common index."),
                new QuestionSpec("What does df.groupby(\"category\").mean() perform in Pandas?",
                    "Splits data by unique category values, computes the arithmetic mean for each group, and combines results", "Deletes category column", "Filters duplicate rows", "Sorts table randomly", 0, "The split-apply-combine paradigm aggregates metrics across unique categorical partitions."),
                new QuestionSpec("What is the Interquartile Range (IQR) used for in Exploratory Data Analysis (EDA)?",
                    "Measuring statistical dispersion (Q3 - Q1) and detecting outliers beyond 1.5 * IQR", "Calculating average", "Counting missing null values", "Encoding categorical text", 0, "Values lying below Q1 - 1.5*IQR or above Q3 + 1.5*IQR are standard statistical outlier thresholds."),
                new QuestionSpec("What is the purpose of Principal Component Analysis (PCA) in data science?",
                    "Unsupervised dimensionality reduction that projects features onto orthogonal axes of maximum variance", "Supervised classification", "Database clustering", "Interpolating missing data", 0, "PCA finds principal eigenvectors that preserve the maximum possible information in lower dimensions."),
                new QuestionSpec("What does a p-value < 0.05 signify in hypothesis testing (e.g. A/B testing)?",
                    "Statistically significant evidence to reject the null hypothesis at the 5% alpha significance level", "5% probability the hypothesis is true", "Experiment failed", "Data is corrupted", 0, "A p-value below alpha indicates observed results are unlikely to have occurred under random chance alone."),
                new QuestionSpec("What is the architecture of Apache Spark designed for in Big Data engineering?",
                    "Distributed, in-memory parallel computation across cluster worker nodes using DAG execution", "Single-core disk operations", "Relational schema locking", "Desktop GUI rendering", 0, "Spark caches Resilient Distributed Datasets (RDDs) in cluster RAM for high-throughput distributed pipelines.")
            );
        }

        // 6. GAME DEVELOPMENT
        if (key.contains("game") || key.contains("physics") || key.contains("unity") || lowerMod.contains("game")) {
            return List.of(
                new QuestionSpec("Why is delta time (deltaTime) used in game movement calculations (e.g. position += velocity * dt)?",
                    "To achieve frame rate independent movement regardless of whether running at 30, 60, or 144 FPS", "To accelerate graphics", "To prevent memory leaks", "To sync audio waves", 0, "Multiplying speed by delta time ensures objects traverse the same physical distance per second at any framerate."),
                new QuestionSpec("What does the Dot Product of two normalized 2D/3D vectors indicate in game math?",
                    "Cosine of the angle between vectors (1 = identical direction, 0 = perpendicular, -1 = opposite)", "Cross product magnitude", "Vector length", "Euclidean distance", 0, "Vector dot product tests field-of-view, lighting diffuse angles, and directional alignment."),
                new QuestionSpec("What collision detection method tests overlapping minimum and maximum bounds along coordinate axes?",
                    "AABB (Axis-Aligned Bounding Box) Collision Test", "Ray marching", "Convex Hull Decomposition", "Minkowski Portal Refinement", 0, "AABB tests if (boxA.min <= boxB.max && boxA.max >= boxB.min) across X, Y, and Z axes."),
                new QuestionSpec("What is the purpose of the MonoBehaviour Update() vs FixedUpdate() methods in Unity?",
                    "Update runs once per rendered frame; FixedUpdate runs on a reliable fixed physics timestep", "Both run simultaneously", "Update is for physics only", "FixedUpdate runs only on startup", 0, "Physics calculations require consistent fixed time steps (FixedUpdate) to avoid simulation instability."),
                new QuestionSpec("What does Linear Interpolation (Lerp) calculate between point A and point B with parameter t in [0, 1]?",
                    "A + (B - A) * t (smooth transition position between A and B)", "Euclidean distance", "Angular momentum", "Reflection vector", 0, "Lerp computes smooth intermediate values for camera movement, animations, and color transitions."),
                new QuestionSpec("How does a Raycast function in 3D physics engines?",
                    "Casts an invisible mathematical ray from an origin along a direction vector to detect intersecting colliders", "Renders a visual laser beam", "Calculates lighting reflections only", "Generates terrain heightmaps", 0, "Raycasts return hit point, normal, distance, and collider for shooting mechanics and line-of-sight checks."),
                new QuestionSpec("What is the primary benefit of Object Pooling in game performance optimization?",
                    "Reuses pre-allocated game objects (e.g. bullets, particles) to eliminate runtime garbage collection spikes", "Compiles shaders faster", "Increases texture resolution", "Automates physics", 0, "Instantiating and destroying hundreds of entities creates GC memory stalls; pooling recycles active instances."),
                new QuestionSpec("What is the difference between Vertex Shaders and Fragment (Pixel) Shaders in modern graphics pipelines?",
                    "Vertex shaders process 3D vertex positions; Fragment shaders compute final pixel colors and lighting", "Vertex shaders run on CPU", "Fragment shaders load textures only", "Both run after rasterization", 0, "Vertex shaders transform geometry; rasterization generates pixels; fragment shaders shade each pixel.")
            );
        }

        return null;
    }

    private static List<QuestionSpec> buildFallbackQuestions(String topic, String module, String submodule, String title, List<QuestionSpec> existing) {
        List<QuestionSpec> list = new ArrayList<>();
        if (existing != null) {
            list.addAll(existing);
        }

        String t = title != null ? title : "the concept";
        String m = module != null ? module : "this module";
        String s = submodule != null ? submodule : "core fundamentals";

        list.add(new QuestionSpec(
            "What is the primary foundational concept governing " + t + " in " + m + "?",
            "Structured execution, high cohesion, and deterministic state transitions",
            "Unbounded non-deterministic execution",
            "Bypassing compiler validation and type safety",
            "Eliminating memory management completely",
            0,
            t + " establishes predictable architectural boundaries and modular design in " + m + "."
        ));

        list.add(new QuestionSpec(
            "What key invariant must hold true throughout operations in " + t + "?",
            "Internal state consistency and validity before and after execution",
            "Data corruption is permitted during concurrency",
            "All resources remain allocated indefinitely",
            "Execution order is randomized across threads",
            0,
            "Maintaining correct invariants ensures " + t + " executes reliably without data corruption."
        ));

        list.add(new QuestionSpec(
            "What is the computational complexity or resource trade-off characteristic of " + t + "?",
            "Optimal performance scaling predictably with input size under standard runtime constraints",
            "Factorial runtime O(n!) in all typical scenarios",
            "Strictly zero memory allocation regardless of input size",
            "Exponential latency under normal workloads",
            0,
            "Engineered implementations of " + t + " minimize latency and computational overhead."
        ));

        list.add(new QuestionSpec(
            "Which critical boundary condition or edge case must be handled when implementing " + t + "?",
            "Empty inputs, null references, and extreme boundary limits",
            "Only positive inputs",
            "Exact powers of two only",
            "Pre-sorted alphabetical data only",
            0,
            "Robust software must handle empty, minimal, and maximum edge boundaries gracefully."
        ));

        list.add(new QuestionSpec(
            "How does " + t + " contribute to maintainability and code quality in " + s + "?",
            "By encapsulating domain logic, avoiding code duplication, and simplifying unit testing",
            "By hard-coding values throughout the codebase",
            "By coupling independent modules tightly together",
            "By disabling compiler warnings and logging",
            0,
            "High-cohesion design in " + s + " makes code testable, performant, and extensible."
        ));

        list.add(new QuestionSpec(
            "What testing strategy is recommended for verifying " + t + "?",
            "Automated unit testing covering standard cases, edge cases, and unexpected inputs",
            "Testing exclusively in production with live users",
            "Visual inspection without assertions",
            "Omitting tests if compilation succeeds",
            0,
            "Comprehensive test coverage verifies that " + t + " behaves correctly under all conditions."
        ));

        list.add(new QuestionSpec(
            "How should error handling be structured when operating with " + t + "?",
            "Throw descriptive exceptions or return result types, logging actionable context for diagnosis",
            "Silently swallow errors and continue in an unknown state",
            "Immediately terminate the operating system process",
            "Ignore return values and status codes",
            0,
            "Graceful error handling and deterministic cleanup are required for production stability."
        ));

        list.add(new QuestionSpec(
            "What is a prominent real-world application of " + t + " in modern software systems?",
            "High-scale enterprise backends, responsive client UIs, cloud microservices, and distributed data pipelines",
            "Solely academic chalkboard proofs with no real-world relevance",
            "Only obsolete legacy punch-card systems",
            "Limited strictly to single-line terminal scripts",
            0,
            t + " is an industry standard technique utilized across distributed systems and modern platforms."
        ));

        return list.subList(0, 8);
    }
}
