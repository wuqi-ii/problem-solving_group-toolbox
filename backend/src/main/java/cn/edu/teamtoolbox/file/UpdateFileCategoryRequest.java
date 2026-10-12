package cn.edu.teamtoolbox.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateFileCategoryRequest(
        @NotBlank
        @Pattern(regexp = "GENERAL|DOCUMENT|IMAGE|DATA|ARCHIVE|TASK_RESULT", message = "文件分类不正确")
        String category
) {}
