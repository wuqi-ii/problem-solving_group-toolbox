package cn.edu.teamtoolbox.task;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewTaskRequest(
        @NotNull ReviewDecision decision,
        @Size(max = 1000) String comment
) {
}
