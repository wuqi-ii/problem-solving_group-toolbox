package cn.edu.teamtoolbox.group;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateInvitationRequest(
        @Min(1) @Max(168) int validHours,
        @Min(1) @Max(100) int maxUses
) {
}
