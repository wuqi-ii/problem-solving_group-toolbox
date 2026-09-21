package cn.edu.teamtoolbox.group;

import java.time.Instant;

public record InvitationView(String code, Instant expiresAt, int maxUses, int usedCount) {
    static InvitationView from(GroupInvitationEntity invitation) {
        return new InvitationView(invitation.getCode(), invitation.getExpiresAt(),
                invitation.getMaxUses(), invitation.getUsedCount());
    }
}
