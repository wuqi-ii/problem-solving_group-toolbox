package cn.edu.teamtoolbox.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_]{4,32}$") String account,
        @NotBlank @Size(min = 8, max = 64) String password,
        @NotBlank @Size(max = 30) String nickname
) {
}
