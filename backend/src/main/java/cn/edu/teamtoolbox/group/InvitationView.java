package cn.edu.teamtoolbox.group;

import java.time.Instant;

public record InvitationView(String id, String code, Instant expiresAt, int maxUses, int usedCount,
                             String status, Instant createdAt) {
    static InvitationView from(GroupInvitationEntity invitation) {
        return new InvitationView(invitation.getId(), invitation.getCode(), invitation.getExpiresAt(),
                invitation.getMaxUses(), invitation.getUsedCount(), invitation.getStatus(), invitation.getCreatedAt());
    }
}
