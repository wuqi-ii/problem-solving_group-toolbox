package cn.edu.teamtoolbox.collaboration;

import jakarta.validation.constraints.NotNull;

public record PinPostRequest(@NotNull(message = "置顶状态不能为空") Boolean pinned) {}
