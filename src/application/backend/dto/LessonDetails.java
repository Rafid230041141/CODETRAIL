package application.backend.dto;

public record LessonDetails(Long id, Long topicId, String title, String summary, String bodyMarkdown,
                            String exampleCode, boolean completed, int quizQuestionCount,
                            SimulationView simulation) {
}
