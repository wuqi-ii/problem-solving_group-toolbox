package cn.edu.teamtoolbox.collaboration;

import cn.edu.teamtoolbox.common.error.BusinessException;
import cn.edu.teamtoolbox.common.error.ErrorCode;
import cn.edu.teamtoolbox.group.GroupAuthorizationService;
import cn.edu.teamtoolbox.group.GroupMembershipRepository;
import cn.edu.teamtoolbox.notification.NotificationService;
import cn.edu.teamtoolbox.permission.PermissionType;
import cn.edu.teamtoolbox.task.TaskAssignmentRepository;
import cn.edu.teamtoolbox.task.TaskEntity;
import cn.edu.teamtoolbox.task.TaskRepository;
import cn.edu.teamtoolbox.user.UserEntity;
import cn.edu.teamtoolbox.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CollaborationService {
    private final CollaborationPostRepository postRepository;
    private final GroupAuthorizationService authorizationService;
    private final GroupMembershipRepository membershipRepository;
    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public CollaborationService(
            CollaborationPostRepository postRepository,
            GroupAuthorizationService authorizationService,
            GroupMembershipRepository membershipRepository,
            TaskRepository taskRepository,
            TaskAssignmentRepository assignmentRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.postRepository = postRepository;
        this.authorizationService = authorizationService;
        this.membershipRepository = membershipRepository;
        this.taskRepository = taskRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<CollaborationPostView> listGroupPosts(
            String groupId, CollaborationPostType type, String requesterId
    ) {
        authorizationService.requireMember(groupId, requesterId);
        List<CollaborationPostEntity> posts = postRepository
                .findAllByGroupIdAndTypeAndStatusOrderByPinnedDescCreatedAtDesc(groupId, type, "ACTIVE");
        return toViews(posts, requesterId);
    }

    @Transactional
    public CollaborationPostView createGroupPost(
            String groupId, CollaborationPostType type, String requesterId, CreatePostRequest request
    ) {
        authorizationService.requireMember(groupId, requesterId);
        if (type == CollaborationPostType.ANNOUNCEMENT) {
            authorizationService.requireLeaderOrPermission(groupId, requesterId, PermissionType.GROUP_ANNOUNCE);
        } else if (type != CollaborationPostType.DISCUSSION) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "不支持的协作内容类型");
        }
        CollaborationPostEntity post = postRepository.save(new CollaborationPostEntity(
                groupId, null, type, request.content().trim(), requesterId));
        if (type == CollaborationPostType.ANNOUNCEMENT) notifyAnnouncement(post);
        return toViews(List.of(post), requesterId).get(0);
    }

    @Transactional(readOnly = true)
    public List<CollaborationPostView> listTaskComments(String taskId, String requesterId) {
        TaskEntity task = requireTask(taskId);
        authorizationService.requireMember(task.getGroupId(), requesterId);
        return toViews(postRepository.findAllByTaskIdAndTypeAndStatusOrderByCreatedAtAsc(
                taskId, CollaborationPostType.TASK_COMMENT, "ACTIVE"), requesterId);
    }

    @Transactional
    public CollaborationPostView createTaskComment(
            String taskId, String requesterId, CreatePostRequest request
    ) {
        TaskEntity task = requireTask(taskId);
        authorizationService.requireMember(task.getGroupId(), requesterId);
        CollaborationPostEntity post = postRepository.save(new CollaborationPostEntity(
                task.getGroupId(), taskId, CollaborationPostType.TASK_COMMENT,
                request.content().trim(), requesterId));
        notifyTaskComment(task, post);
        return toViews(List.of(post), requesterId).get(0);
    }

    @Transactional
    public CollaborationPostView setPinned(String postId, boolean pinned, String requesterId) {
        CollaborationPostEntity post = requireActivePost(postId);
        if (post.getType() != CollaborationPostType.ANNOUNCEMENT) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有公告可以置顶");
        }
        authorizationService.requireLeaderOrPermission(
                post.getGroupId(), requesterId, PermissionType.GROUP_ANNOUNCE);
        post.setPinned(pinned);
        return toViews(List.of(post), requesterId).get(0);
    }

    @Transactional
    public void delete(String postId, String requesterId) {
        CollaborationPostEntity post = requireActivePost(postId);
        authorizationService.requireMember(post.getGroupId(), requesterId);
        if (!post.getCreatedBy().equals(requesterId) && !canModerate(post, requesterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能删除自己发布的内容");
        }
        post.delete();
    }

    private List<CollaborationPostView> toViews(List<CollaborationPostEntity> posts, String requesterId) {
        Map<String, UserEntity> authors = userRepository.findAllById(
                        posts.stream().map(CollaborationPostEntity::getCreatedBy).distinct().toList()).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        return posts.stream().map(post -> {
            UserEntity author = authors.get(post.getCreatedBy());
            boolean canPin = post.getType() == CollaborationPostType.ANNOUNCEMENT
                    && authorizationService.isLeaderOrHas(
                    post.getGroupId(), requesterId, PermissionType.GROUP_ANNOUNCE);
            return new CollaborationPostView(
                    post.getId(), post.getGroupId(), post.getTaskId(), post.getType(), post.getContent(),
                    post.getCreatedBy(), author == null ? "已离组成员" : author.getNickname(), post.isPinned(),
                    post.getCreatedBy().equals(requesterId) || canModerate(post, requesterId),
                    canPin, post.getCreatedAt());
        }).toList();
    }

    private boolean canModerate(CollaborationPostEntity post, String requesterId) {
        PermissionType permission = post.getType() == CollaborationPostType.ANNOUNCEMENT
                ? PermissionType.GROUP_ANNOUNCE : PermissionType.MEMBER_MANAGE;
        return authorizationService.isLeaderOrHas(post.getGroupId(), requesterId, permission);
    }

    private void notifyAnnouncement(CollaborationPostEntity post) {
        membershipRepository.findAllByGroupIdAndStatus(post.getGroupId(), "ACTIVE").stream()
                .map(item -> item.getUserId())
                .filter(userId -> !userId.equals(post.getCreatedBy()))
                .forEach(userId -> notificationService.create(
                        userId, "announcement:" + post.getId(), "GROUP_ANNOUNCEMENT",
                        "小组发布了新公告", summarize(post.getContent()), "/collaboration"));
    }

    private void notifyTaskComment(TaskEntity task, CollaborationPostEntity post) {
        LinkedHashSet<String> recipients = new LinkedHashSet<>();
        recipients.add(task.getCreatedBy());
        assignmentRepository.findAllByTaskIdAndStatus(task.getId(), "ACTIVE").stream()
                .map(item -> item.getUserId()).forEach(recipients::add);
        recipients.remove(post.getCreatedBy());
        recipients.stream()
                .filter(userId -> membershipRepository.findByGroupIdAndUserIdAndStatus(
                        task.getGroupId(), userId, "ACTIVE").isPresent())
                .forEach(userId -> notificationService.create(
                userId, "task-comment:" + post.getId(), "TASK_COMMENT",
                "任务有新留言：" + task.getTitle(), summarize(post.getContent()), "/tasks"));
    }

    private String summarize(String content) {
        return content.length() <= 480 ? content : content.substring(0, 480) + "…";
    }

    private TaskEntity requireTask(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "任务不存在"));
    }

    private CollaborationPostEntity requireActivePost(String postId) {
        return postRepository.findById(postId)
                .filter(post -> "ACTIVE".equals(post.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "协作内容不存在"));
    }
}
