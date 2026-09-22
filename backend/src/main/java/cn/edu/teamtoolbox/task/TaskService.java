package cn.edu.teamtoolbox.task;

import cn.edu.teamtoolbox.common.error.BusinessException;
import cn.edu.teamtoolbox.common.error.ErrorCode;
import cn.edu.teamtoolbox.group.GroupAuthorizationService;
import cn.edu.teamtoolbox.group.GroupMembershipRepository;
import cn.edu.teamtoolbox.file.GroupFileEntity;
import cn.edu.teamtoolbox.file.GroupFileRepository;
import cn.edu.teamtoolbox.permission.PermissionType;
import cn.edu.teamtoolbox.notification.NotificationService;
import cn.edu.teamtoolbox.user.UserEntity;
import cn.edu.teamtoolbox.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository assignmentRepository;
    private final TaskSubmissionRepository submissionRepository;
    private final TaskReviewRepository reviewRepository;
    private final GroupMembershipRepository membershipRepository;
    private final GroupAuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final GroupFileRepository fileRepository;
    private final NotificationService notificationService;

    public TaskService(
            TaskRepository taskRepository,
            TaskAssignmentRepository assignmentRepository,
            TaskSubmissionRepository submissionRepository,
            TaskReviewRepository reviewRepository,
            GroupMembershipRepository membershipRepository,
            GroupAuthorizationService authorizationService,
            UserRepository userRepository,
            GroupFileRepository fileRepository,
            NotificationService notificationService
    ) {
        this.taskRepository = taskRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.reviewRepository = reviewRepository;
        this.membershipRepository = membershipRepository;
        this.authorizationService = authorizationService;
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public TaskView create(String groupId, String requesterId, CreateTaskRequest request) {
        authorizationService.requireLeaderOrPermission(groupId, requesterId, PermissionType.TASK_CREATE);
        if (request.dueAt() != null && request.dueAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "截止时间必须晚于当前时间");
        }
        List<String> assigneeIds = new LinkedHashSet<>(request.assigneeIds()).stream().toList();
        assigneeIds.forEach(userId -> membershipRepository
                .findByGroupIdAndUserIdAndStatus(groupId, userId, "ACTIVE")
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_ARGUMENT, "被指派人不是小组成员")));

        TaskEntity task = taskRepository.save(new TaskEntity(
                groupId, request.title().trim(), normalize(request.description()), request.priority(),
                requesterId, request.dueAt()));
        assignmentRepository.saveAll(assigneeIds.stream()
                .map(userId -> new TaskAssignmentEntity(task.getId(), userId)).toList());
        assigneeIds.stream().filter(userId -> !userId.equals(requesterId)).forEach(userId ->
                notificationService.create(userId, "task-assigned:" + task.getId(), "TASK_ASSIGNED",
                        "你有一个新任务", task.getTitle(), "/tasks"));
        return toView(task, requesterId);
    }

    @Transactional(readOnly = true)
    public List<TaskView> listByGroup(String groupId, String requesterId) {
        authorizationService.requireMember(groupId, requesterId);
        return taskRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(task -> toView(task, requesterId)).toList();
    }

    @Transactional
    public TaskView start(String taskId, String requesterId) {
        TaskEntity task = requireTask(taskId);
        requireAssignee(taskId, requesterId);
        if (task.getStatus() != TaskStatus.OPEN && task.getStatus() != TaskStatus.NEEDS_CHANGES) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前任务状态无法开始");
        }
        task.setStatus(TaskStatus.IN_PROGRESS);
        return toView(task, requesterId);
    }

    @Transactional
    public TaskView submit(String taskId, String requesterId, SubmitTaskRequest request) {
        TaskEntity task = requireTask(taskId);
        requireAssignee(taskId, requesterId);
        if (!List.of(TaskStatus.OPEN, TaskStatus.IN_PROGRESS, TaskStatus.NEEDS_CHANGES).contains(task.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前任务状态无法提交");
        }
        int version = submissionRepository.findTopByTaskIdOrderByVersionNoDesc(taskId)
                .map(item -> item.getVersionNo() + 1).orElse(1);
        String attachmentFileId = normalize(request.attachmentFileId());
        if (attachmentFileId != null) {
            fileRepository.findById(attachmentFileId)
                    .filter(file -> "ACTIVE".equals(file.getStatus()) && task.getGroupId().equals(file.getGroupId()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_ARGUMENT, "附件不属于当前小组或已被删除"));
        }
        submissionRepository.save(new TaskSubmissionEntity(
                taskId, requesterId, version, request.content().trim(), attachmentFileId));
        task.setStatus(TaskStatus.SUBMITTED);
        return toView(task, requesterId);
    }

    @Transactional
    public TaskView review(String taskId, String requesterId, ReviewTaskRequest request) {
        TaskEntity task = requireTask(taskId);
        authorizationService.requireLeaderOrPermission(task.getGroupId(), requesterId, PermissionType.TASK_REVIEW);
        if (task.getStatus() != TaskStatus.SUBMITTED) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有待审核任务可以审核");
        }
        TaskSubmissionEntity submission = submissionRepository.findTopByTaskIdOrderByVersionNoDesc(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "任务还没有提交记录"));
        if (reviewRepository.findBySubmissionId(submission.getId()).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前提交版本已经审核");
        }
        reviewRepository.save(new TaskReviewEntity(
                taskId, submission.getId(), requesterId, request.decision(), normalize(request.comment())));
        task.setStatus(request.decision() == ReviewDecision.APPROVED
                ? TaskStatus.COMPLETED : TaskStatus.NEEDS_CHANGES);
        String result = request.decision() == ReviewDecision.APPROVED ? "已通过" : "需要修改";
        notificationService.create(submission.getSubmittedBy(), "task-reviewed:" + submission.getId(),
                "TASK_REVIEWED", "任务审核结果：" + result, task.getTitle(), "/tasks");
        return toView(task, requesterId);
    }

    private TaskView toView(TaskEntity task, String requesterId) {
        List<TaskAssignmentEntity> assignments = assignmentRepository.findAllByTaskIdAndStatus(task.getId(), "ACTIVE");
        Map<String, UserEntity> users = userRepository.findAllById(
                        assignments.stream().map(TaskAssignmentEntity::getUserId).toList()).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        List<TaskAssigneeView> assignees = assignments.stream()
                .filter(item -> users.containsKey(item.getUserId()))
                .map(item -> new TaskAssigneeView(item.getUserId(), users.get(item.getUserId()).getNickname()))
                .toList();
        TaskSubmissionView latest = submissionRepository.findTopByTaskIdOrderByVersionNoDesc(task.getId())
                .map(submission -> {
                    TaskReviewEntity review = reviewRepository.findBySubmissionId(submission.getId()).orElse(null);
                    GroupFileEntity attachment = submission.getAttachmentFileId() == null ? null
                            : fileRepository.findById(submission.getAttachmentFileId()).orElse(null);
                    return new TaskSubmissionView(
                            submission.getId(), submission.getVersionNo(), submission.getContent(),
                            submission.getAttachmentFileId(), attachment == null ? null : attachment.getDisplayName(),
                            submission.getSubmittedBy(), submission.getSubmittedAt(),
                            review == null ? null : review.getDecision(),
                            review == null ? null : review.getComment(),
                            review == null ? null : review.getReviewedAt());
                }).orElse(null);
        boolean assignee = assignmentRepository.existsByTaskIdAndUserIdAndStatus(task.getId(), requesterId, "ACTIVE");
        boolean canReview = task.getStatus() == TaskStatus.SUBMITTED
                && authorizationService.isLeaderOrHas(task.getGroupId(), requesterId, PermissionType.TASK_REVIEW);
        return new TaskView(
                task.getId(), task.getGroupId(), task.getTitle(), task.getDescription(), task.getPriority(),
                task.getStatus(), task.getCreatedBy(), task.getDueAt(), assignees, latest,
                assignee && List.of(TaskStatus.OPEN, TaskStatus.NEEDS_CHANGES).contains(task.getStatus()),
                assignee && List.of(TaskStatus.OPEN, TaskStatus.IN_PROGRESS, TaskStatus.NEEDS_CHANGES).contains(task.getStatus()),
                canReview);
    }

    private TaskEntity requireTask(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "任务不存在"));
    }

    private void requireAssignee(String taskId, String userId) {
        if (!assignmentRepository.existsByTaskIdAndUserIdAndStatus(taskId, userId, "ACTIVE")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "你不是该任务的执行人");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
