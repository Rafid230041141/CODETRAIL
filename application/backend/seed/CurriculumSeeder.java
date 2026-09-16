package application.backend.seed;

import application.backend.domain.CourseModule;
import application.backend.domain.Lesson;
import application.backend.domain.QuizQuestion;
import application.backend.domain.Role;
import application.backend.domain.Simulation;
import application.backend.domain.Submodule;
import application.backend.domain.Topic;
import application.backend.domain.UserAccount;
import application.backend.repository.TopicRepository;
import application.backend.repository.LessonRepository;
import application.backend.repository.UserAccountRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Seeds the small, useful learning catalogue used by the first two phases.
 * Content is deliberately created in Java so a fresh H2 or PostgreSQL database
 * has the same tree without depending on a database-specific import script. */
@Component
public class CurriculumSeeder implements CommandLineRunner {
    private final TopicRepository topics;
    private final LessonRepository lessons;
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final application.backend.repository.LessonProgressRepository lessonProgress;
    private final application.backend.repository.QuizAttemptRepository quizAttempts;

    public CurriculumSeeder(TopicRepository topics, LessonRepository lessons, UserAccountRepository users,
            PasswordEncoder passwordEncoder,
            application.backend.repository.LessonProgressRepository lessonProgress,
            application.backend.repository.QuizAttemptRepository quizAttempts) {
        this.topics = topics;
        this.lessons = lessons;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.lessonProgress = lessonProgress;
        this.quizAttempts = quizAttempts;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedContent();
        backfillDsaSimulations();
        backfillLessonContent();
        backfillQuizQuestions();
        backfillWebDevCurriculum();
        backfillAppDevCurriculum();
        backfillLanguagesCurriculum();
        backfillAiMlCurriculum();
        backfillDataScienceCurriculum();
        backfillGameDevCurriculum();
        seedDemoUser("admin", "Administrator", "admin123", Role.ADMIN);
        seedDemoUser("student", "Demo Student", "student123", Role.STUDENT);
    }

    /**
     * Existing installations already have lessons and progress, so their
     * simulation rows are updated in place instead of replacing any lesson
     * or user-owned progress data.
     */
    private void backfillDsaSimulations() {
        for (Lesson lesson : lessons.findAll()) {
            SimulationSpec simulation = simulationFor(lesson.getTitle());
            if (simulation == null) {
                continue;
            }
            if (lesson.getSimulation() == null) {
                lesson.setSimulation(new Simulation(lesson, simulation.type(), simulation.configJson()));
            } else {
                lesson.getSimulation().setType(simulation.type());
                lesson.getSimulation().setConfigJson(simulation.configJson());
            }
            lessons.save(lesson);
        }
    }

    private void backfillLessonContent() {
        for (Lesson lesson : lessons.findAll()) {
            String key = slug(lesson.getTitle());
            if ("array".equals(key)) {
                lesson.setBodyMarkdown(arrayMarkdown());
                lesson.setExampleCode(arrayExampleCode());
                lesson.setSummary("Master arrays: contiguous memory, zero-based indexing, looping, and calculating sums.");
                lessons.save(lesson);
                System.out.println("[CodeTrail] Updated database lesson content for: " + lesson.getTitle());
            } else if ("linked-list".equals(key)) {
                lesson.setBodyMarkdown(linkedListMarkdown());
                lesson.setExampleCode(linkedListExampleCode());
                lesson.setSummary("Master linked lists: node structure, dynamic allocation, head & tail pointers, insertions, and deletions.");
                lessons.save(lesson);
                System.out.println("[CodeTrail] Updated database lesson content for: " + lesson.getTitle());
            }
        }
    }

    private void backfillQuizQuestions() {
        for (Lesson lesson : lessons.findAll()) {
            if (lesson.getQuizQuestions().size() < 8) {
                String topicTitle = (lesson.getSubmodule() != null && lesson.getSubmodule().getModule() != null && lesson.getSubmodule().getModule().getTopic() != null)
                        ? lesson.getSubmodule().getModule().getTopic().getTitle() : "Course Curriculum";
                String moduleTitle = (lesson.getSubmodule() != null && lesson.getSubmodule().getModule() != null)
                        ? lesson.getSubmodule().getModule().getTitle() : "Core Foundations";
                String subTitle = lesson.getSubmodule() != null ? lesson.getSubmodule().getTitle() : "Concepts";
                List<QuizBankSeeder.QuestionSpec> questions = QuizBankSeeder.getQuestionsForLesson(topicTitle, moduleTitle, subTitle, lesson.getTitle());
                lesson.getQuizQuestions().clear();
                for (int i = 0; i < questions.size(); i++) {
                    QuizBankSeeder.QuestionSpec q = questions.get(i);
                    lesson.getQuizQuestions().add(new QuizQuestion(lesson, i + 1, q.prompt(), q.a(), q.b(), q.c(), q.d(), q.correct(), q.explanation()));
                }
                lessons.save(lesson);
            }
        }
    }

    private void upgradeTopicCurriculum(String topicSlug, String firstModuleKeyword, java.util.function.Consumer<Topic> seeder) {
        java.util.Optional<Topic> topicOpt = topics.findBySlugIgnoreCase(topicSlug);
        if (topicOpt.isEmpty()) {
            return;
        }
        Topic topic = topicOpt.get();
        boolean alreadyUpgraded = topic.getModules().stream()
                .anyMatch(m -> m.getTitle().toLowerCase(Locale.ROOT).contains(firstModuleKeyword.toLowerCase(Locale.ROOT)));
        if (alreadyUpgraded) {
            return;
        }
        System.out.println("[CodeTrail] Upgrading curriculum for topic: " + topic.getTitle() + "...");
        Long topicId = topic.getId();
        java.util.List<application.backend.domain.LessonProgress> oldProgress = lessonProgress.findAll().stream()
                .filter(p -> p.getLesson() != null
                        && p.getLesson().getSubmodule() != null
                        && p.getLesson().getSubmodule().getModule() != null
                        && p.getLesson().getSubmodule().getModule().getTopic() != null
                        && topicId.equals(p.getLesson().getSubmodule().getModule().getTopic().getId()))
                .toList();
        if (!oldProgress.isEmpty()) {
            lessonProgress.deleteAll(oldProgress);
        }

        java.util.List<application.backend.domain.QuizAttempt> oldAttempts = quizAttempts.findAll().stream()
                .filter(q -> q.getLesson() != null
                        && q.getLesson().getSubmodule() != null
                        && q.getLesson().getSubmodule().getModule() != null
                        && q.getLesson().getSubmodule().getModule().getTopic() != null
                        && topicId.equals(q.getLesson().getSubmodule().getModule().getTopic().getId()))
                .toList();
        if (!oldAttempts.isEmpty()) {
            quizAttempts.deleteAll(oldAttempts);
        }

        topic.getModules().clear();
        topics.saveAndFlush(topic);
        seeder.accept(topic);
        topics.save(topic);
        System.out.println("[CodeTrail] Curriculum upgraded successfully for: " + topic.getTitle());
    }

    private void backfillWebDevCurriculum() {
        upgradeTopicCurriculum("web-development", "html5", this::seedWebDev);
    }

    private void backfillAppDevCurriculum() {
        upgradeTopicCurriculum("app-development", "flutter", this::seedAppDev);
    }

    private void backfillLanguagesCurriculum() {
        upgradeTopicCurriculum("languages", "python 3", this::seedLanguages);
    }

    private void backfillAiMlCurriculum() {
        upgradeTopicCurriculum("ai-ml", "foundations", this::seedAiMl);
    }

    private void backfillDataScienceCurriculum() {
        upgradeTopicCurriculum("data-science", "numpy", this::seedDataScience);
    }

    private void backfillGameDevCurriculum() {
        upgradeTopicCurriculum("game-development", "game math", this::seedGameDev);
    }

    private void seedDemoUser(String username, String displayName, String password, Role role) {
        if (!users.existsByUsernameIgnoreCase(username)) {
            users.save(new UserAccount(username, displayName, passwordEncoder.encode(password), role));
        }
    }

    private void seedContent() {
        if (!topics.existsBySlugIgnoreCase("languages")) {
            Topic languages = new Topic("languages", "Languages", "Practical programming language foundations, syntax, OOP, and tools.", 1, true);
            seedLanguages(languages);
            topics.save(languages);
        }

        if (!topics.existsBySlugIgnoreCase("dsa-competitive-programming")) {
            Topic dsa = new Topic("dsa-competitive-programming", "DSA / Competitive Programming",
                    "Core data structures and algorithms with implementation-oriented lessons.", 2, true);
            seedDsa(dsa);
            topics.save(dsa);
        }

        if (!topics.existsBySlugIgnoreCase("web-development")) {
            Topic web = new Topic("web-development", "Web Development",
                    "Build modern web apps, APIs, frontend frameworks, and server architectures.", 3, true);
            seedWebDev(web);
            topics.save(web);
        }

        if (!topics.existsBySlugIgnoreCase("app-development")) {
            Topic app = new Topic("app-development", "App Development",
                    "Design and build cross-platform mobile and desktop applications.", 4, true);
            seedAppDev(app);
            topics.save(app);
        }

        if (!topics.existsBySlugIgnoreCase("ai-ml")) {
            Topic aiml = new Topic("ai-ml", "AI / ML",
                    "Machine learning foundations, neural networks, and modern AI models.", 5, true);
            seedAiMl(aiml);
            topics.save(aiml);
        }

        if (!topics.existsBySlugIgnoreCase("data-science")) {
            Topic ds = new Topic("data-science", "Data Science",
                    "Data analysis, statistical modeling, visualization, and manipulation libraries.", 6, true);
            seedDataScience(ds);
            topics.save(ds);
        }

        if (!topics.existsBySlugIgnoreCase("game-development")) {
            Topic game = new Topic("game-development", "Game Development",
                    "Create interactive 2D and 3D games using modern game engines and mechanics.", 7, true);
            seedGameDev(game);
            topics.save(game);
        }
    }

