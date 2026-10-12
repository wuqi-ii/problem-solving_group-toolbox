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
        List<TaskAssigneeView> historicalAssignees,
        TaskSubmissionView latestSubmission,
        List<TaskSubmissionView> submissions,
        boolean canStart,
        boolean canSubmit,
        boolean canReview,
        boolean canManage,
        boolean overdue
) {
}
