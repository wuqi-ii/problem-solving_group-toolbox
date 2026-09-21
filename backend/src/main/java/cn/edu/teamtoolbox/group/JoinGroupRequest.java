package cn.edu.teamtoolbox.group;

import jakarta.validation.constraints.NotBlank;

public record JoinGroupRequest(@NotBlank String code) {
}
