package cn.edu.teamtoolbox.permission;

import jakarta.validation.constraints.NotNull;

public record GrantPermissionRequest(@NotNull PermissionType permission) {
}
