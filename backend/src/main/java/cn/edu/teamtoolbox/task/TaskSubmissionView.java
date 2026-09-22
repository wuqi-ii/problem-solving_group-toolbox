package cn.edu.teamtoolbox.task;

import java.time.Instant;

public record TaskSubmissionView(
        String id,
        int versionNo,
        String content,
        String attachmentFileId,
        String attachmentName,
        String submittedBy,
        Instant submittedAt,
        ReviewDecision reviewDecision,
        String reviewComment,
        Instant reviewedAt
) {
}
