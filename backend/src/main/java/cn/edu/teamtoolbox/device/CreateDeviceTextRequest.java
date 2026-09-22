package cn.edu.teamtoolbox.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDeviceTextRequest(
        @Pattern(regexp = "TEXT|LINK") String type,
        @NotBlank @Size(max = 4000) String content,
        @Size(max = 100) String sourceDevice
) {}