    private void seedLanguages(Topic topic) {
        module(topic, "Python 3 & Scripting", new String[][]{
                {"Python Foundations", "Python Syntax & Data Types", "Control Flow & Functions"},
                {"Python Advanced", "OOP & Decorators", "Virtual Environments & Packages"}
        });
        module(topic, "Java Enterprise OOP", new String[][]{
                {"Core Java", "OOP & Class Design", "Collections Framework"},
                {"Java Modern", "Streams API & Lambdas", "Multithreading & Concurrency"}
        });
        module(topic, "C++ Modern Systems", new String[][]{
                {"C++ Core", "Pointers & References", "RAII & Smart Pointers"},
                {"Modern C++", "STL Containers & Algorithms", "Templates & Modern Patterns"}
        });
        module(topic, "C Systems Programming", new String[][]{
                {"C Foundations", "Pointers & Memory Allocation", "Stack vs Heap Mechanics"},
                {"Low-Level C", "Structs & Bitwise Operations", "Systems & File I/O"}
        });
        module(topic, "TypeScript & Modern JS", new String[][]{
                {"TypeScript Core", "Static Typing & Interfaces", "Generics & Unions"},
                {"TypeScript Applied", "Modules & Build Tooling"}
        });
        module(topic, "Rust Memory Safety", new String[][]{
                {"Rust Core", "Ownership & Borrowing", "Lifetimes & References"},
                {"Rust Systems", "Pattern Matching & Cargo"}
        });
        module(topic, "Go (Golang) Microservices", new String[][]{
                {"Go Foundations", "Goroutines & Channels", "Interfaces & Structs"},
                {"Go Services", "HTTP Standard Library & APIs"}
        });
        module(topic, "SQL & Relational Modeling", new String[][]{
                {"SQL Foundations", "Relational Schemas & SELECT", "Table Joins & Grouping"},
                {"Advanced SQL", "Transactions & Constraints"}
        });
    }

    private void seedWebDev(Topic topic) {
        module(topic, "HTML5 & Semantic Web", new String[][]{
                {"HTML Foundations", "HTML5 Document Anatomy", "Semantic Elements & Layout"},
                {"Forms & Media", "Forms, Inputs & Validation", "Media, Tables & Accessibility"}
        });
        module(topic, "CSS3 & Modern Styling", new String[][]{
                {"CSS Core", "The CSS Box Model", "Positioning & Specificity"},
                {"Modern Layouts", "Flexbox Layout System", "CSS Grid Architecture"},
                {"Responsive & Frameworks", "Responsive Design & Media Queries", "Bootstrap 5 & Tailwind CSS"}
        });
        module(topic, "JavaScript & DOM Manipulation", new String[][]{
                {"Core JavaScript", "Variables, Data Types & Control Flow", "Functions, Scope & Closures", "Arrays, Objects & Destructuring"},
                {"DOM & Browser Events", "DOM Selection & Manipulation", "Event Handling & Bubbling"},
                {"Async JavaScript", "Promises, Async/Await & Fetch API"}
        });
        module(topic, "Frontend Framework: React.js", new String[][]{
                {"React Fundamentals", "JSX & React Components", "Props & Component Composition"},
                {"State & Lifecycle", "State Management with useState", "Side Effects with useEffect"},
                {"Advanced React", "React Router & Single Page Apps"}
        });
        module(topic, "Backend Engineering: Node.js & Express", new String[][]{
                {"Node.js Runtime", "Node.js Architecture & Event Loop", "Built-in Node Modules & NPM"},
                {"Express Server", "Express Server & Routing", "Middleware & RESTful API Design"}
        });
        module(topic, "Databases: PostgreSQL & MongoDB", new String[][]{
                {"Relational Databases (SQL)", "PostgreSQL Foundations & Schemas", "CRUD Queries & Table Joins"},
                {"NoSQL (MongoDB)", "MongoDB & Document Storage", "Mongoose ORM & Data Validation"}
        });
        module(topic, "Authentication & Web Security", new String[][]{
                {"User Authentication", "Password Hashing with Bcrypt", "Sessions, Cookies & JWT", "OAuth 2.0 & Google Sign-In"},
                {"Web Security", "Web Security & OWASP Top 10"}
        });
        module(topic, "Full-Stack Projects & Cloud Deployment", new String[][]{
                {"DevOps & Version Control", "Git & GitHub Version Control", "Environment Variables & Secrets"},
                {"Cloud Hosting", "Cloud Deployment with Vercel & Render"}
        });
    }

    private void seedAppDev(Topic topic) {
        module(topic, "Flutter & Dart Foundations", new String[][]{
                {"Dart Language Basics", "Dart Syntax & Types", "Control Flow & Functions"},
                {"Flutter Core & Widgets", "Stateless vs Stateful Widgets", "Layouts & UI Trees"}
        });
        module(topic, "React Native & Expo Mobile", new String[][]{
                {"React Native Basics", "Expo Tooling & Project Setup", "Core Components & Flexbox"},
                {"Mobile Navigation & APIs", "Stack & Tab Navigation", "Handling Touch & Gestures"}
        });
        module(topic, "Native Android: Kotlin & Compose", new String[][]{
                {"Kotlin for Android", "Kotlin Syntax & Coroutines", "Android Studio & Gradle"},
                {"Jetpack Compose", "Compose Declarative UI", "ViewModels & StateFlow"}
        });
        module(topic, "Native iOS: Swift & SwiftUI", new String[][]{
                {"Swift Language Core", "Swift Syntax & Optionals", "Object-Oriented & Protocol Swift"},
                {"SwiftUI Architecture", "Views, Modifiers & Layouts", "State, Binding & Environment"}
        });
        module(topic, "Mobile State Management", new String[][]{
                {"Architecture Patterns", "State Architecture & MVVM", "Clean Architecture for Mobile"},
                {"State Management Libraries", "Flutter BLoC & Riverpod"}
        });
        module(topic, "Mobile APIs & Firebase", new String[][]{
                {"HTTP & REST Clients", "REST APIs with Retrofit & Dio", "JSON Serialization & Caching"},
                {"Firebase Cloud Services", "Firebase Authentication & Firestore"}
        });
        module(topic, "Local Storage: SQLite & Room", new String[][]{
                {"Relational Mobile DB", "SQLite & Room Database Setup", "DAOs, Entities & Queries"},
                {"Key-Value & Offline Stores", "Encrypted SharedPreferences & Realm"}
        });
        module(topic, "Deployment: App Store & Play Store", new String[][]{
                {"Build & Signing", "Android Keystores & Play Console", "iOS Certificates & Provisioning"},
                {"Release & CI/CD", "TestFlight & Fastlane Pipelines"}
        });
    }

    private void seedAiMl(Topic topic) {
        module(topic, "Machine Learning Foundations", new String[][]{
                {"Supervised Learning", "Linear & Logistic Regression", "Decision Trees & Ensembles"},
                {"Evaluation", "Overfitting & Regularization", "Train Test Splitting"}
        });
        module(topic, "Math for Machine Learning", new String[][]{
                {"Linear Algebra", "Matrix Multiplications", "Eigenvalues & Vectors"},
                {"Calculus & Optimization", "Partial Derivatives", "Gradient Descent Mechanics"}
        });
        module(topic, "Applied ML with Scikit-learn", new String[][]{
                {"Data Preprocessing", "Imputation & One-Hot Encoding", "Feature Scaling Pipelines"},
                {"Model Validation", "Cross-Validation & Grid Search", "ROC-AUC & Classification Metrics"}
        });
        module(topic, "Deep Learning & Neural Networks", new String[][]{
                {"Neural Foundations", "Perceptrons & Activation Functions", "Backpropagation Calculus"},
                {"PyTorch Framework", "PyTorch Tensors & Autograd", "Optimizers & Training Loops"}
        });
        module(topic, "Computer Vision & CNNs", new String[][]{
                {"Image Processing", "OpenCV Image Filters", "Convolution & Pooling Layers"},
                {"Advanced Vision", "Transfer Learning & YOLO"}
        });
        module(topic, "Natural Language Processing", new String[][]{
                {"Text Processing", "Tokenization & Embeddings", "Self-Attention Mechanism"},
                {"Transformers", "Transformer Architecture & HuggingFace"}
        });
        module(topic, "Generative AI & LLMs", new String[][]{
                {"Prompt Engineering", "Prompt Design Patterns", "RAG & Vector Databases"},
                {"LLM Integration", "Fine-Tuning & LLM APIs"}
        });
        module(topic, "MLOps & Cloud Deployment", new String[][]{
                {"Model Serving", "FastAPI Inference Endpoints", "Docker Containers for ML"},
                {"Lifecycle Management", "MLflow Tracking & Monitoring"}
        });
    }

