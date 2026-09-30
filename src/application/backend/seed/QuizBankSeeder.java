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

        // ── 7. DATABASE: SQL FUNDAMENTALS ────────────────────────────────────
        if (key.contains("select") || key.contains("filter") || (lowerMod.contains("sql") && key.contains("quer"))) {
            return List.of(
                new QuestionSpec("Which SQL clause is used to filter rows returned by a SELECT query?",
                    "WHERE", "HAVING", "GROUP BY", "ORDER BY", 0, "WHERE filters individual rows before grouping; HAVING filters after grouping."),
                new QuestionSpec("What does SELECT DISTINCT do in SQL?",
                    "Returns only unique (non-duplicate) rows", "Sorts rows alphabetically", "Limits rows to 10", "Joins two tables", 0, "DISTINCT removes duplicate values from the result set."),
                new QuestionSpec("Which SQL operator checks if a value falls within a range (inclusive)?",
                    "BETWEEN", "IN", "LIKE", "EXISTS", 0, "BETWEEN x AND y is equivalent to x <= col <= y and includes both boundary values."),
                new QuestionSpec("What does the LIKE operator with pattern '%sql%' match?",
                    "Any string containing 'sql' anywhere", "Strings starting with 'sql'", "Only the exact string 'sql'", "Strings of exactly 3 characters", 0, "The % wildcard matches zero or more characters, so '%sql%' matches any string with 'sql' inside."),
                new QuestionSpec("Which SQL keyword is used to sort query results in descending order?",
                    "ORDER BY column DESC", "ORDER BY column ASC", "SORT BY column", "ARRANGE column DESC", 0, "ORDER BY col DESC sorts from highest to lowest; ASC (default) sorts lowest to highest."),
                new QuestionSpec("What is the result of: SELECT 10 / 3 in most SQL databases (integer division)?",
                    "3", "3.33", "4", "Error", 0, "Integer division truncates the decimal in most databases; use 10.0/3 or CAST to get a decimal."),
                new QuestionSpec("Which clause limits the number of rows returned by a SELECT query?",
                    "LIMIT (or TOP in SQL Server)", "WHERE", "HAVING", "DISTINCT", 0, "LIMIT n (MySQL/PostgreSQL) or TOP n (SQL Server) restricts the result set size."),
                new QuestionSpec("What does NULL represent in SQL?",
                    "An unknown or missing value (not zero or empty string)", "The integer 0", "An empty string ''", "A boolean false", 0, "NULL means absence of data; use IS NULL / IS NOT NULL to check, not = NULL.")
            );
        }

        if (key.contains("aggregate") || key.contains("group-by") || key.contains("group")) {
            return List.of(
                new QuestionSpec("Which SQL aggregate function returns the number of rows in a group?",
                    "COUNT(*)", "SUM()", "AVG()", "MAX()", 0, "COUNT(*) counts all rows including NULLs; COUNT(col) counts non-NULL values only."),
                new QuestionSpec("What is the difference between WHERE and HAVING in SQL?",
                    "WHERE filters rows before grouping; HAVING filters groups after GROUP BY",
                    "HAVING filters rows before grouping; WHERE filters after",
                    "They are interchangeable",
                    "WHERE works only with JOINs", 0, "WHERE applies to individual rows; HAVING applies to aggregated groups."),
                new QuestionSpec("What does GROUP BY do in a SQL query?",
                    "Groups rows with the same column values to allow aggregate functions per group",
                    "Sorts rows by a column",
                    "Filters duplicate rows",
                    "Joins two tables by a key", 0, "GROUP BY collapses rows with matching values into a single group row for aggregation."),
                new QuestionSpec("Which query counts employees per department only for departments with more than 5 employees?",
                    "SELECT dept, COUNT(*) FROM employees GROUP BY dept HAVING COUNT(*) > 5",
                    "SELECT dept, COUNT(*) FROM employees WHERE COUNT(*) > 5",
                    "SELECT dept FROM employees HAVING COUNT(*) > 5",
                    "SELECT dept, COUNT(*) FROM employees ORDER BY COUNT(*) > 5", 0, "HAVING COUNT(*) > 5 filters the grouped results, which WHERE cannot do."),
                new QuestionSpec("What does AVG(salary) return when some salary values are NULL?",
                    "Average of non-NULL salary values only (NULLs are ignored)",
                    "Returns NULL for the whole query",
                    "Treats NULL as 0 in the average",
                    "Throws a division by zero error", 0, "SQL aggregate functions ignore NULL values unless you explicitly handle them with COALESCE."),
                new QuestionSpec("What is the purpose of the ROLLUP modifier in GROUP BY?",
                    "Generates subtotals and a grand total row in addition to regular group rows",
                    "Randomly shuffles group order",
                    "Removes duplicate rows",
                    "Joins aggregated results to another table", 0, "GROUP BY ROLLUP(a, b) produces subtotals for each level and a grand total as the final row."),
                new QuestionSpec("Which SQL function returns the highest value in a numeric column?",
                    "MAX(column)", "TOP(column)", "GREATEST(column)", "CEILING(column)", 0, "MAX() returns the largest value; MIN() returns the smallest."),
                new QuestionSpec("What does COUNT(DISTINCT column) compute?",
                    "The number of unique non-NULL values in the column",
                    "Total number of rows including duplicates",
                    "The maximum distinct value",
                    "The sum of distinct values", 0, "DISTINCT inside COUNT deduplicates values before counting.")
            );
        }

        // ── 8. DATABASE: ADVANCED SQL ─────────────────────────────────────────
        if (key.contains("join") || key.contains("inner") || key.contains("outer") || key.contains("left-right")) {
            return List.of(
                new QuestionSpec("What does an INNER JOIN return?",
                    "Only rows where the join condition matches in BOTH tables",
                    "All rows from the left table and matching rows from the right",
                    "All rows from both tables regardless of match",
                    "Only rows that do NOT match", 0, "INNER JOIN is the intersection — it returns only matched rows from both sides."),
                new QuestionSpec("What does a LEFT JOIN (LEFT OUTER JOIN) return?",
                    "All rows from the left table, with NULLs for unmatched right-table columns",
                    "Only rows that match in both tables",
                    "All rows from the right table",
                    "No rows if any null exists", 0, "LEFT JOIN preserves every left-table row and fills non-matching right columns with NULL."),
                new QuestionSpec("What is a SELF JOIN used for?",
                    "Joining a table to itself to compare rows within the same table (e.g. employee-manager hierarchy)",
                    "Joining a table to a copy of itself stored in a different database",
                    "Joining two tables with the same schema",
                    "Recursively deleting duplicate rows", 0, "Self joins are used for hierarchical or comparative queries where both sides reference the same table."),
                new QuestionSpec("What is the result of a FULL OUTER JOIN?",
                    "All rows from both tables, with NULLs where there is no match on either side",
                    "Only matched rows from both tables",
                    "All rows from the left table only",
                    "A cartesian product of both tables", 0, "FULL OUTER JOIN is the union of LEFT JOIN and RIGHT JOIN results."),
                new QuestionSpec("What is a CROSS JOIN?",
                    "A cartesian product: every row from table A paired with every row from table B",
                    "A join with no matching condition that returns 0 rows",
                    "A join that filters NULL values",
                    "Equivalent to an INNER JOIN", 0, "CROSS JOIN with m rows × n rows produces m×n result rows; no ON condition is used."),
                new QuestionSpec("Which JOIN type should you use to find rows in table A that have NO match in table B?",
                    "LEFT JOIN ... WHERE B.id IS NULL",
                    "INNER JOIN ... WHERE B.id IS NULL",
                    "FULL JOIN ... WHERE A.id IS NOT NULL",
                    "CROSS JOIN ... HAVING count = 0", 0, "A LEFT JOIN with WHERE B.pk IS NULL finds rows in A with no corresponding B row."),
                new QuestionSpec("What is the ON clause in a JOIN statement used for?",
                    "Specifying the condition that defines how rows from two tables are matched",
                    "Filtering rows after the join is complete",
                    "Sorting the joined result",
                    "Defining primary keys", 0, "ON specifies the join predicate, e.g. ON orders.customer_id = customers.id."),
                new QuestionSpec("What does a NATURAL JOIN do automatically?",
                    "Joins tables on ALL columns with the same name and type in both tables",
                    "Randomly selects join columns",
                    "Creates an index automatically",
                    "Is identical to a CROSS JOIN", 0, "NATURAL JOIN is convenient but risky — column name collisions can cause unexpected behavior.")
            );
        }

        if (key.contains("cte") || key.contains("window") || key.contains("subquer") || key.contains("row-number") || key.contains("rank")) {
            return List.of(
                new QuestionSpec("What is a CTE (Common Table Expression) in SQL?",
                    "A named temporary result set defined with WITH that can be referenced in the main query",
                    "A permanent view stored in the database",
                    "A type of index",
                    "A stored procedure", 0, "CTEs defined with WITH clause_name AS (...) improve readability and allow recursive queries."),
                new QuestionSpec("What does the SQL window function ROW_NUMBER() do?",
                    "Assigns a unique sequential integer to each row within its partition, starting at 1",
                    "Counts the total number of rows in the table",
                    "Returns the row with the minimum value",
                    "Moves rows to the next partition", 0, "ROW_NUMBER() OVER (PARTITION BY col ORDER BY col) numbers rows uniquely within each group."),
                new QuestionSpec("What is the difference between RANK() and DENSE_RANK() when there are ties?",
                    "RANK() skips numbers after a tie (1,2,2,4); DENSE_RANK() does not skip (1,2,2,3)",
                    "DENSE_RANK() skips numbers; RANK() does not",
                    "They are identical",
                    "RANK() only works with strings", 0, "RANK() leaves gaps after tied rows; DENSE_RANK() assigns the next consecutive rank without gaps."),
                new QuestionSpec("What does the PARTITION BY clause inside a window function do?",
                    "Divides the result set into groups (partitions) so the window function is applied independently per group",
                    "Creates a physical table partition",
                    "Filters rows before the window function runs",
                    "Sorts the final result", 0, "PARTITION BY resets the window calculation for each group, similar to GROUP BY but without collapsing rows."),
                new QuestionSpec("What does LAG(salary, 1) OVER (ORDER BY hire_date) compute?",
                    "The salary value from the previous row in the ORDER BY sequence",
                    "The salary value from the next row",
                    "The average salary of the last 2 rows",
                    "The difference in hire dates", 0, "LAG accesses a prior row's value; LEAD accesses the next row's value within the window."),
                new QuestionSpec("What is a correlated subquery?",
                    "A subquery that references a column from the outer query and is re-evaluated for each outer row",
                    "A subquery that runs only once and caches results",
                    "A subquery that creates a CTE",
                    "A subquery inside a GROUP BY clause", 0, "Correlated subqueries are powerful but can be slow — each outer row triggers a separate inner execution."),
                new QuestionSpec("What does the OVER() clause signify in a SQL window function?",
                    "It defines the window (set of rows) over which the function is calculated",
                    "It opens a database transaction",
                    "It creates a new schema",
                    "It executes the query recursively", 0, "Without OVER(), COUNT/SUM/AVG are aggregate functions; with OVER(), they become window functions."),
                new QuestionSpec("What is the purpose of a recursive CTE in SQL?",
                    "To query hierarchical or graph-structured data (e.g. org charts, category trees) using self-referencing WITH RECURSIVE",
                    "To improve index performance",
                    "To delete rows in a loop",
                    "To create temporary tables", 0, "Recursive CTEs have a base case (anchor) and a recursive step, enabling traversal of tree structures.")
            );
        }

        // ── 9. DATABASE: POSTGRESQL ───────────────────────────────────────────
        if (key.contains("psql") || key.contains("data-type") || key.contains("postgresql") || key.contains("postgres")) {
            return List.of(
                new QuestionSpec("Which PostgreSQL data type stores variable-length character strings with an optional length limit?",
                    "VARCHAR(n)", "CHAR(n)", "TEXT", "BYTEA", 0, "VARCHAR(n) allows strings up to n characters; TEXT stores unlimited length strings without padding."),
                new QuestionSpec("What is the psql command to list all tables in the current database?",
                    "\\dt", "\\d", "\\l", "\\tables", 0, "\\dt lists all tables; \\d <table> shows columns; \\l lists all databases."),
                new QuestionSpec("What does the SERIAL data type do in PostgreSQL?",
                    "Auto-increments an integer column (shorthand for INTEGER with a sequence)",
                    "Stores binary-serialized Java objects",
                    "Creates a UUID primary key",
                    "Stores a fixed-length byte string", 0, "SERIAL auto-creates a sequence and sets the column default to nextval(), perfect for auto-increment IDs."),
                new QuestionSpec("What does the UNIQUE constraint enforce in PostgreSQL?",
                    "All values in the column (or column group) must be distinct across all rows",
                    "The column cannot store NULL",
                    "The column must be a primary key",
                    "Values must match a foreign key", 0, "UNIQUE prevents duplicate entries; unlike PRIMARY KEY, a UNIQUE column can still contain NULLs."),
                new QuestionSpec("What type of index does PostgreSQL use by default when you CREATE INDEX?",
                    "B-tree index", "Hash index", "GIN index", "BRIN index", 0, "B-tree is the default and works for equality and range queries with <, <=, =, >=, > operators."),
                new QuestionSpec("What does JSONB differ from JSON in PostgreSQL?",
                    "JSONB stores data in a parsed binary format enabling fast indexing; JSON stores raw text",
                    "JSON supports indexing; JSONB does not",
                    "JSONB is slower to query than JSON",
                    "They are identical", 0, "JSONB parses and compresses JSON at write time, allowing GIN indexes for fast key/value lookups."),
                new QuestionSpec("What does EXPLAIN ANALYZE do in PostgreSQL?",
                    "Executes the query and shows the actual execution plan with real timing and row counts",
                    "Shows the plan without running the query",
                    "Optimizes the query automatically",
                    "Creates statistics on a table", 0, "EXPLAIN shows estimated plan; EXPLAIN ANALYZE actually runs it and reports actual vs estimated rows."),
                new QuestionSpec("What is a PostgreSQL stored procedure used for?",
                    "Encapsulating reusable SQL logic executed server-side, supporting transactions and complex control flow",
                    "Creating permanent views",
                    "Defining column constraints",
                    "Scheduling cron jobs", 0, "Stored procedures (CREATE PROCEDURE) can include BEGIN/COMMIT and are called with CALL procedure_name().")
            );
        }

        if (key.contains("constraint") || key.contains("stored-procedure") || key.contains("trigger")) {
            return List.of(
                new QuestionSpec("What does the NOT NULL constraint enforce on a table column?",
                    "The column must always have a value; NULL is not permitted",
                    "The column value must be unique",
                    "The column must reference a foreign key",
                    "The column value must be positive", 0, "NOT NULL prevents insertion of rows without providing a value for that column."),
                new QuestionSpec("What is a FOREIGN KEY constraint used for?",
                    "Enforcing referential integrity by ensuring a column value matches a primary key in another table",
                    "Preventing duplicate values",
                    "Auto-incrementing an ID column",
                    "Encrypting sensitive column data", 0, "FOREIGN KEY links tables, preventing orphaned records and ensuring relational consistency."),
                new QuestionSpec("What does ON DELETE CASCADE do in a foreign key constraint?",
                    "Automatically deletes child rows when the referenced parent row is deleted",
                    "Sets child rows to NULL when parent is deleted",
                    "Prevents deletion of the parent row",
                    "Copies the parent row to an archive table", 0, "CASCADE propagates the DELETE down the foreign key chain automatically."),
                new QuestionSpec("What is a CHECK constraint?",
                    "A condition that every row must satisfy (e.g. CHECK(age >= 18))",
                    "A constraint that checks for NULL values only",
                    "Equivalent to a UNIQUE constraint",
                    "A trigger that runs on SELECT", 0, "CHECK lets you define custom validation rules at the database level, enforced on INSERT and UPDATE."),
                new QuestionSpec("What is the purpose of a database trigger?",
                    "Automatically executing a function in response to INSERT, UPDATE, or DELETE events on a table",
                    "Scheduling periodic maintenance jobs",
                    "Enforcing foreign key constraints manually",
                    "Creating indexes automatically", 0, "Triggers (BEFORE/AFTER INSERT/UPDATE/DELETE) run stored functions to enforce business rules or audit changes."),
                new QuestionSpec("What does the DEFAULT constraint do in a column definition?",
                    "Provides an automatic value when no value is specified during INSERT",
                    "Sets the column as the primary key",
                    "Forces the column to be unique",
                    "Makes the column read-only", 0, "DEFAULT simplifies inserts by pre-filling a column value, e.g. DEFAULT CURRENT_TIMESTAMP."),
                new QuestionSpec("What is the difference between a stored procedure and a function in PostgreSQL?",
                    "Procedures can commit/rollback transactions; functions cannot and must return a value",
                    "Functions can modify table data; procedures cannot",
                    "They are identical in PostgreSQL 14+",
                    "Procedures are faster than functions", 0, "PostgreSQL functions are used in SELECT; procedures are called with CALL and support full transaction control."),
                new QuestionSpec("Which SQL statement modifies a table's structure, such as adding a new column?",
                    "ALTER TABLE", "UPDATE TABLE", "MODIFY TABLE", "CHANGE TABLE", 0, "ALTER TABLE tablename ADD COLUMN col_name datatype adds a column to an existing table.")
            );
        }

        // ── 10. DATABASE: MONGODB ─────────────────────────────────────────────
        if (key.contains("bson") || key.contains("collection") || key.contains("document") || key.contains("mongodb") || key.contains("mongo")) {
            return List.of(
                new QuestionSpec("What format does MongoDB use to store documents internally?",
                    "BSON (Binary JSON) — a binary-encoded superset of JSON",
                    "Plain text JSON files",
                    "CSV rows",
                    "XML documents", 0, "BSON extends JSON with additional types (Date, ObjectId, Binary) and is optimized for serialization speed."),
                new QuestionSpec("What is a MongoDB collection equivalent to in a relational database?",
                    "A table", "A row", "A column", "A database schema", 0, "A collection holds documents the way a table holds rows; it has no fixed schema by default."),
                new QuestionSpec("What is the default primary key field for every MongoDB document?",
                    "_id (an ObjectId generated automatically if not provided)",
                    "id (an integer auto-increment)",
                    "uuid (a UUID string)",
                    "pk (a composite key)", 0, "MongoDB auto-generates a 12-byte ObjectId for _id if you don't provide one."),
                new QuestionSpec("Which MongoDB query operator finds documents where a field value is in a given array of values?",
                    "$in", "$exists", "$elemMatch", "$all", 0, "db.col.find({status: {$in: ['active','pending']}}) returns docs where status is either 'active' or 'pending'."),
                new QuestionSpec("What does the MongoDB command db.collection.find({age: {$gt: 25}}) return?",
                    "All documents where the age field is greater than 25",
                    "Documents where age equals 25",
                    "Documents where age is less than 25",
                    "All documents regardless of age", 0, "$gt means 'greater than'; $gte = >=, $lt = <, $lte = <=, $ne = not equal."),
                new QuestionSpec("What is the purpose of MongoDB's aggregation pipeline?",
                    "Processes documents through sequential stages ($match, $group, $sort, $project) to transform and compute results",
                    "Replaces all documents matching a filter",
                    "Creates a permanent view of the collection",
                    "Generates BSON indexes automatically", 0, "Each pipeline stage transforms its input documents and passes results to the next stage."),
                new QuestionSpec("What does the MongoDB $lookup stage do in an aggregation pipeline?",
                    "Performs a left outer join with another collection (equivalent to SQL LEFT JOIN)",
                    "Looks up the schema of a collection",
                    "Creates an index on a field",
                    "Counts matching documents", 0, "$lookup joins documents from another collection based on a matching field, enabling SQL-style relational queries."),
                new QuestionSpec("Which MongoDB method inserts a single document into a collection?",
                    "db.collection.insertOne({})", "db.collection.save({})", "db.collection.add({})", "db.collection.put({})", 0, "insertOne() inserts one document; insertMany([]) inserts multiple; both return the inserted _id(s).")
            );
        }

        if (key.contains("crud") || key.contains("aggregation-pipeline") || key.contains("mongoose") || key.contains("odm") || key.contains("schema-pattern")) {
            return List.of(
                new QuestionSpec("Which MongoDB method updates a single matching document?",
                    "db.collection.updateOne({filter}, {$set: {field: value}})",
                    "db.collection.modify({filter}, {field: value})",
                    "db.collection.edit({filter}, {field: value})",
                    "db.collection.patch({filter}, {field: value})", 0, "updateOne with $set modifies only the specified fields without replacing the entire document."),
                new QuestionSpec("What does the MongoDB $set operator do in an update operation?",
                    "Sets or updates the value of specific fields without affecting other document fields",
                    "Replaces the entire document",
                    "Deletes the specified fields",
                    "Increments numeric field values", 0, "$set is the most common update operator; $unset removes fields; $inc increments numbers."),
                new QuestionSpec("What is Mongoose in the context of Node.js and MongoDB?",
                    "An Object Document Mapper (ODM) that provides schema validation and model methods for MongoDB",
                    "A MongoDB GUI client",
                    "A REST API framework built on top of MongoDB",
                    "A MongoDB backup tool", 0, "Mongoose defines Schemas and Models that enforce structure, types, and validations on MongoDB documents in Node.js."),
                new QuestionSpec("What does a Mongoose Schema define?",
                    "The structure, field types, validations, and defaults for documents in a MongoDB collection",
                    "The MongoDB server connection settings",
                    "The aggregation pipeline stages",
                    "The index configuration for a collection", 0, "Mongoose Schema enforces typed fields (String, Number, Date), required rules, and custom validators."),
                new QuestionSpec("Which MongoDB aggregation stage filters documents by a condition?",
                    "$match", "$group", "$project", "$sort", 0, "$match (like SQL WHERE) filters documents early in the pipeline to reduce data processed by later stages."),
                new QuestionSpec("What does the MongoDB $group stage compute?",
                    "Groups documents by a key and applies accumulators like $sum, $avg, $count, $push",
                    "Sorts documents by a field",
                    "Joins two collections together",
                    "Filters out null fields", 0, "$group is equivalent to SQL GROUP BY with aggregate functions — e.g. {_id: '$dept', total: {$sum: '$salary'}}."),
                new QuestionSpec("What is the purpose of db.collection.deleteMany({}) in MongoDB?",
                    "Deletes all documents in the collection that match the filter (empty filter deletes all)",
                    "Drops the entire collection including its indexes",
                    "Archives documents to a backup collection",
                    "Removes the collection from the database", 0, "deleteMany({}) with an empty filter removes every document; drop() removes the collection itself."),
                new QuestionSpec("How do you create an index on the 'email' field in MongoDB?",
                    "db.users.createIndex({email: 1})",
                    "db.users.addIndex('email')",
                    "db.users.index({field: 'email'})",
                    "db.users.ensureIndex({email: true})", 0, "createIndex({field: 1}) creates ascending; {field: -1} creates descending; {field: 1}, {unique: true} enforces uniqueness.")
            );
        }

        // ── ORDER BY, LIMIT & Subqueries ─────────────────────────────────────
        if (key.contains("order-by") || key.contains("limit") || (key.contains("subquer") && !key.contains("cte"))) {
            return List.of(
                new QuestionSpec("What does ORDER BY column DESC do in SQL?",
                    "Sorts the result set by the column from highest to lowest (descending)",
                    "Sorts ascending by default", "Removes duplicate rows", "Groups rows by column", 0,
                    "ASC (default) sorts lowest to highest; DESC sorts highest to lowest."),
                new QuestionSpec("What does LIMIT 5 OFFSET 10 do in a SQL query?",
                    "Skips the first 10 rows and returns the next 5 rows",
                    "Returns the first 5 rows", "Returns rows 5 through 10", "Sorts by 5 then 10", 0,
                    "OFFSET skips rows; LIMIT caps results — used for pagination."),
                new QuestionSpec("What is a subquery in SQL?",
                    "A query nested inside another query (SELECT, INSERT, UPDATE, or DELETE)",
                    "A stored procedure called by a main query", "A view used as a table", "A JOIN between two queries", 0,
                    "Subqueries can appear in SELECT, WHERE, FROM, or HAVING clauses."),
                new QuestionSpec("What does SELECT * FROM (SELECT id, name FROM users) AS sub do?",
                    "Selects from an inline derived table (subquery in FROM clause)",
                    "Creates a permanent view called sub", "Deletes the users table", "Renames the users table", 0,
                    "A subquery in the FROM clause is called a derived table and must be aliased."),
                new QuestionSpec("Which SQL clause is used to return only the top 3 highest-paid employees?",
                    "ORDER BY salary DESC LIMIT 3",
                    "WHERE salary = MAX(salary) LIMIT 3", "HAVING salary DESC LIMIT 3", "GROUP BY salary TOP 3", 0,
                    "Sorting descending by salary and limiting to 3 rows gives the top 3 earners."),
                new QuestionSpec("What does the IN operator do in a WHERE clause?",
                    "Checks if a value matches any value in a specified list or subquery",
                    "Checks range between two values", "Counts matching rows", "Joins two tables", 0,
                    "WHERE dept IN ('HR','IT','Finance') is shorthand for multiple OR conditions."),
                new QuestionSpec("What is the difference between a scalar subquery and a table subquery?",
                    "A scalar subquery returns exactly one row and one column; a table subquery returns multiple rows/columns",
                    "A scalar subquery is faster by definition", "They are identical in SQL",
                    "Table subqueries can only appear in FROM", 0,
                    "Scalar subqueries are used in SELECT or WHERE; table subqueries are used in FROM as derived tables."),
                new QuestionSpec("What does ORDER BY 2 mean in SQL (using positional notation)?",
                    "Sort by the second column in the SELECT list",
                    "Sort by the column named '2'", "Limit results to 2 rows", "Skip 2 rows", 0,
                    "Positional ORDER BY references column position in the SELECT list (1-based index).")
            );
        }

        // ── ACID Transactions & Constraints ──────────────────────────────────
        if (key.contains("acid") || key.contains("transaction") || (key.contains("constraint") && key.contains("acid"))) {
            return List.of(
                new QuestionSpec("What does ACID stand for in database transactions?",
                    "Atomicity, Consistency, Isolation, Durability",
                    "Availability, Consistency, Integrity, Durability",
                    "Atomicity, Concurrency, Indexing, Distribution",
                    "Authorization, Consistency, Isolation, Data", 0,
                    "ACID guarantees reliable transaction processing even in the face of errors and crashes."),
                new QuestionSpec("What does the Atomicity property of ACID guarantee?",
                    "A transaction either completes fully or is fully rolled back — no partial updates",
                    "All transactions execute in parallel", "Data is always consistent across tables",
                    "Concurrent transactions never interfere", 0,
                    "If any step in a transaction fails, all changes are rolled back as if nothing happened."),
                new QuestionSpec("What SQL command saves all changes made in a transaction permanently?",
                    "COMMIT", "ROLLBACK", "SAVEPOINT", "BEGIN", 0,
                    "COMMIT makes all transaction changes durable; ROLLBACK undoes them."),
                new QuestionSpec("What does ROLLBACK do in a SQL transaction?",
                    "Undoes all changes made in the current transaction back to the last COMMIT or SAVEPOINT",
                    "Permanently saves partial changes", "Deletes the table", "Restarts the database server", 0,
                    "ROLLBACK is used when an error occurs to ensure no partial data is committed."),
                new QuestionSpec("What does the Isolation property in ACID protect against?",
                    "Concurrent transactions interfering with each other's intermediate state",
                    "Power failures corrupting data", "Schema changes breaking queries",
                    "Unauthorized access to data", 0,
                    "Isolation ensures transactions appear to run sequentially, preventing dirty reads and phantom reads."),
                new QuestionSpec("Which SQL constraint ensures a column value uniquely identifies each row?",
                    "PRIMARY KEY", "UNIQUE", "NOT NULL", "FOREIGN KEY", 0,
                    "PRIMARY KEY enforces both UNIQUE and NOT NULL, and is used as the main row identifier."),
                new QuestionSpec("What is a SAVEPOINT used for in SQL transactions?",
                    "Creating a named checkpoint within a transaction to allow partial rollback",
                    "Saving the database to disk", "Committing partial results", "Locking a table temporarily", 0,
                    "SAVEPOINT sp1 lets you ROLLBACK TO sp1 without undoing the entire transaction."),
                new QuestionSpec("What does the Durability property of ACID ensure?",
                    "Once a transaction is committed, its changes survive system crashes (written to disk/WAL)",
                    "Data is replicated to multiple servers", "Transactions always complete within 1 second",
                    "Committed data is encrypted automatically", 0,
                    "Durability is achieved through write-ahead logs (WAL) and fsync in PostgreSQL.")
            );
        }

        // ── JSONB & Full-Text Search ──────────────────────────────────────────
        if (key.contains("jsonb") || key.contains("full-text") || key.contains("json") && key.contains("search")) {
            return List.of(
                new QuestionSpec("What is JSONB in PostgreSQL?",
                    "A binary-encoded JSON format that supports indexing and fast key lookups",
                    "A JSON type that stores data as plain text", "A JSON type only for arrays",
                    "An external JSON file format", 0,
                    "JSONB parses JSON at write time into binary, enabling GIN indexes for fast querying."),
                new QuestionSpec("Which PostgreSQL operator retrieves a JSON field value as text?",
                    "->> (returns text value)", "-> (returns JSON value)", "# (direct access)", ":: (casts type)", 0,
                    "col->'key' returns JSON; col->>'key' returns the value as plain text."),
                new QuestionSpec("Which index type is most efficient for JSONB queries in PostgreSQL?",
                    "GIN (Generalized Inverted Index)", "B-tree", "Hash", "BRIN", 0,
                    "GIN indexes all keys and values inside JSONB, enabling fast @>, ?, ?| operators."),
                new QuestionSpec("What does the @> operator do in PostgreSQL JSONB?",
                    "Tests if the left JSONB value contains the right JSONB value (containment check)",
                    "Appends JSON to a column", "Extracts a nested JSON field", "Compares two JSON sizes", 0,
                    "data @> '{\"status\":\"active\"}' returns rows where the JSON contains that key-value pair."),
                new QuestionSpec("What function is used for full-text search in PostgreSQL?",
                    "to_tsvector() and to_tsquery() with the @@ match operator",
                    "LIKE '%keyword%'", "CONTAINS(column, 'keyword')", "MATCH AGAINST in BOOLEAN MODE", 0,
                    "to_tsvector converts text to a searchable vector; to_tsquery creates a query; @@ matches them."),
                new QuestionSpec("What does to_tsvector('english', 'Fast running dogs') return?",
                    "A normalized lexeme vector: 'dog':3 'fast':1 'run':2",
                    "The exact string 'Fast running dogs'", "A hash of the sentence", "An array of words", 0,
                    "tsvector normalizes words (stemming), removes stop words, and stores positions."),
                new QuestionSpec("What is the advantage of a GIN index on a tsvector column?",
                    "Full-text searches become O(log n) lookups instead of O(n) sequential scans",
                    "Reduces storage size of text columns", "Enables LIKE queries with wildcards",
                    "Automatically translates between languages", 0,
                    "GIN on tsvector maps each lexeme to the rows containing it for fast full-text search."),
                new QuestionSpec("How do you update a specific JSONB field without replacing the whole document in PostgreSQL?",
                    "jsonb_set(column, '{key}', 'new_value')",
                    "UPDATE table SET column = '{\"key\": \"value\"}'",
                    "PATCH column SET key = 'value'", "column->>'key' = 'new_value'", 0,
                    "jsonb_set(data, '{address,city}', '\"London\"') updates a nested field non-destructively.")
            );
        }

        // ── EXPLAIN ANALYZE & Query Planning ─────────────────────────────────
        if (key.contains("explain") || key.contains("query-plan") || key.contains("query-planning")) {
            return List.of(
                new QuestionSpec("What is the difference between EXPLAIN and EXPLAIN ANALYZE in PostgreSQL?",
                    "EXPLAIN shows estimated plan without executing; EXPLAIN ANALYZE actually runs the query and shows real timings",
                    "They are identical", "ANALYZE creates statistics; EXPLAIN shows indexes",
                    "EXPLAIN ANALYZE only works on SELECT queries", 0,
                    "EXPLAIN ANALYZE runs the query and shows actual rows, loops, and time — crucial for optimization."),
                new QuestionSpec("What does 'Seq Scan' mean in a PostgreSQL EXPLAIN output?",
                    "A sequential scan reading every row in the table (no index used)",
                    "A scan using a secondary index", "A parallel scan using multiple CPUs",
                    "A scan on a sorted table", 0,
                    "Seq Scan on large tables is slow; adding an index converts it to an Index Scan."),
                new QuestionSpec("What does the 'cost' estimate in EXPLAIN output represent?",
                    "An abstract unit estimating I/O and CPU work: startup_cost..total_cost",
                    "Time in milliseconds to complete the query", "Memory in megabytes used",
                    "Number of rows returned", 0,
                    "EXPLAIN cost is relative — lower total cost means a more efficient plan."),
                new QuestionSpec("What query hint forces PostgreSQL to use a specific index?",
                    "PostgreSQL has no direct hint — use SET enable_seqscan=off to discourage seq scans",
                    "USE INDEX (index_name)", "FORCE INDEX (index_name)", "WITH (INDEX=index_name)", 0,
                    "Unlike MySQL, PostgreSQL uses the planner's cost model; disable_seqscan forces index use in testing."),
                new QuestionSpec("What does 'rows=1000 (actual rows=50000)' in EXPLAIN ANALYZE indicate?",
                    "The planner underestimated rows — statistics may be stale; run ANALYZE to update",
                    "The query returned wrong results", "There is a bug in the query planner",
                    "50000 rows were deleted during the query", 0,
                    "Large estimation errors cause bad plans; ANALYZE updates statistics so the planner makes better decisions."),
                new QuestionSpec("What does VACUUM ANALYZE do in PostgreSQL?",
                    "Reclaims storage from dead tuples and updates planner statistics for all columns",
                    "Drops all indexes and rebuilds them", "Restores the database from a backup",
                    "Clears the shared memory buffer cache", 0,
                    "Dead tuples from UPDATE/DELETE accumulate; VACUUM reclaims space; ANALYZE refreshes row statistics."),
                new QuestionSpec("What does a Hash Join mean in PostgreSQL EXPLAIN output?",
                    "One table is hashed into memory; the other is scanned and probed against the hash table",
                    "Both tables are sorted and merged", "One table is fully scanned for each row of the other",
                    "An index is used to join both tables", 0,
                    "Hash Join is efficient when one side fits in work_mem; Merge Join uses sorted inputs."),
                new QuestionSpec("What does SET work_mem = '256MB' affect in PostgreSQL?",
                    "The memory available per sort/hash operation — larger values enable in-memory sorts instead of disk spills",
                    "Total RAM allocated to PostgreSQL", "Size of the shared buffer cache",
                    "Maximum connection count", 0,
                    "Insufficient work_mem causes sort and hash operations to spill to disk, slowing queries.")
            );
        }

        // ── MongoDB: Indexing Strategies & Performance ────────────────────────
        if (key.contains("indexing-strateg") || (key.contains("index") && key.contains("perform") && key.contains("strateg"))) {
            return List.of(
                new QuestionSpec("Why are indexes important in MongoDB?",
                    "They allow queries to scan a small index structure instead of every document (collection scan)",
                    "They compress documents to reduce storage", "They enforce schema validation",
                    "They replicate data to other nodes", 0,
                    "Without an index, MongoDB performs a full collection scan — O(n). Indexes reduce this to O(log n)."),
                new QuestionSpec("What does db.collection.createIndex({name: 1}) create?",
                    "An ascending B-tree index on the 'name' field",
                    "A descending index on 'name'", "A compound index on all fields",
                    "A text index for full-text search", 0,
                    "1 = ascending, -1 = descending. Index direction matters for sort queries."),
                new QuestionSpec("What is a compound index in MongoDB?",
                    "An index on multiple fields, e.g. createIndex({dept: 1, salary: -1})",
                    "An index combining two collections", "An index on embedded array fields",
                    "A unique index on the _id field", 0,
                    "Compound indexes support queries on prefixes — {dept, salary} also supports queries on just {dept}."),
                new QuestionSpec("What is a sparse index in MongoDB?",
                    "An index that only includes documents where the indexed field exists (skips documents with null/missing field)",
                    "An index with low cardinality", "An index stored in compressed form",
                    "An index that covers only 10% of documents randomly", 0,
                    "Sparse indexes are useful for optional fields to avoid indexing millions of null values."),
                new QuestionSpec("What does the explain() method return in MongoDB?",
                    "The query execution plan showing whether an index was used (IXSCAN vs COLLSCAN)",
                    "The document count in the collection", "The index definitions only",
                    "The query as a JSON string", 0,
                    "db.col.find({}).explain('executionStats') shows IXSCAN (index scan) or COLLSCAN (full scan)."),
                new QuestionSpec("What is a covered query in MongoDB?",
                    "A query where all required fields are in the index, so no document fetch is needed",
                    "A query with a WHERE clause", "A query that uses aggregation",
                    "A query run with elevated privileges", 0,
                    "Covered queries are the fastest — all data comes from the index without touching documents."),
                new QuestionSpec("What is the ESR rule for MongoDB compound indexes?",
                    "Equality fields first, Sort fields second, Range fields last for optimal index usage",
                    "Equality, Schema, Range ordering", "Exact, Sparse, Redundant ordering",
                    "Equal, Sorted, Ranked ordering", 0,
                    "ESR (Equality-Sort-Range) maximizes index efficiency by placing equality predicates at the front."),
                new QuestionSpec("What command lists all indexes on a MongoDB collection?",
                    "db.collection.getIndexes()", "db.collection.listIndexes()",
                    "db.collection.showIndexes()", "db.collection.indexes()", 0,
                    "getIndexes() returns an array of all index documents including _id index and any custom indexes.")
            );
        }

        // ── MongoDB: Mongoose ODM & Schema Patterns ───────────────────────────
        if (key.contains("mongoose") || key.contains("odm") || key.contains("schema-pattern")) {
            return List.of(
                new QuestionSpec("What is the purpose of defining a Schema in Mongoose?",
                    "To enforce structure, data types, validations, and defaults on MongoDB documents",
                    "To configure the MongoDB connection string", "To create database indexes automatically",
                    "To define REST API endpoints", 0,
                    "Mongoose Schema adds type safety and validation on top of MongoDB's schema-free documents."),
                new QuestionSpec("How do you define a required String field in a Mongoose Schema?",
                    "{ name: { type: String, required: true } }",
                    "{ name: String.required }", "{ name: 'String', required: 1 }",
                    "{ name: required(String) }", 0,
                    "Mongoose validators run before saving — required: true throws a ValidationError if the field is missing."),
                new QuestionSpec("What does mongoose.model('User', userSchema) do?",
                    "Creates a Model class mapped to the 'users' collection with the given schema",
                    "Creates a new database called 'User'", "Creates a REST endpoint for users",
                    "Runs the schema validation immediately", 0,
                    "Mongoose pluralizes the model name ('User' → 'users') to determine the collection name."),
                new QuestionSpec("How do you find all documents where age > 25 using Mongoose?",
                    "User.find({ age: { $gt: 25 } })",
                    "User.where('age').greaterThan(25)", "User.query({ age: '>25' })",
                    "User.select({ age: { gt: 25 } })", 0,
                    "Mongoose passes MongoDB query operators like $gt, $lt, $in directly to the driver."),
                new QuestionSpec("What does the populate() method do in Mongoose?",
                    "Replaces a referenced ObjectId with the actual document from the referenced collection (like a JOIN)",
                    "Fills in default values for empty fields", "Creates a new document with random data",
                    "Runs all pre-save hooks", 0,
                    "populate('author') fetches the Author document and replaces the ObjectId reference automatically."),
                new QuestionSpec("What is a Mongoose virtual field?",
                    "A computed property that exists on the model but is not stored in MongoDB",
                    "A field with a default value", "An indexed field for fast lookup",
                    "A field synchronized with another collection", 0,
                    "Virtuals like fullName (firstName + lastName) are computed at runtime and not persisted to the database."),
                new QuestionSpec("What is the Embedding vs Referencing pattern in MongoDB schema design?",
                    "Embedding stores related data in the same document; referencing stores an ObjectId pointing to another collection",
                    "Embedding is for arrays; referencing is for strings",
                    "They are identical in performance", "Referencing is not supported in Mongoose", 0,
                    "Embed for 1-to-few with frequent reads; reference for 1-to-many or shared data across collections."),
                new QuestionSpec("What does the Mongoose pre('save') hook do?",
                    "Executes middleware logic before a document is saved to MongoDB (e.g. hashing passwords)",
                    "Validates the document schema after saving", "Runs after a find() query",
                    "Automatically creates an index before saving", 0,
                    "Middleware hooks (pre/post) are used for password hashing, audit timestamps, and data transformation.")
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
