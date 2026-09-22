package cn.edu.teamtoolbox.task;

import java.time.Instant;
import java.util.List;

public record TaskView(
        String id,
        String groupId,
        String title,
        String description,
        TaskPriority priority,
        TaskStatus status,
        String createdBy,
        Instant dueAt,
        List<TaskAssigneeView> assignees,
        TaskSubmissionView latestSubmission,
        boolean canStart,
        boolean canSubmit,
        boolean canReview
) {
}