    private void seedDataScience(Topic topic) {
        module(topic, "Numerical Computing with NumPy", new String[][]{
                {"NumPy Core", "N-Dimensional Arrays & Slicing", "Broadcasting & Vectorization"},
                {"Scientific Computing", "Matrix Operations & Linear Algebra", "SciPy Optimization Routines"}
        });
        module(topic, "Data Wrangling with Pandas", new String[][]{
                {"Pandas Core", "Series & DataFrames", "Handling Missing Data & Outliers"},
                {"Aggregation & Joins", "GroupBy & Pivot Tables", "Merging & Concatenation"}
        });
        module(topic, "Exploratory Data Analysis", new String[][]{
                {"Visualization Core", "Matplotlib Foundations", "Seaborn Statistical Plots"},
                {"Interactive EDA", "Correlation Heatmaps", "Plotly Interactive Charts"}
        });
        module(topic, "Statistics & Hypothesis Testing", new String[][]{
                {"Probability Foundations", "Probability Distributions", "Central Limit Theorem"},
                {"Inference", "Confidence Intervals", "A/B Testing & p-Values"}
        });
        module(topic, "Feature Engineering & PCA", new String[][]{
                {"Feature Transforms", "Scaling & Log Transforms", "Categorical Encoding Techniques"},
                {"Dimensionality Reduction", "Principal Component Analysis"}
        });
        module(topic, "Big Data with Apache Spark", new String[][]{
                {"Spark Architecture", "Distributed Cluster Computing", "PySpark DataFrames"},
                {"Spark Operations", "Transformations & Spark SQL"}
        });
        module(topic, "SQL for Analytics & Warehouses", new String[][]{
                {"Analytical SQL", "Window Functions (RANK/LAG)", "Common Table Expressions"},
                {"Data Warehousing", "Star & Snowflake Schema Design"}
        });
        module(topic, "BI & Interactive Dashboards", new String[][]{
                {"Dashboard Engineering", "Streamlit Web Framework", "Interactive Filters & Charts"},
                {"Business Intelligence", "Real-Time KPI Tracking"}
        });
    }

    private void seedGameDev(Topic topic) {
        module(topic, "Game Math & Architecture", new String[][]{
                {"Game Loops", "Fixed vs Variable Timestep", "Game Loop Architecture"},
                {"Game Mathematics", "2D & 3D Vectors", "Dot & Cross Products in Games"}
        });
        module(topic, "2D Game Dev with Pygame", new String[][]{
                {"Pygame Core", "Surfaces, Rects & Sprites", "Keyboard & Mouse Input"},
                {"Collisions & States", "AABB Collision Detection", "Game State Management"}
        });
        module(topic, "Unity Engine & C#", new String[][]{
                {"Unity Foundations", "GameObjects & Transforms", "MonoBehaviour Lifecycle"},
                {"Unity Scripting", "C# Scripting for Unity", "Prefabs & Instantiation"}
        });
        module(topic, "Unity 3D Design & Lighting", new String[][]{
                {"Environment Design", "Terrain Generation & Sculpting", "PBR Materials & Shaders"},
                {"Lighting & Cameras", "Real-Time & Baked Lighting", "Cinemachine Dynamic Cameras"}
        });
        module(topic, "Unreal Engine 5 & Blueprints", new String[][]{
                {"Unreal Architecture", "Actor Hierarchy & Levels", "Blueprint Visual Scripting"},
                {"Unreal Features", "Nanite & Lumen Global Illumination"}
        });
        module(topic, "Game Physics & Collisions", new String[][]{
                {"Physics Simulation", "Rigidbodies, Forces & Velocity", "Raycasting & Triggers"},
                {"Advanced Physics", "Ragdoll Physics & Constraints"}
        });
        module(topic, "Game Audio, Shaders & VFX", new String[][]{
                {"Audiovisual Polish", "Spatial 3D Audio & Music", "Particle Systems (Niagara)"},
                {"Visual Shaders", "Shader Graph & Screen Effects"}
        });
        module(topic, "Game Publishing & Optimization", new String[][]{
                {"Optimization", "Frame Rate Profiling & Draw Calls", "Memory Diagnostics & Asset Bundles"},
                {"Store Release", "Steam & Store Submission"}
        });
    }

    private void seedDsa(Topic topic) {
        module(topic, "Data Structures", new String[][]{
                {"Linear", "Array", "Linked List", "Stack", "Queue"},
                {"Non-Linear", "Graph", "Heap", "Hash Map", "DSU"},
                {"Tree-Based", "BST", "AVL", "Trie", "Segment Tree", "Fenwick Tree"}
        });
        module(topic, "Sorting Algorithms", new String[][]{
                {"Comparison-Based", "Merge", "Quick", "Heap Sort"},
                {"Non-Comparison-Based", "Counting", "Radix", "Bucket Sort"}
        });
        module(topic, "Searching", new String[][]{
                {"Linear & Binary Search", "Linear & Binary Search", "Binary Search"},
                {"Binary Search on Answer", "Binary Search on Answer"}
        });
        module(topic, "Graphs", new String[][]{
                {"Traversal", "BFS", "DFS"},
                {"Shortest Path", "Dijkstra", "Bellman-Ford", "Floyd-Warshall"},
                {"Minimum Spanning Tree", "Kruskal", "Prim"},
                {"Topological Sort", "Topological Sort"},
                {"Advanced", "SCC/Tarjan", "Bridges & Articulation Points", "Max Flow"}
        });
        module(topic, "Range Queries", new String[][]{
                {"Prefix Sum", "Prefix Sum"},
                {"Offline Range Query", "Offline Range Query"},
                {"Online Range Query", "Online Range Query"},
                {"Segment Tree (with/without Lazy Propagation)", "Segment Tree (with/without Lazy Propagation)"},
                {"Fenwick Tree (BIT)", "Fenwick Tree (BIT)"},
                {"Sparse Table", "Sparse Table"},
                {"Sqrt Decomposition", "Sqrt Decomposition"}
        });
        module(topic, "Algorithmic Paradigms", new String[][]{
                {"Divide and Conquer", "Divide and Conquer"},
                {"Greedy", "Greedy"},
                {"Dynamic Programming", "Dynamic Programming"},
                {"Recursion", "Recursion"},
                {"Backtracking", "Backtracking"}
        });
        module(topic, "String Algorithms", new String[][]{
                {"KMP & Z-function", "KMP & Z-function"},
                {"Trie-Based Matching", "Trie-Based Matching"},
                {"Suffix Array / Suffix Automaton", "Suffix Array / Suffix Automaton"},
                {"String Hashing", "String Hashing"}
        });
        module(topic, "Mathematics", new String[][]{
                {"Algebra", "Algebra"},
                {"Linear Algebra", "Linear Algebra"},
                {"Number Theory", "Number Theory"},
                {"Combinatorics", "Combinatorics"},
                {"Geometry", "Geometry"}
        });
    }

    private void module(Topic topic, String moduleTitle, String[][] groups) {
        CourseModule module = new CourseModule(topic, moduleTitle,
                moduleTitle + " lessons organized from fundamentals through applied tools.",
                topic.getModules().size() + 1);
        topic.getModules().add(module);
        for (int i = 0; i < groups.length; i++) {
            String[] group = groups[i];
            String submoduleTitle = group[0];
            Submodule submodule = new Submodule(module, submoduleTitle, i + 1);
            module.getSubmodules().add(submodule);
            for (int j = 1; j < group.length; j++) {
                addLesson(topic, module, submodule, group[j], j);
            }
        }
    }

    private void addLesson(Topic topic, CourseModule module, Submodule submodule, String title, int position) {
        String slug = slug(topic.getSlug() + "-" + module.getTitle() + "-" + submodule.getTitle() + "-" + title);
        Lesson lesson = new Lesson(submodule, slug, title,
                "Learn the definition, mechanics, and practical trade-offs of " + title + ".",
                markdown(topic.getTitle(), module.getTitle(), submodule.getTitle(), title),
                exampleCode(title), position, true);
        submodule.getLessons().add(lesson);
        SimulationSpec simulation = simulationFor(title);
        if (simulation != null) {
            lesson.setSimulation(new Simulation(lesson, simulation.type(), simulation.configJson()));
        }
        List<QuizBankSeeder.QuestionSpec> quiz = QuizBankSeeder.getQuestionsForLesson(topic.getTitle(), module.getTitle(), submodule.getTitle(), title);
        for (int i = 0; i < quiz.size(); i++) {
            QuizBankSeeder.QuestionSpec q = quiz.get(i);
            lesson.getQuizQuestions().add(new QuizQuestion(lesson, i + 1, q.prompt(), q.a(), q.b(), q.c(), q.d(), q.correct(), q.explanation()));
        }
    }

    private String markdown(String topic, String module, String submodule, String title) {
        String key = slug(title);
        if ("array".equals(key)) {
            return arrayMarkdown();
        }
        if ("linked-list".equals(key)) {
            return linkedListMarkdown();
        }
        String definition = conceptNote(title, module, submodule);
        return "# " + title + "\n\n"
                + "## Definition\n" + definition + "\n\n"
                + "## Core idea\nFocus on the state that changes during " + title + ". Trace one small input by hand, "
                + "record each decision, and connect that trace to the implementation.\n\n"
                + "## Practical use\nThis lesson belongs to " + topic + " > " + module + " > " + submodule
                + ". Use the technique when its stated guarantees match the problem, and compare its setup, runtime, "
                + "memory cost, and failure cases with a simpler alternative.\n\n"
                + "## Check your understanding\nExplain the invariant that remains true after every step, then test the "
                + "example with an empty, minimal, and typical input.\n";
    }

