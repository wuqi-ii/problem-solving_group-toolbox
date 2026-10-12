package cn.edu.teamtoolbox.collaboration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotBlank(message = "内容不能为空")
        @Size(max = 2000, message = "内容不能超过 2000 字")
        String content
) {}
