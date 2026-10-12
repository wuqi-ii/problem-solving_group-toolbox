package cn.edu.teamtoolbox.collaboration;

import java.time.Instant;

public record CollaborationPostView(
        String id,
        String groupId,
        String taskId,
        CollaborationPostType type,
        String content,
        String createdBy,
        String authorName,
        boolean pinned,
        boolean canDelete,
        boolean canPin,
        Instant createdAt
) {}