    private String conceptNote(String title, String module, String submodule) {
        return switch (slug(title)) {
            case "array" -> "An array stores indexed values in contiguous logical positions, giving constant-time access while insertion in the middle requires shifting later values.";
            case "linked-list" -> "A linked list stores each value in a node that points to another node, trading direct indexing for inexpensive local pointer changes.";
            case "stack" -> "A stack exposes the most recently added item first through push, peek, and pop operations.";
            case "queue" -> "A queue processes items in first-in, first-out order through enqueue and dequeue operations.";
            case "graph" -> "A graph models vertices and edges; adjacency lists favor sparse graphs while matrices give direct edge lookup.";
            case "heap" -> "A heap is a complete tree whose parent-child order keeps the minimum or maximum at the root in O(1), with updates in O(log n).";
            case "hash-map" -> "A hash map converts keys into bucket locations and resolves collisions to provide expected O(1) lookup, insertion, and deletion.";
            case "dsu" -> "Disjoint Set Union maintains changing connected components with find and union; path compression and union by rank make operations nearly constant amortized time.";
            case "bst" -> "A binary search tree keeps smaller keys on the left and larger keys on the right, enabling ordered traversal and shape-dependent search.";
            case "avl" -> "An AVL tree is a height-balanced BST that uses LL, RR, LR, and RL rotations to keep operations O(log n).";
            case "trie", "trie-based-matching" -> "A trie follows one edge per character, sharing prefixes so insertion and lookup depend on key length rather than the number of stored keys.";
            case "segment-tree", "segment-tree-with-without-lazy-propagation" -> "A segment tree stores summaries for nested ranges, answering queries in O(log n); lazy tags postpone covered range updates.";
            case "fenwick-tree", "fenwick-tree-bit" -> "A Fenwick tree stores partial sums selected by the lowest set bit, supporting prefix queries and point updates in O(log n).";
            case "merge" -> "Merge Sort recursively sorts two halves and merges them in O(n log n) time with stable ordering and O(n) auxiliary storage.";
            case "quick" -> "Quick Sort partitions values around a pivot and recursively sorts both sides; it is fast in practice but can degrade to O(n squared) with poor pivots.";
            case "heap-sort" -> "Heap Sort builds a heap and repeatedly extracts its root, guaranteeing O(n log n) time with constant auxiliary array storage.";
            case "counting" -> "Counting Sort records value frequencies and rebuilds the output in linear time when the integer key range is reasonably small.";
            case "radix" -> "Radix Sort processes keys one digit at a time with a stable bucket pass, avoiding direct comparisons between full keys.";
            case "bucket-sort" -> "Bucket Sort distributes values across ranges, sorts each bucket, and concatenates them; performance depends on an even distribution.";
            case "linear-binary-search" -> "Linear search checks candidates in order, while binary search repeatedly halves a sorted search interval.";
            case "binary-search" -> "Binary search compares the middle element of a sorted range and discards the half that cannot contain the target, taking O(log n) comparisons.";
            case "binary-search-on-answer" -> "Binary search on answer locates the first or last value where a monotonic feasibility predicate changes truth value.";
            case "bfs" -> "Breadth-first search uses a queue to expand a graph layer by layer and finds minimum edge-count paths in an unweighted graph.";
            case "dfs" -> "Depth-first search follows one path as far as possible using recursion or a stack before backtracking to unexplored neighbors.";
            case "dijkstra" -> "Dijkstra repeatedly settles the unvisited vertex with the smallest known distance and relaxes non-negative weighted edges.";
            case "bellman-ford" -> "Bellman-Ford relaxes every edge across repeated passes, supporting negative weights and detecting reachable negative cycles.";
            case "floyd-warshall" -> "Floyd-Warshall updates an all-pairs distance matrix by allowing each vertex to act as an intermediate point.";
            case "kruskal" -> "Kruskal sorts edges by weight and uses DSU to accept the lightest edge that joins two different components.";
            case "prim" -> "Prim grows one minimum spanning tree by repeatedly choosing the lightest edge that crosses from the tree to a new vertex.";
            case "topological-sort" -> "A topological order places every directed edge before its destination and exists only for directed acyclic graphs.";
            case "scc-tarjan" -> "Tarjan's SCC algorithm combines DFS discovery times, low-link values, and a stack to identify strongly connected components in linear time.";
            case "bridges-articulation-points" -> "DFS low-link values reveal edges or vertices whose removal increases the number of connected components.";
            case "max-flow" -> "Maximum-flow algorithms repeatedly augment source-to-sink paths in a residual graph until no more capacity can be sent.";
            case "prefix-sum" -> "A prefix-sum array stores cumulative totals so any static range sum is the difference of two prefix values.";
            case "offline-range-query" -> "Offline range-query methods reorder known queries to reuse work, such as moving Mo's Algorithm window only a short distance.";
            case "online-range-query" -> "Online range queries must answer in arrival order, so a maintained tree or decomposition replaces global query reordering.";
            case "sparse-table" -> "A sparse table precomputes power-of-two ranges, giving O(1) idempotent queries after O(n log n) construction.";
            case "sqrt-decomposition" -> "Square-root decomposition groups values into blocks and combines whole-block summaries with a small boundary scan.";
            case "divide-and-conquer" -> "Divide and conquer splits a problem into independent smaller instances, solves them recursively, and combines their results.";
            case "greedy" -> "A greedy algorithm commits to the best local valid choice and requires a proof that this choice preserves a global optimum.";
            case "dynamic-programming" -> "Dynamic programming stores solutions to overlapping subproblems and evaluates them in an order that satisfies their dependencies.";
            case "recursion" -> "Recursion solves a problem through smaller calls of the same function; a base case stops descent and returning calls unwind the stack.";
            case "backtracking" -> "Backtracking builds a candidate, abandons it when constraints fail, undoes the last choice, and tries the next alternative.";
            case "kmp-z-function" -> "KMP and the Z-function reuse prefix-match information so string matching avoids rechecking characters and runs in linear time.";
            case "suffix-array-suffix-automaton" -> "Suffix structures index all suffixes or substrings so repeated pattern and lexicographic queries can be answered efficiently.";
            case "string-hashing" -> "String hashing maps substrings to numeric fingerprints, enabling fast comparisons with a small, manageable collision risk.";
            case "algebra" -> "Algebra expresses constraints with symbols and transformations, providing the equations used to reason about algorithm inputs and bounds.";
            case "linear-algebra" -> "Linear algebra studies vectors, matrices, and linear transformations used in geometry, graphics, optimization, and machine learning.";
            case "number-theory" -> "Number theory covers divisibility, primes, modular arithmetic, and gcd-based tools used throughout competitive programming.";
            case "combinatorics" -> "Combinatorics counts structured choices through permutations, combinations, recurrence relations, and inclusion-exclusion.";
            case "geometry" -> "Computational geometry represents points and shapes numerically and uses orientation, distance, and intersection tests robustly.";
            case "syntax-semantics", "syntax" -> "Syntax defines which programs are grammatically valid, while semantics explains what those valid statements do when executed.";
            case "variables-data-types" -> "Variables bind names to values, and data types determine the operations, representation, and constraints of those values.";
            case "operators" -> "Operators combine or compare values; precedence and associativity determine how a compound expression is grouped.";
            case "control-flow-if-else-loops", "control-flow" -> "Conditionals choose a path and loops repeat a block while preserving a clear termination condition and loop invariant.";
            case "functions-scope", "functions" -> "Functions package reusable behavior behind parameters and returns, while scope controls where each name can be accessed.";
            case "lists-tuples", "collections", "containers" -> "Collection types organize multiple values; choose among mutable sequences, fixed records, sets, and key-value mappings by required operations.";
            case "sets" -> "A set stores unique values and supports membership and mathematical set operations without positional indexing.";
            case "dictionaries" -> "A dictionary maps unique keys to values, supporting direct lookup and update by meaningful identifiers.";
            case "string-manipulation" -> "String manipulation combines indexing, slicing, searching, formatting, and immutable transformations to process text safely.";
            case "file-handling" -> "File handling opens a resource with an explicit mode, processes data, handles encoding and errors, and closes it deterministically.";
            case "exception-handling", "exceptions" -> "Exceptions separate normal control flow from recoverable failures and should be caught only where useful recovery or context is available.";
            case "modules-packages" -> "Modules and packages divide code into importable namespaces, making dependencies and public interfaces easier to maintain.";
            case "oop-classes-inheritance-polymorphism", "oop" -> "Object-oriented design groups state with behavior; composition, inheritance, and polymorphism model variation with different coupling trade-offs.";
            case "iterators-generators", "iterators" -> "Iterators produce one value at a time, and generators suspend execution between yields to represent lazy sequences compactly.";
            case "decorators" -> "A decorator wraps a callable or class to add behavior without changing its original body.";
            case "lambda-map-filter-reduce", "linq", "streams" -> "Functional collection pipelines transform, filter, and aggregate values while keeping the data flow explicit.";
            case "virtual-environments-pip" -> "A virtual environment isolates Python packages for one project, while pip installs versions declared by that project's dependency policy.";
            case "pointers", "pointers-memory-management" -> "Pointers store memory addresses; correct ownership, lifetime, bounds, and null handling are essential for safe low-level code.";
            case "raii", "smart-pointers" -> "RAII ties resource lifetime to object lifetime, and smart pointers encode ownership so cleanup occurs automatically.";
            case "maven", "gradle" -> title + " describes dependencies and repeatable build tasks so a Java project compiles and tests consistently across machines.";
            case "junit", "pytest", "jest", "xunit", "mockito" -> title + " supports automated tests that arrange state, exercise one behavior, and assert an observable result.";
            case "html5-document-anatomy" -> "HTML5 defines the standard document anatomy consisting of <!DOCTYPE html>, <html>, <head> for metadata/links, and <body> for rendered page elements.";
            case "semantic-elements-layout" -> "Semantic HTML elements (<header>, <nav>, <main>, <article>, <section>, <footer>) communicate document meaning directly to browsers, search crawlers, and assistive readers.";
            case "forms-inputs-validation" -> "HTML5 forms capture user input with typed inputs (text, email, password, radio, checkbox, select) and native browser constraints (required, pattern, min/max).";
            case "media-tables-accessibility" -> "Accessible web media uses <img> with alt attributes, <video>/<audio> controls, and structured <table> markup with <thead>, <tbody>, and scope attributes.";
            case "the-css-box-model" -> "The CSS Box Model calculates space through content, padding, border, and margin layers; box-sizing: border-box includes padding and border inside the declared width.";
            case "positioning-specificity" -> "CSS positioning controls flow (static, relative, absolute, fixed, sticky), while selector specificity calculates rule priority hierarchically.";
            case "flexbox-layout-system" -> "Flexbox is a 1D layout model aligning items along a main axis (justify-content) and cross axis (align-items) with flexible sizing (flex-grow, flex-shrink).";
            case "css-grid-architecture" -> "CSS Grid is a 2D layout system defining explicit columns and rows simultaneously using fractional (fr) units, grid areas, and gap properties.";
            case "responsive-design-media-queries" -> "Responsive design combines fluid units (%, rem, vw/vh), viewport meta configuration, and @media queries to adapt interfaces gracefully across screen viewports.";
            case "bootstrap-5-tailwind-css" -> "Bootstrap accelerates design with pre-built components and a 12-column grid, while Tailwind CSS uses utility classes directly in markup for granular design control.";
            case "variables-data-types-control-flow" -> "Modern JavaScript manages values with block-scoped let and const, handles primitive/reference types, and directs execution with branching and loops.";
            case "functions-scope-closures" -> "JavaScript functions package reusable logic; arrow functions preserve lexical this, and closures retain access to an outer function's scope across execution.";
            case "arrays-objects-destructuring" -> "JavaScript collections store structured data; ES6 destructuring, rest/spread operators, and functional methods (map, filter, reduce) enable clean immutable transformations.";
            case "dom-selection-manipulation" -> "The Document Object Model (DOM) represents HTML as a tree of objects accessible via document.querySelector, classList methods, and attribute mutation.";
            case "event-handling-bubbling" -> "Browser events listen for user interaction through addEventListener, capturing, targeting, and bubbling events up through parent DOM nodes.";
            case "promises-async-await-fetch-api" -> "Asynchronous JavaScript manages non-blocking network requests using Promises, async/await syntax, and the browser's native Fetch API.";
            case "jsx-react-components" -> "React models user interfaces through declarative functional components and JSX syntax, rendering efficiently via a Virtual DOM diffing engine.";
            case "props-component-composition" -> "Props pass read-only configuration and event callbacks downward through the component tree, enabling reusable and composable UI hierarchies.";
            case "state-management-with-usestate" -> "useState gives functional React components reactive local state, triggering automatic re-renders whenever state changes.";
            case "side-effects-with-useeffect" -> "useEffect synchronizes React components with external systems, handling data fetching, subscriptions, timers, and cleanup routines.";
            case "react-router-single-page-apps" -> "React Router manages client-side navigation in Single Page Applications (SPAs), updating views without triggering browser page reloads.";
            case "node-js-architecture-event-loop" -> "Node.js executes server-side JavaScript on Google's V8 engine, using an event-driven, single-threaded event loop and libuv for high-throughput non-blocking I/O.";
            case "built-in-node-modules-npm" -> "Node provides essential core modules (fs, path, http) and the NPM ecosystem for managing dependencies declared in package.json.";
            case "express-server-routing" -> "Express simplifies server development with expressive routing methods (app.get, app.post) mapping URL paths and HTTP verbs to handler callbacks.";
            case "middleware-restful-api-design" -> "Express middleware intercepts requests and responses, enabling request body parsing, CORS headers, logging, and standard REST endpoints.";
            case "postgresql-foundations-schemas" -> "PostgreSQL is an ACID-compliant relational database organizing structured records into typed tables with primary and foreign key constraints.";
            case "crud-queries-table-joins" -> "SQL queries query and modify relational data with SELECT, INSERT, UPDATE, DELETE, and join related entities using INNER and LEFT JOIN.";
            case "mongodb-document-storage" -> "MongoDB stores flexible, schema-free BSON documents in collections, scaling horizontally for high-throughput unstructured data.";
            case "mongoose-orm-data-validation" -> "Mongoose provides elegant schema modeling, validation, middleware hooks, and query builders on top of MongoDB.";
            case "password-hashing-with-bcrypt" -> "Bcrypt secures stored passwords by applying cryptographically random salts and slow adaptive hashing, mitigating rainbow-table and brute-force attacks.";
            case "sessions-cookies-jwt" -> "Stateful auth relies on encrypted HTTP-only session cookies, while stateless auth encodes user identity into digitally signed JSON Web Tokens (JWT).";
            case "oauth-2-0-google-sign-in" -> "OAuth 2.0 delegates authentication to identity providers like Google and GitHub, granting secure access tokens without exposing user credentials.";
            case "web-security-owasp-top-10" -> "Web security defends systems against top vulnerabilities including SQL injection, Cross-Site Scripting (XSS), CSRF, and broken access controls.";
            case "git-github-version-control" -> "Git tracks distributed code history across commits and branches, while GitHub facilitates pull requests, team code reviews, and automated CI/CD pipelines.";
            case "environment-variables-secrets" -> "Environment variables keep sensitive database credentials, API keys, and deployment configurations out of version-controlled source repositories.";
            case "cloud-deployment-with-vercel-render" -> "Modern cloud platforms (Vercel, Render) automate production builds, edge routing, SSL certification, and deployment directly from Git pushes.";
            case "dart-syntax-types" -> "Dart is a strongly typed, client-optimized language powering Flutter, featuring sound null safety and JIT/AOT compilation.";
            case "control-flow-functions" -> "Dart controls application logic with branching, loops, first-class functions, and fat-arrow syntax.";
            case "stateless-vs-stateful-widgets" -> "StatelessWidget represents immutable UI, while StatefulWidget preserves mutable state across user interactions using State.setState.";
            case "layouts-ui-trees" -> "Flutter builds UIs by composing widget trees with Column, Row, Stack, Container, and Expanded layout widgets.";
            case "expo-tooling-project-setup" -> "Expo simplifies React Native development with managed project templates, Expo Go mobile testing, and pre-configured native tooling.";
            case "core-components-flexbox" -> "React Native compiles declarative components (View, Text, Image, ScrollView) to native mobile primitives using Flexbox styling.";
            case "stack-tab-navigation" -> "React Navigation orchestrates screen transitions and user routing using native stack headers and bottom tab bars.";
            case "handling-touch-gestures" -> "React Native captures tactile user input through Pressable, TouchableOpacity, and the Gesture Handler system.";
            case "kotlin-syntax-coroutines" -> "Kotlin powers native Android with expressive syntax, null safety, data classes, and lightweight Coroutines for asynchronous work.";
            case "compose-declarative-ui" -> "Jetpack Compose transforms Android UI development with declarative Kotlin @Composable functions and reactive state observing.";
            case "viewmodels-stateflow" -> "Android ViewModels survive screen configuration changes and emit UI state reactively using StateFlow and SharedFlow.";
            case "swift-syntax-optionals" -> "Swift delivers high performance on Apple platforms with strong type inference, Optionals, structs, and protocol-oriented architecture.";
            case "views-modifiers-layouts" -> "SwiftUI declares user interfaces through composable View structs and chainable view modifiers.";
            case "state-binding-environment" -> "SwiftUI synchronizes UI reactivity through @State, @Binding, @ObservedObject, and @Environment property wrappers.";
            case "state-architecture-mvvm" -> "The MVVM (Model-View-ViewModel) pattern decouples presentation from business logic for maintainable, testable mobile apps.";
            case "flutter-bloc-riverpod" -> "BLoC separates presentation from events using reactive streams, while Riverpod provides compile-safe dependency injection.";
            case "rest-apis-with-retrofit-dio" -> "HTTP clients (Dio for Flutter, Retrofit for Android) simplify making asynchronous API requests, intercepting headers, and handling JSON.";
            case "firebase-authentication-firestore" -> "Firebase powers serverless mobile backends with turnkey social auth, email sign-in, and real-time NoSQL Firestore syncing.";
            case "sqlite-room-database-setup" -> "Room provides an abstraction layer over SQLite on Android, ensuring compile-time SQL verification and reactive queries.";
            case "encrypted-sharedpreferences-realm" -> "Secure local mobile storage protects user tokens and offline datasets using encrypted key-value stores and Realm databases.";
            case "android-keystores-play-console" -> "Android applications are cryptographically signed with private keystores and uploaded as Android App Bundles (.aab) to Google Play.";
            case "testflight-fastlane-pipelines" -> "Fastlane automates mobile screenshot generation, certificate provisioning, beta deployment to TestFlight, and store submission.";
            default -> title + " is a " + module + " topic used in " + submodule
                    + ". The lesson identifies its purpose, the state it manages, and the smallest working workflow before comparing practical trade-offs.";
        };
    }

