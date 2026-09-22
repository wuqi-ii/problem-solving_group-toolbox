package cn.edu.teamtoolbox.file;

import java.time.Instant;

public record FileView(
        String id,
        String groupId,
        String name,
        String contentType,
        long sizeBytes,
        String uploadedBy,
        String uploaderName,
        Instant createdAt,
        boolean canManage
) {
}
