package cn.edu.teamtoolbox.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitTaskRequest(
        @NotBlank @Size(max = 2000) String content,
        String attachmentFileId
) {
}