    private String exampleCode(String title) {
        String key = slug(title);
        return switch (key) {
            case "array" -> arrayExampleCode();
            case "linked-list" -> linkedListExampleCode();
            case "html5-document-anatomy" -> "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <title>Document Anatomy</title>\n</head>\n<body>\n  <h1>Welcome to CodeTrail</h1>\n</body>\n</html>";
            case "semantic-elements-layout" -> "<header>\n  <nav><a href=\"#home\">Home</a></nav>\n</header>\n<main>\n  <article>\n    <h2>Semantic Article</h2>\n    <p>Semantic tags improve accessibility and SEO.</p>\n  </article>\n</main>\n<footer>&copy; 2026 CodeTrail</footer>";
            case "forms-inputs-validation" -> "<form action=\"/api/submit\" method=\"POST\">\n  <label for=\"email\">Email:</label>\n  <input type=\"email\" id=\"email\" name=\"email\" required>\n  <button type=\"submit\">Submit</button>\n</form>";
            case "the-css-box-model" -> "* {\n  box-sizing: border-box;\n}\n.box {\n  width: 300px;\n  padding: 20px;\n  border: 2px solid #0089fc;\n  margin: 15px auto;\n}";
            case "flexbox-layout-system" -> ".container {\n  display: flex;\n  justify-content: space-between;\n  align-items: center;\n  flex-wrap: wrap;\n  gap: 16px;\n}";
            case "css-grid-architecture" -> ".dashboard-grid {\n  display: grid;\n  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));\n  gap: 20px;\n}";
            case "variables-data-types-control-flow" -> "const user = 'Student';\nlet progress = 85;\nif (progress >= 80) {\n  console.log(`${user} has almost completed the course!`);\n}";
            case "functions-scope-closures" -> "function createCounter() {\n  let count = 0;\n  return () => ++count;\n}\nconst counter = createCounter();\nconsole.log(counter()); // 1";
            case "promises-async-await-fetch-api" -> "async function fetchUser(id) {\n  const response = await fetch(`/api/users/${id}`);\n  const user = await response.json();\n  return user;\n}";
            case "jsx-react-components" -> "function WelcomeBanner({ username }) {\n  return (\n    <div className=\"banner\">\n      <h1>Hello, {username}!</h1>\n    </div>\n  );\n}";
            case "state-management-with-usestate" -> "import { useState } from 'react';\n\nfunction Counter() {\n  const [count, setCount] = useState(0);\n  return <button onClick={() => setCount(c => c + 1)}>Clicks: {count}</button>;\n}";
            case "express-server-routing" -> "const express = require('express');\nconst app = express();\n\napp.use(express.json());\napp.get('/api/lessons', (req, res) => {\n  res.json([{ id: 1, title: 'HTML5 Basics' }]);\n});\napp.listen(3000);";
            case "postgresql-foundations-schemas" -> "CREATE TABLE users (\n  id SERIAL PRIMARY KEY,\n  username VARCHAR(80) UNIQUE NOT NULL,\n  email VARCHAR(120) UNIQUE NOT NULL,\n  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n);";
            case "crud-queries-table-joins" -> "SELECT users.username, enrollments.enrolled_at\nFROM users\nINNER JOIN enrollments ON users.id = enrollments.user_id\nWHERE enrollments.completed = true;";
            case "password-hashing-with-bcrypt" -> "const bcrypt = require('bcrypt');\nconst saltRounds = 12;\nconst hash = await bcrypt.hash('secretPassword', saltRounds);\nconst isMatch = await bcrypt.compare('secretPassword', hash);";
            case "dart-syntax-types" -> "void main() {\n  String framework = 'Flutter';\n  int version = 3;\n  print('Building with $framework $version');\n}";
            case "stateless-vs-stateful-widgets" -> "import 'package:flutter/material.dart';\n\nclass HelloWidget extends StatelessWidget {\n  @override\n  Widget build(BuildContext context) {\n    return Center(child: Text('Hello CodeTrail!'));\n  }\n}";
            case "expo-tooling-project-setup" -> "import { View, Text, StyleSheet } from 'react-native';\n\nexport default function App() {\n  return (\n    <View style={styles.container}>\n      <Text>Welcome to React Native & Expo!</Text>\n    </View>\n  );\n}\nconst styles = StyleSheet.create({ container: { flex: 1, justifyContent: 'center', alignItems: 'center' } });";
            case "kotlin-syntax-coroutines" -> "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\n    launch {\n        delay(1000L)\n        println(\"World!\")\n    }\n    println(\"Hello\")\n}";
            case "compose-declarative-ui" -> "@Composable\nfun Greeting(name: String) {\n    Surface(color = MaterialTheme.colorScheme.primary) {\n        Text(text = \"Hello $name!\", modifier = Modifier.padding(16.dp))\n    }\n}";
            case "swift-syntax-optionals" -> "var username: String? = \"AppDevStudent\"\nif let name = username {\n    print(\"Welcome, \\(name)!\")\n}";
            case "views-modifiers-layouts" -> "import SwiftUI\n\nstruct ContentView: View {\n    var body: some View {\n        Text(\"Welcome to SwiftUI\")\n            .font(.headline)\n            .foregroundColor(.blue)\n            .padding()\n    }\n}";
            case "sqlite-room-database-setup" -> "@Dao\ninterface UserDao {\n    @Query(\"SELECT * FROM user\")\n    fun getAll(): List<User>\n    @Insert\n    fun insertAll(vararg users: User)\n}";

            case "recursion" -> "int factorial(int n) { return n <= 1 ? 1 : n * factorial(n - 1); }";
            case "binary-search" -> "int lo = 0, hi = values.length - 1; while (lo <= hi) { int mid = (lo + hi) / 2; }";
            case "binary-search-on-answer" -> "long lo = minAnswer, hi = maxAnswer; while (lo < hi) { long mid = (lo + hi) / 2; }";
            case "bfs" -> "queue.add(start); while (!queue.isEmpty()) { int node = queue.remove(); visit(node); }";
            case "dfs" -> "void dfs(int node) { seen[node] = true; for (int next : graph[node]) if (!seen[next]) dfs(next); }";
            case "merge" -> "merge(leftHalf, rightHalf); // each half is already sorted";
            case "quick" -> "int pivot = partition(values, left, right); quicksort(values, left, pivot - 1);";
            case "dsu" -> "parent[find(a)] = find(b); // union two components";
            case "heap" -> "PriorityQueue<Integer> heap = new PriorityQueue<>(); heap.add(value);";
            default -> "// Exercise: build a minimal " + title + " example.\n"
                    + "// Print each important state, then test one edge case.";
        };
    }

