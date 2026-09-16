package application.client.dto;

import java.util.List;

public final class ApiModels {
    private ApiModels() {
    }

    public record AuthResponse(
            String token,
            String role,
            Long userId,
            String username,
            String displayName) {
    }

    public record TopicSummary(Long id, String slug, String title, String description, int position) {
    }

    public record TopicTree(
            Long id,
            String slug,
            String title,
            List<ModuleView> modules) {
    }

    public record ModuleView(
            Long id,
            String title,
            String description,
            int position,
            List<SubmoduleView> submodules) {
    }

    public record SubmoduleView(
            Long id,
            String title,
            int position,
            List<LessonSummary> lessons) {
    }

    public record LessonSummary(
            Long id,
            String slug,
            String title,
            String summary,
            int position,
            boolean completed) {
    }

    public record LessonDetails(
            Long id,
            Long topicId,
            String title,
            String summary,
            String bodyMarkdown,
            String exampleCode,
            boolean completed,
            int quizQuestionCount,
            SimulationView simulation) {
    }

    public record SimulationView(Long id, String type, String configJson) {
    }

    public record ProgressSummary(
            int completedLessons,
            int totalLessons,
            int quizAttempts,
            List<TopicProgress> topics) {
    }

    public record TopicProgress(
            Long topicId,
            String title,
            int completedLessons,
            int totalLessons) {
    }

    public record QuizQuestionView(Long id, String prompt, List<String> options) {
    }

    public record QuizAttemptResult(int score, int total, List<String> feedback) {
    }

    public record AdminUserView(
            Long id,
            String username,
            String displayName,
            String role,
            int completedLessons,
            int totalLessons,
            int quizAttempts) {
    }

    public record AuthMessageResponse(String message, String code) {
    }
}
