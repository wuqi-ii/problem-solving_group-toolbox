package cn.edu.teamtoolbox.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record CreateTaskRequest(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 2000) String description,
        @NotNull TaskPriority priority,
        Instant dueAt,
        @NotEmpty List<String> assigneeIds
) {
}