    private String arrayMarkdown() {
        return """
                # Array: Definition, Indexing, Loops & Summation

                ## 1. What is an Array?
                An array is a fundamental linear data structure that stores a collection of elements of the same type in contiguous (adjacent) memory locations.

                - Contiguous Memory: All elements sit side-by-side in physical memory with zero gaps.
                - Direct Address Calculation: Accessing any element is O(1) constant time because the computer calculates the exact address immediately using:
                  Address = BaseAddress + (Index * ElementSize)
                - Fixed Capacity: Once created, an array's size cannot change. Inserting beyond capacity requires creating a new, larger array and copying all elements.

                ## 2. The Concept of Indexing
                An index is an integer offset that indicates an element's position relative to the start (base address) of the array.

                - Why 0-Indexed? The index represents how many elements away a value is from the start. The very first element is at offset 0 (0 elements away from BaseAddress).
                - Index Range: For an array of size N, valid indices are numbered from 0 to N - 1.
                - Bounds Safety: Accessing an index less than 0 or greater than or equal to N causes an ArrayIndexOutOfBoundsException in Java.

                ## 3. Accessing Elements with a For Loop
                Because array indices are sequential integers (0, 1, 2, ..., N - 1), a standard for loop is the ideal mechanism to traverse, read, and update array elements.

                ```java
                int[] numbers = {12, 45, 7, 89, 23};

                // Standard indexed for loop
                for (int i = 0; i < numbers.length; i++) {
                    System.out.println("Element at index " + i + " is " + numbers[i]);
                }

                // Enhanced for-each loop (read-only traversal)
                for (int num : numbers) {
                    System.out.println("Value: " + num);
                }
                ```

                ## 4. Calculating the Sum of Values in an Array
                To calculate the sum of all elements in an array:
                - Step 1: Initialize an accumulator variable `sum = 0`.
                - Step 2: Loop from index 0 through `numbers.length - 1`.
                - Step 3: Add `numbers[i]` to `sum` in each iteration.
                - Step 4: After the loop finishes, `sum` holds the total. You can also calculate the average by dividing sum by the array length.

                ```java
                int[] scores = {12, 45, 7, 89, 23};
                int sum = 0;

                for (int i = 0; i < scores.length; i++) {
                    sum += scores[i];
                }

                double average = (double) sum / scores.length;
                System.out.println("Total Sum: " + sum);
                System.out.println("Average: " + average);
                ```

                ## 5. Algorithmic Complexity
                - Index Access (A[i]): O(1) Constant Time
                - Updating Value (A[i] = x): O(1) Constant Time
                - Linear Search / Sum / Traversal: O(N) Linear Time
                - Insert/Delete at Middle/Beginning: O(N) Linear Time (due to shifting elements)
                """;
    }

