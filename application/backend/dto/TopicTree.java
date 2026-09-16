package application.backend.dto;

import java.util.List;

public record TopicTree(Long id, String slug, String title, List<ModuleView> modules) {
}
