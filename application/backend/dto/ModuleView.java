package application.backend.dto;

import java.util.List;

public record ModuleView(Long id, String title, String description, int position, List<SubmoduleView> submodules) {
}