    private String arrayExampleCode() {
        return """
                // Complete Java program demonstrating Array indexing, loops, and sum
                public class ArrayDemo {
                    public static void main(String[] args) {
                        // 1. Declare and initialize an integer array
                        int[] numbers = {12, 45, 7, 89, 23};
                        
                        System.out.println("Array length: " + numbers.length);
                        System.out.println("First element (index 0): " + numbers[0]);
                        System.out.println("Last element (index " + (numbers.length - 1) + "): " + numbers[numbers.length - 1]);
                        
                        // 2. Access elements using a standard for loop
                        System.out.println("\\n--- Traversing Array with For Loop ---");
                        for (int i = 0; i < numbers.length; i++) {
                            System.out.println("Index " + i + " -> " + numbers[i]);
                        }
                        
                        // 3. Calculating the sum of all values
                        int sum = 0;
                        for (int i = 0; i < numbers.length; i++) {
                            sum += numbers[i];
                        }
                        
                        double average = (double) sum / numbers.length;
                        
                        System.out.println("\\n--- Calculation Results ---");
                        System.out.println("Total Sum = " + sum);
                        System.out.println("Average   = " + average);
                    }
                }
                """;
    }

    private String linkedListMarkdown() {
        return """
                # Linked List: Node Architecture, Head/Tail & Operations

                ## 1. What is a Linked List?
                A Linked List is a linear data structure where elements are not stored in contiguous memory locations. Instead, each element is housed inside an independent object called a Node.

                - Two-Division Node Design:
                  1. Data Division: Contains the stored value (e.g. integer, string, object).
                  2. Next Pointer Division: Contains the memory address reference pointing to the next Node.
                - Head Pointer: A special pointer variable that tracks the very first node of the list.
                - Tail Pointer: A pointer tracking the last node in the list.
                - Terminal NULL: The last node's next pointer points to NULL, marking the end of the chain.

                ## 2. Why Use a Linked List Instead of an Array?
                - Dynamic Sizing: Arrays have a fixed capacity allocated upfront. Linked lists allocate each node dynamically on demand in available heap memory, never requiring capacity doubling or mass memory reallocations.
                - Fast O(1) Insertions/Deletions at Head: Inserting or removing at the head of a linked list only requires updating pointer references (O(1)). In an array, inserting at the front requires shifting every element to the right (O(N)).
                - Fast O(1) Appends at Tail: When maintaining a `tail` reference, appending a new node takes O(1) without shifting.
                - Trade-off (Sequential Access): Unlike arrays which support O(1) random index access (A[i]), linked lists require sequential traversal from the head (O(N)) to reach an arbitrary index.

                ## 3. Initializing a Linked List
                To initialize a linked list in Java, we define:
                1. A Node class representing the two compartments: `int data` and `Node next`.
                2. A LinkedList class maintaining `head`, `tail`, and `size`.

                ```java
                class Node {
                    int data;   // Division 1: Data value
                    Node next;  // Division 2: Pointer to next node

                    Node(int data) {
                        this.data = data;
                        this.next = null;
                    }
                }

                class LinkedList {
                    Node head = null;
                    Node tail = null;
                    int size = 0;
                }
                ```

                ## 4. Types of Linked Lists
                - Singly Linked List (SLL): Each node contains one pointer pointing forward to the next node. Traversal is strictly forward.
                - Doubly Linked List (DLL): Each node has two pointers: `prev` (pointing to previous node) and `next` (pointing to next node). Enables bidirectional traversal and O(1) node deletion when a node reference is given.
                - Circular Linked List (CLL): The tail node's next pointer links back to the head node rather than NULL, forming a continuous ring. Ideal for round-robin CPU scheduling and playlist loops.

                ## 5. Adding Elements (Insertion Operations)
                - Insert at Head (O(1)):
                  1. Allocate newNode.
                  2. Set newNode.next = head.
                  3. Update head = newNode (and tail = newNode if previously empty).
                - Insert at Tail / Append (O(1) with Tail pointer):
                  1. Allocate newNode.
                  2. Set tail.next = newNode.
                  3. Update tail = newNode.
                - Insert After a Node (O(1) once node is located):
                  1. Set newNode.next = curr.next.
                  2. Set curr.next = newNode.

                ## 6. Removing Elements (Deletion Operations)
                - Remove from Head (O(1)):
                  1. Check if head is null (empty check).
                  2. Update head = head.next. If list becomes empty, set tail = null.
                - Remove by Value (O(N) search + O(1) bypass):
                  1. Traverse until curr.next holds the target value.
                  2. Bypass target node: `curr.next = curr.next.next`.
                  3. The disconnected node is automatically reclaimed by garbage collection.

                ## 7. Operation Complexity Summary
                - Insert at Head: O(1)
                - Insert at Tail (with tail pointer): O(1)
                - Remove from Head: O(1)
                - Search / Traverse / Access by index: O(N)
                - Delete by Value: O(N) to locate + O(1) to unlink
                """;
    }

    private String linkedListExampleCode() {
        return """
                // Complete runnable Singly Linked List with Head, Tail, Insertions & Deletions
                public class LinkedListDemo {

                    // Node: [ Data Division | Next Pointer Division ]
                    static class Node {
                        int data;
                        Node next;

                        Node(int data) {
                            this.data = data;
                            this.next = null;
                        }
                    }

                    static class SinglyLinkedList {
                        Node head = null;
                        Node tail = null;
                        int size = 0;

                        // 1. Insert at Head - O(1)
                        public void insertHead(int value) {
                            Node newNode = new Node(value);
                            if (head == null) {
                                head = tail = newNode;
                            } else {
                                newNode.next = head;
                                head = newNode;
                            }
                            size++;
                            System.out.println("Inserted " + value + " at HEAD");
                        }

                        // 2. Insert at Tail (Append) - O(1) using tail reference
                        public void insertTail(int value) {
                            Node newNode = new Node(value);
                            if (tail == null) {
                                head = tail = newNode;
                            } else {
                                tail.next = newNode;
                                tail = newNode;
                            }
                            size++;
                            System.out.println("Inserted " + value + " at TAIL");
                        }

                        // 3. Remove Head - O(1)
                        public int removeHead() {
                            if (head == null) throw new IllegalStateException("List is empty");
                            int val = head.data;
                            head = head.next;
                            if (head == null) tail = null;
                            size--;
                            System.out.println("Removed " + val + " from HEAD");
                            return val;
                        }

                        // 4. Remove by Value - O(N) search + O(1) pointer bypass
                        public boolean removeValue(int target) {
                            if (head == null) return false;
                            if (head.data == target) {
                                removeHead();
                                return true;
                            }
                            Node curr = head;
                            while (curr.next != null && curr.next.data != target) {
                                curr = curr.next;
                            }
                            if (curr.next != null) {
                                System.out.println("Bypassing and removing node " + target);
                                curr.next = curr.next.next;
                                if (curr.next == null) tail = curr;
                                size--;
                                return true;
                            }
                            return false;
                        }

                        // 5. Display: [data | ●] --> ... --> NULL
                        public void printList() {
                            Node curr = head;
                            System.out.print("List (size " + size + "): ");
                            while (curr != null) {
                                System.out.print("[" + curr.data + " | ●] --> ");
                                curr = curr.next;
                            }
                            System.out.println("NULL");
                        }
                    }

                    public static void main(String[] args) {
                        SinglyLinkedList list = new SinglyLinkedList();
                        list.insertTail(10);
                        list.insertTail(20);
                        list.insertTail(30);
                        list.printList();

                        list.insertHead(5);
                        list.printList();

                        list.removeHead();
                        list.printList();

                        list.removeValue(20);
                        list.printList();
                    }
                }
                """;
    }

