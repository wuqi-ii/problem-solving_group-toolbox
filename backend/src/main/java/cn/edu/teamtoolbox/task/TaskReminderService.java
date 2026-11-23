package cn.edu.teamtoolbox.task;

import cn.edu.teamtoolbox.notification.NotificationService;
import cn.edu.teamtoolbox.group.GroupMembershipRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TaskReminderService {
    private static final List<TaskStatus> ACTIVE_STATUSES = List.of(
            TaskStatus.OPEN, TaskStatus.IN_PROGRESS, TaskStatus.NEEDS_CHANGES);
    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository assignmentRepository;
    private final GroupMembershipRepository membershipRepository;
    private final NotificationService notificationService;

    public TaskReminderService(
            TaskRepository taskRepository,
            TaskAssignmentRepository assignmentRepository,
            GroupMembershipRepository membershipRepository,
            NotificationService notificationService
    ) {
        this.taskRepository = taskRepository;
        this.assignmentRepository = assignmentRepository;
        this.membershipRepository = membershipRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelayString = "${app.task.reminder-interval-ms:900000}")
    @Transactional
    public void sendReminders() {
        Instant now = Instant.now();
        taskRepository.findAllByStatusInAndDueAtBetween(ACTIVE_STATUSES, now, now.plus(24, ChronoUnit.HOURS))
                .forEach(task -> notifyAssignees(task, "TASK_DUE_SOON", "任务将在 24 小时内截止", "due-soon"));
        taskRepository.findAllByStatusInAndDueAtBefore(ACTIVE_STATUSES, now)
                .forEach(task -> notifyAssignees(task, "TASK_OVERDUE", "任务已逾期", "overdue"));
    }

    private void notifyAssignees(TaskEntity task, String type, String title, String eventType) {
        String dueVersion = String.valueOf(task.getDueAt().toEpochMilli());
        assignmentRepository.findAllByTaskIdAndStatus(task.getId(), "ACTIVE").stream()
                .filter(assignment -> membershipRepository.findByGroupIdAndUserIdAndStatus(
                        task.getGroupId(), assignment.getUserId(), "ACTIVE").isPresent())
                .forEach(assignment ->
                notificationService.create(
                        assignment.getUserId(), "task-" + eventType + ":" + task.getId() + ":" + dueVersion,
                        type, title, task.getTitle(), "/tasks?group=" + task.getGroupId() + "&item=" + task.getId()));
    }
}
