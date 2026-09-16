package application.backend.dto;

public final class AdminContentViews {
    private AdminContentViews() {
    }

    public record TopicItem(
            Long id, String slug, String title, String description, int position, boolean published) {
    }

    public record ModuleItem(
            Long id, Long topicId, String title, String description, int position) {
    }

    public record SubmoduleItem(Long id, Long moduleId, String title, int position) {
    }

    public record LessonItem(
            Long id, Long submoduleId, String slug, String title, String summary, int position, boolean published) {
    }
}
