package application;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import application.backend.BackendApplication;
import application.client.dto.ApiModels.AdminUserView;
import application.client.dto.ApiModels.AuthResponse;
import application.client.dto.ApiModels.LessonDetails;
import application.client.dto.ApiModels.LessonSummary;
import application.client.dto.ApiModels.QuizAttemptResult;
import application.client.dto.ApiModels.TopicTree;
import application.client.service.ApiClient;
import application.client.service.ApiClient.ApiException;
import application.client.simulation.SimulationEngine;
import application.client.simulation.SimulationEngine.SimulationRun;

public final class VerificationHarness {
    private VerificationHarness() {
    }

    public static void main(String[] args) throws Exception {
        try (ConfigurableApplicationContext context = startBackend()) {
            int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
            ApiClient api = context.getBean(ApiClient.class);
            api.setBaseUri(URI.create("http://127.0.0.1:" + port + "/api/"));
            verifyPhaseTwo(api);
        }
        System.out.println("ALL PHASE 2 BACKEND CHECKS PASSED");
    }

    private static ConfigurableApplicationContext startBackend() {
        String databaseUrl = environment("TEST_DATABASE_URL",
                "jdbc:h2:mem:codetrail;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH");
        String driver = databaseUrl.startsWith("jdbc:postgresql:")
                ? "org.postgresql.Driver" : "org.h2.Driver";
        return new SpringApplicationBuilder(BackendApplication.class)
                .web(WebApplicationType.SERVLET)
                .headless(true)
                .run(
                        "--server.port=0",
                        "--spring.datasource.url=" + databaseUrl,
                        "--spring.datasource.driver-class-name=" + driver,
                        "--spring.datasource.username=" + environment("TEST_DATABASE_USER", "sa"),
                        "--spring.datasource.password=" + environment("TEST_DATABASE_PASSWORD", ""),
                        "--spring.jpa.hibernate.ddl-auto=create-drop",
                        "--spring.jpa.open-in-view=false",
                        "--logging.level.root=ERROR");
    }