    private SimulationSpec simulationFor(String title) {
        return switch (slug(title)) {
            case "array" -> new SimulationSpec("ARRAY", "{\"values\":[4,1,7,2,6]}");
            case "linked-list" -> new SimulationSpec("LINKED_LIST", "{\"values\":[4,1,7,2]}");
            case "stack" -> new SimulationSpec("STACK", "{\"values\":[3,1,4,2]}");
            case "queue" -> new SimulationSpec("QUEUE", "{\"values\":[3,1,4,2]}");
            case "graph" -> new SimulationSpec("GRAPH_REPRESENTATION", "{\"vertices\":[\"A\",\"B\",\"C\",\"D\"],\"edges\":[[\"A\",\"B\"],[\"A\",\"C\"],[\"B\",\"D\"],[\"C\",\"D\"]],\"directed\":false}");
            case "hash-map" -> new SimulationSpec("HASH_MAP", "{\"keys\":[\"cat\",\"dog\",\"ant\"]}");
            case "bst" -> new SimulationSpec("BST", "{\"values\":[7,3,9,1,5]}");
            case "avl" -> new SimulationSpec("AVL", "{\"values\":[3,2,1,4,5]}");
            case "trie" -> new SimulationSpec("TRIE", "{\"words\":[\"cat\",\"car\",\"dog\"],\"prefix\":\"ca\"}");
            case "merge", "quick", "heap-sort", "counting", "radix", "bucket-sort" ->
                    new SimulationSpec("SORTING", "{\"values\":[8,3,5,1,9,2],\"algorithm\":\""
                            + title.toUpperCase(Locale.ROOT) + "\"}");
            case "linear-binary-search" -> new SimulationSpec("LINEAR_SEARCH", "{\"values\":[4,8,1,9,2],\"target\":9}");
            case "recursion" -> new SimulationSpec("RECURSION", "{\"n\":5}");
            case "binary-search" -> new SimulationSpec("BINARY_SEARCH", "{\"values\":[1,3,5,7,9,12],\"target\":7}");
            case "binary-search-on-answer" -> new SimulationSpec("BINARY_SEARCH_ANSWER", "{\"limit\":12,\"firstTrue\":7}");
            case "bfs" -> new SimulationSpec("BFS", "{\"edges\":[[\"A\",\"B\"],[\"A\",\"C\"],[\"B\",\"D\"],[\"B\",\"E\"],[\"C\",\"F\"],[\"E\",\"F\"]],\"directed\":false,\"start\":\"A\"}");
            case "dfs" -> new SimulationSpec("DFS", "{\"edges\":[[\"A\",\"B\"],[\"A\",\"C\"],[\"B\",\"D\"],[\"B\",\"E\"],[\"C\",\"F\"],[\"E\",\"F\"]],\"directed\":false,\"start\":\"A\"}");
            case "bellman-ford" -> new SimulationSpec("BELLMAN_FORD", "{\"weightedEdges\":[[\"A\",\"B\",4],[\"A\",\"C\",2],[\"B\",\"C\",-1],[\"B\",\"D\",2],[\"C\",\"D\",3],[\"D\",\"E\",1]],\"start\":\"A\"}");
            case "floyd-warshall" -> new SimulationSpec("FLOYD_WARSHALL", "{\"vertices\":[\"A\",\"B\",\"C\",\"D\"],\"matrix\":[[0,4,11,999],[999,0,2,7],[999,999,0,3],[999,999,999,0]]}");
            case "segment-tree" -> new SimulationSpec("SEGMENT_TREE", "{\"values\":[2,1,5,3,4],\"operation\":\"sum\"}");
            case "fenwick-tree" -> new SimulationSpec("FENWICK_TREE", "{\"values\":[2,1,5,3,4]}");
            case "dsu" -> new SimulationSpec("DSU", "{\"size\":6}");
            case "heap" -> new SimulationSpec("HEAP", "{\"values\":[7,2,9,1,5],\"kind\":\"min\"}");
            case "topological-sort" -> new SimulationSpec("TOPOLOGICAL_SORT", "{\"vertices\":[\"A\",\"B\",\"C\",\"D\"],\"edges\":[[\"A\",\"B\"],[\"A\",\"C\"],[\"B\",\"D\"],[\"C\",\"D\"]]}");
            case "scc-tarjan" -> new SimulationSpec("SCC", "{\"vertices\":[\"A\",\"B\",\"C\",\"D\",\"E\"],\"edges\":[[\"A\",\"B\"],[\"B\",\"C\"],[\"B\",\"D\"],[\"C\",\"A\"],[\"D\",\"E\"],[\"E\",\"D\"]]}");
            case "bridges-articulation-points" -> new SimulationSpec("BRIDGES", "{\"vertices\":[\"A\",\"B\",\"C\",\"D\",\"E\"],\"edges\":[[\"A\",\"B\"],[\"B\",\"C\"],[\"B\",\"D\"],[\"C\",\"D\"],[\"D\",\"E\"]]}");
            case "max-flow" -> new SimulationSpec("MAX_FLOW", "{\"capacityEdges\":[[\"S\",\"A\",3],[\"S\",\"B\",2],[\"A\",\"B\",1],[\"A\",\"T\",2],[\"B\",\"T\",3]],\"source\":\"S\",\"sink\":\"T\"}");
            case "dijkstra" -> new SimulationSpec("DIJKSTRA", "{\"start\":\"A\"}");
            case "kruskal" -> new SimulationSpec("KRUSKAL", "{}");
            case "prim" -> new SimulationSpec("PRIM", "{\"start\":\"A\"}");
            case "prefix-sum" -> new SimulationSpec("PREFIX_SUM", "{\"values\":[2,1,5,3,4]}");
            case "offline-range-query" -> new SimulationSpec("RANGE_QUERY", "{\"mode\":\"offline\",\"values\":[2,1,5,3,4]}");
            case "online-range-query" -> new SimulationSpec("RANGE_QUERY", "{\"mode\":\"online\",\"values\":[2,1,5,3,4]}");
            case "segment-tree-with-without-lazy-propagation" -> new SimulationSpec("SEGMENT_TREE", "{\"operation\":\"lazy-range-update\",\"values\":[2,1,5,3,4]}");
            case "fenwick-tree-bit" -> new SimulationSpec("FENWICK_TREE", "{\"values\":[2,1,5,3,4]}");
            case "sparse-table" -> new SimulationSpec("SPARSE_TABLE", "{\"values\":[7,2,5,1,6,3]}");
            case "sqrt-decomposition" -> new SimulationSpec("SQRT_DECOMPOSITION", "{\"values\":[7,2,5,1,6,3]}");
            case "divide-and-conquer" -> new SimulationSpec("DIVIDE_CONQUER", "{\"values\":[8,3,5,1,9,2]}");
            case "greedy" -> new SimulationSpec("GREEDY", "{\"intervals\":[[1,3],[2,4],[3,5],[5,7]]}");
            case "dynamic-programming" -> new SimulationSpec("DYNAMIC_PROGRAMMING", "{\"n\":7}");
            case "backtracking" -> new SimulationSpec("BACKTRACKING", "{\"size\":4}");
            case "kmp-z-function" -> new SimulationSpec("STRING_MATCHING", "{\"text\":\"ABABDABACDABABCABAB\",\"pattern\":\"ABABCABAB\"}");
            case "trie-based-matching" -> new SimulationSpec("TRIE", "{\"words\":[\"cat\",\"car\",\"dog\"],\"prefix\":\"ca\"}");
            case "suffix-array-suffix-automaton" -> new SimulationSpec("SUFFIX_STRUCTURE", "{\"text\":\"banana\"}");
            case "string-hashing" -> new SimulationSpec("STRING_HASHING", "{\"text\":\"abracadabra\",\"pattern\":\"abra\"}");
            case "algebra" -> new SimulationSpec("ALGEBRA", "{\"a\":3,\"b\":2,\"x\":4}");
            case "linear-algebra" -> new SimulationSpec("LINEAR_ALGEBRA", "{\"matrix\":[[1,2],[3,4]],\"vector\":[5,6]}");
            case "number-theory" -> new SimulationSpec("NUMBER_THEORY", "{\"a\":84,\"b\":30}");
            case "combinatorics" -> new SimulationSpec("COMBINATORICS", "{\"n\":5}");
            case "geometry" -> new SimulationSpec("GEOMETRY", "{\"a\":[0,0],\"b\":[4,1],\"c\":[2,5]}");
            default -> null;
        };
    }

    private List<QuestionSpec> quizFor(String title) {
        return switch (slug(title)) {
            case "recursion" -> List.of(
                    new QuestionSpec("What stops a recursive function?", "A base case", "A larger input", "A random value", "A network call", 0, "A base case terminates the recursive descent."),
                    new QuestionSpec("What does factorial(0) conventionally return?", "0", "1", "-1", "Undefined", 1, "The empty product is 1."),
                    new QuestionSpec("Each recursive call should make progress toward what?", "The base case", "A new class", "A database", "A larger stack", 0, "Progress toward a base case prevents infinite recursion."));
            case "binary-search" -> List.of(
                    new QuestionSpec("What must be true before binary search starts?", "Data is sorted", "Data is encrypted", "Data is duplicated", "Data is random", 0, "Halving the search interval relies on sorted order."),
                    new QuestionSpec("How many items can binary search discard per step?", "About half", "Exactly one", "None", "All", 0, "Each comparison removes one half of the remaining interval."),
                    new QuestionSpec("What is the usual time complexity?", "O(log n)", "O(n²)", "O(1) always", "O(n!)", 0, "The interval halves on each iteration."));
            case "bfs" -> List.of(
                    new QuestionSpec("Which structure drives breadth-first search?", "Queue", "Stack", "Heap only", "Hash map only", 0, "A FIFO queue visits a graph layer by layer."),
                    new QuestionSpec("BFS gives shortest edge count paths in which graph?", "Unweighted graph", "Any weighted graph", "Only a tree with weights", "No graph", 0, "All edges have equal cost in an unweighted graph."),
                    new QuestionSpec("When should a node be marked visited?", "When first enqueued", "After every neighbor", "Never", "Only at the end", 0, "Marking on enqueue avoids duplicate work."));
            case "dsu" -> List.of(
                    new QuestionSpec("What does DSU maintain?", "Disjoint sets", "Sorted arrays only", "A call stack", "A queue", 0, "Disjoint Set Union tracks connected components."),
                    new QuestionSpec("Which operation returns a component representative?", "find", "push", "rotate", "slice", 0, "find follows parent links to a representative."),
                    new QuestionSpec("Which heuristics make DSU nearly constant amortized?", "Path compression and union by rank", "Sorting and hashing", "Recursion and BFS", "Locking and paging", 0, "Both heuristics keep trees shallow."));
            default -> List.of();
        };
    }

    private String slug(String value) {
        String normalized = value.toLowerCase(Locale.ROOT)
                .replace("c++", "c-plus-plus")
                .replace("c#", "c-sharp");
        return normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private record SimulationSpec(String type, String configJson) { }
    private record QuestionSpec(String prompt, String a, String b, String c, String d, int correct, String explanation) { }
}