    private static String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void verifyPhaseTwo(ApiClient api) throws Exception {
        AuthResponse newStudent = await(api.register(
                "phase2student", "Phase Two Student", "student123", "STUDENT"));
        check("STUDENT".equals(newStudent.role()), "student registration issues a student JWT");
        AuthResponse newAdmin = await(api.register(
                "phase2admin", "Phase Two Admin", "administrator123", "ADMIN"));
        check("ADMIN".equals(newAdmin.role()), "admin registration issues an admin JWT");

        AuthResponse student = await(api.login("phase2student", "student123"));
        api.useSession(student);
        var topics = await(api.topics());
        check(topics.size() == 2, "only Languages and DSA/CP are published");
        check(topics.stream().map(topic -> topic.title()).toList()
                .equals(List.of("Languages", "DSA / Competitive Programming")),
                "published topics match the Phase 2 scope");

        TopicTree languages = await(api.topicTree(topics.get(0).id()));
        TopicTree dsa = await(api.topicTree(topics.get(1).id()));
        check(languages.modules().size() == 6 && lessonCount(languages) == 79,
                "Languages contains all six language tracks and 79 seeded lessons");
        check(dsa.modules().size() == 8 && lessonCount(dsa) == 54,
                "DSA/CP contains all eight branches and 54 seeded lessons");

        ObjectMapper simulationMapper = new ObjectMapper();
        int dsaSimulationCount = 0;
        for (LessonSummary summary : lessons(dsa)) {
            LessonDetails details = await(api.lesson(summary.id()));
            check(details.simulation() != null,
                    "DSA lesson " + summary.title() + " exposes a simulation");
            if (details.simulation() != null) {
                dsaSimulationCount++;
                SimulationRun run = SimulationEngine.build(simulationMapper,
                        details.simulation().type(), details.simulation().configJson());
                check(!run.pseudocode().isEmpty() && run.steps().size() > 1
                                && run.steps().stream().allMatch(step -> !step.cells().isEmpty()),
                        "DSA lesson " + summary.title() + " builds a multi-step visualization");
            }
        }
        check(dsaSimulationCount == lessonCount(dsa),
                "all DSA lessons expose buildable simulations");

        LessonSummary recursion = lesson(dsa, "Recursion");
        LessonDetails recursionDetails = await(api.lesson(recursion.id()));
        check(recursionDetails.bodyMarkdown().contains("## Core idea")
                        && recursionDetails.simulation() != null
                        && "RECURSION".equals(recursionDetails.simulation().type()),
                "real lesson content and the recursion simulation load over HTTP");
        check(await(api.quiz(recursion.id())).size() == 3,
                "the Recursion lesson exposes a three-question quiz without answers");

        for (String title : List.of("Merge", "Quick", "Binary Search", "Binary Search on Answer", "BFS", "DFS",
                "Segment Tree", "Fenwick Tree", "DSU", "Heap", "Dijkstra", "Kruskal", "Prim")) {
            check(await(api.lesson(lesson(dsa, title).id())).simulation() != null,
                    title + " has an interactive simulation");
        }

        await(api.enroll(topics.get(1).id()));
        await(api.updateCompletion(recursion.id(), true));
        check(await(api.progress()).completedLessons() == 1,
                "lesson completion persists through the progress API");
        QuizAttemptResult quiz = await(api.submitQuiz(recursion.id(), List.of(0, 1, 0)));
        check(quiz.score() == 3 && quiz.total() == 3 && quiz.feedback().size() == 3,
                "quiz submission is scored and returns per-question feedback");
        check(await(api.progress()).quizAttempts() == 1,
                "quiz attempts update the student progress summary");

        expectForbidden(api, "student cannot access the admin user endpoint");

        api.useSession(await(api.login("phase2admin", "administrator123")));
        List<AdminUserView> users = await(api.adminUsers());
        AdminUserView reflected = users.stream()
                .filter(user -> user.username().equals("phase2student"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("FAILED: registered student missing from admin list"));
        check(reflected.completedLessons() == 1 && reflected.quizAttempts() == 1,
                "admin dashboard reflects the student's completion and quiz progress");

        int initialAdminTopics = await(api.adminTopics()).size();
        Map<String, Object> draft = await(api.createAdminTopic(Map.of(
                "slug", "phase-two-crud-check",
                "title", "CRUD check",
                "description", "Temporary unpublished content used by verification.",
                "position", 99,
                "published", false)));
        long draftId = ((Number) draft.get("id")).longValue();
        check(await(api.adminTopics()).size() == initialAdminTopics + 1,
                "admin content Create and Read endpoints work over HTTP");
        await(api.updateAdminTopic(draftId, Map.of(
                "slug", "phase-two-crud-check",
                "title", "Updated CRUD check",
                "description", "Updated temporary content.",
                "position", 99,
                "published", false)));
        check(await(api.adminTopics()).stream()
                .anyMatch(topic -> draftId == ((Number) topic.get("id")).longValue()
                        && "Updated CRUD check".equals(topic.get("title"))),
                "admin content Update endpoint persists changes");
        await(api.deleteAdminTopic(draftId));
        check(await(api.adminTopics()).size() == initialAdminTopics,
                "admin content Delete endpoint removes the draft");
        check(await(api.topics()).size() == 2,
                "admin CRUD does not expose deferred or unpublished topics to students");
    }

    private static void expectForbidden(ApiClient api, String description) {
        try {
            await(api.adminUsers());
            throw new AssertionError("FAILED: " + description);
        } catch (Exception exception) {
            Throwable current = exception;
            while (current.getCause() != null) {
                current = current.getCause();
            }
            check(current instanceof ApiException apiError && apiError.statusCode() == 403, description);
        }
    }

    private static LessonSummary lesson(TopicTree topic, String title) {
        return topic.modules().stream()
                .flatMap(module -> module.submodules().stream())
                .flatMap(submodule -> submodule.lessons().stream())
                .filter(candidate -> candidate.title().equals(title))
                .findFirst()
                .orElseThrow(() -> new AssertionError("FAILED: missing lesson " + title));
    }

    private static List<LessonSummary> lessons(TopicTree topic) {
        return topic.modules().stream()
                .flatMap(module -> module.submodules().stream())
                .flatMap(submodule -> submodule.lessons().stream())
                .toList();
    }

    private static long lessonCount(TopicTree topic) {
        return topic.modules().stream()
                .flatMap(module -> module.submodules().stream())
                .mapToLong(submodule -> submodule.lessons().size())
                .sum();
    }

    private static <T> T await(java.util.concurrent.CompletableFuture<T> future) throws Exception {
        return future.get(20, TimeUnit.SECONDS);
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError("FAILED: " + description);
        }
        System.out.println("PASS: " + description);
    }
}
