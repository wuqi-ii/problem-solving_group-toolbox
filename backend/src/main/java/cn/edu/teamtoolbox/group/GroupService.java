package cn.edu.teamtoolbox.group;

import cn.edu.teamtoolbox.common.error.BusinessException;
import cn.edu.teamtoolbox.common.error.ErrorCode;
import cn.edu.teamtoolbox.permission.PermissionGrantEntity;
import cn.edu.teamtoolbox.permission.PermissionGrantRepository;
import cn.edu.teamtoolbox.permission.PermissionType;
import cn.edu.teamtoolbox.notification.NotificationService;
import cn.edu.teamtoolbox.user.UserEntity;
import cn.edu.teamtoolbox.user.UserRepository;
import cn.edu.teamtoolbox.task.TaskAssignmentRepository;
import cn.edu.teamtoolbox.task.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GroupService {
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final GroupInvitationRepository invitationRepository;
    private final PermissionGrantRepository permissionRepository;
    private final NotificationService notificationService;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskRepository taskRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public GroupService(
            GroupRepository groupRepository,
            GroupMembershipRepository membershipRepository,
            UserRepository userRepository,
            GroupInvitationRepository invitationRepository,
            PermissionGrantRepository permissionRepository,
            NotificationService notificationService,
            TaskAssignmentRepository taskAssignmentRepository,
            TaskRepository taskRepository
    ) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
        this.permissionRepository = permissionRepository;
        this.notificationService = notificationService;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public GroupView create(String userId, CreateGroupRequest request) {
        GroupEntity group = groupRepository.save(new GroupEntity(
                request.name().trim(), normalizeDescription(request.description()), userId));
        GroupMembershipEntity membership = membershipRepository.save(
                new GroupMembershipEntity(group.getId(), userId, "LEADER"));
        return GroupView.from(group, membership.getRole(), List.of());
    }

    @Transactional(readOnly = true)
    public List<GroupView> listMine(String userId) {
        return membershipRepository.findAllByUserIdAndStatus(userId, "ACTIVE").stream()
                .map(membership -> groupRepository.findById(membership.getGroupId())
                        .filter(group -> "ACTIVE".equals(group.getStatus()))
                        .map(group -> GroupView.from(group, membership.getRole(),
                                activePermissions(group.getId(), userId)))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MemberView> listMembers(String groupId, String requesterId) {
        requireMembership(groupId, requesterId);
        List<GroupMembershipEntity> memberships = membershipRepository.findAllByGroupIdAndStatus(groupId, "ACTIVE");
        Map<String, UserEntity> users = userRepository.findAllById(
                        memberships.stream().map(GroupMembershipEntity::getUserId).toList()).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        Map<String, List<String>> permissions = new HashMap<>();
        permissionRepository.findAllByGroupIdAndUserIdInAndRevokedAtIsNull(
                        groupId, memberships.stream().map(GroupMembershipEntity::getUserId).toList())
                .forEach(grant -> permissions.computeIfAbsent(grant.getUserId(), ignored -> new ArrayList<>())
                        .add(grant.getPermission()));
        return memberships.stream()
                .filter(membership -> users.containsKey(membership.getUserId()))
                .map(membership -> {
                    UserEntity user = users.get(membership.getUserId());
                    return new MemberView(user.getId(), user.getAccount(), user.getNickname(), membership.getGroupNickname(),
                            membership.getRole(), membership.getJoinedAt(),
                            permissions.getOrDefault(user.getId(), List.of()));
                })
                .toList();
    }

    @Transactional
    public void updateMyNickname(String groupId, String userId, String nickname) {
        requireGroup(groupId);
        GroupMembershipEntity membership = membershipRepository.findByGroupIdAndUserIdAndStatus(groupId, userId, "ACTIVE")
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "你不是该小组成员"));
        membership.setGroupNickname(normalizeDescription(nickname));
    }

    @Transactional
    public InvitationView createInvitation(
            String groupId, String requesterId, CreateInvitationRequest request
    ) {
        requireLeaderOrMemberManage(groupId, requesterId);
        GroupInvitationEntity invitation = new GroupInvitationEntity(
                groupId, newInvitationCode(), requesterId,
                Instant.now().plus(request.validHours(), ChronoUnit.HOURS), request.maxUses());
        return InvitationView.from(invitationRepository.save(invitation));
    }

    @Transactional
    public List<InvitationView> listInvitations(String groupId, String requesterId) {
        requireLeaderOrMemberManage(groupId, requesterId);
        Instant now = Instant.now();
        return invitationRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .peek(invitation -> invitation.expireIfNecessary(now))
                .map(InvitationView::from)
                .toList();
    }

    @Transactional
    public void cancelInvitation(String groupId, String invitationId, String requesterId) {
        requireLeaderOrMemberManage(groupId, requesterId);
        GroupInvitationEntity invitation = invitationRepository.findById(invitationId)
                .filter(item -> item.getGroupId().equals(groupId))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "邀请码不存在"));
        invitation.expireIfNecessary(Instant.now());
        if (!"ACTIVE".equals(invitation.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "该邀请码已不可取消");
        }
        invitation.cancel();
    }

    @Transactional
    public GroupView join(String userId, JoinGroupRequest request) {
        String code = request.code().trim().toUpperCase();
        GroupInvitationEntity invitation = invitationRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "邀请码无效"));
        if (!"ACTIVE".equals(invitation.getStatus()) || invitation.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.CONFLICT, "邀请码已过期或已用完");
        }
        if (membershipRepository.findByGroupIdAndUserIdAndStatus(
                invitation.getGroupId(), userId, "ACTIVE").isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT, "你已是该小组成员");
        }
        invitation.useOnce();
        membershipRepository.findByGroupIdAndUserId(invitation.getGroupId(), userId)
                .ifPresentOrElse(GroupMembershipEntity::reactivate,
                        () -> membershipRepository.save(new GroupMembershipEntity(
                                invitation.getGroupId(), userId, "MEMBER")));
        GroupEntity group = groupRepository.findById(invitation.getGroupId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "小组不存在"));
        return GroupView.from(group, "MEMBER", List.of());
    }

    @Transactional
    public void grantPermission(
            String groupId, String targetUserId, PermissionType permission, String requesterId
    ) {
        requireLeader(groupId, requesterId);
        requireMembership(groupId, targetUserId);
        if (!permissionRepository.existsByGroupIdAndUserIdAndPermissionAndRevokedAtIsNull(
                groupId, targetUserId, permission.name())) {
            permissionRepository.save(new PermissionGrantEntity(
                    groupId, targetUserId, permission.name(), requesterId));
        }
    }

    @Transactional
    public void revokePermission(
            String groupId, String targetUserId, PermissionType permission, String requesterId
    ) {
        requireLeader(groupId, requesterId);
        permissionRepository.findByGroupIdAndUserIdAndPermissionAndRevokedAtIsNull(
                        groupId, targetUserId, permission.name())
                .ifPresent(PermissionGrantEntity::revoke);
    }

    @Transactional
    public void removeMember(String groupId, String targetUserId, String requesterId) {
        requireLeaderOrMemberManage(groupId, requesterId);
        if (targetUserId.equals(requesterId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "不能通过成员管理移除自己，请使用退出小组");
        }
        GroupMembershipEntity target = membershipRepository
                .findByGroupIdAndUserIdAndStatus(groupId, targetUserId, "ACTIVE")
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "成员不存在"));
        if ("LEADER".equals(target.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能移除小组组长");
        }
        target.remove();
        revokeAllPermissions(groupId, targetUserId);
        deactivateTaskAssignments(groupId, targetUserId);
        notificationService.create(targetUserId, "group-removed:" + groupId + ":" + targetUserId,
                "GROUP_MEMBERSHIP", "你已被移出小组", requireGroup(groupId).getName(), "/groups");
    }

    @Transactional
    public void leaveGroup(String groupId, String userId) {
        GroupMembershipEntity membership = membershipRepository
                .findByGroupIdAndUserIdAndStatus(groupId, userId, "ACTIVE")
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "你不在该小组中"));
        if ("LEADER".equals(membership.getRole())) {
            throw new BusinessException(ErrorCode.CONFLICT, "组长退出前必须先转让组长");
        }
        membership.leave();
        revokeAllPermissions(groupId, userId);
        deactivateTaskAssignments(groupId, userId);
    }

    @Transactional
    public GroupView transferLeadership(String groupId, String targetUserId, String requesterId) {
        requireLeader(groupId, requesterId);
        if (targetUserId.equals(requesterId)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "不能将组长转让给自己");
        }
        GroupEntity group = requireGroup(groupId);
        GroupMembershipEntity currentLeader = membershipRepository
                .findByGroupIdAndUserIdAndStatus(groupId, requesterId, "ACTIVE").orElseThrow();
        GroupMembershipEntity newLeader = membershipRepository
                .findByGroupIdAndUserIdAndStatus(groupId, targetUserId, "ACTIVE")
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_ARGUMENT, "新组长必须是当前小组成员"));
        currentLeader.demoteToMember();
        newLeader.promoteToLeader();
        group.transferLeadership(targetUserId);
        revokeAllPermissions(groupId, targetUserId);
        notificationService.create(targetUserId, "group-leader:" + groupId + ":" + targetUserId,
                "GROUP_MEMBERSHIP", "你已成为小组组长", group.getName(), "/groups");
        return GroupView.from(group, "MEMBER", activePermissions(groupId, requesterId));
    }

    private void requireMembership(String groupId, String userId) {
        if (!groupRepository.existsById(groupId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "小组不存在");
        }
        membershipRepository.findByGroupIdAndUserIdAndStatus(groupId, userId, "ACTIVE")
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "你不是该小组成员"));
    }

    private void requireLeader(String groupId, String userId) {
        GroupMembershipEntity membership = membershipRepository
                .findByGroupIdAndUserIdAndStatus(groupId, userId, "ACTIVE")
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "你不是该小组成员"));
        if (!"LEADER".equals(membership.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有组长可以执行该操作");
        }
    }

    private void requireLeaderOrMemberManage(String groupId, String userId) {
        GroupMembershipEntity membership = membershipRepository
                .findByGroupIdAndUserIdAndStatus(groupId, userId, "ACTIVE")
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "你不是该小组成员"));
        if (!"LEADER".equals(membership.getRole())
                && !permissionRepository.existsByGroupIdAndUserIdAndPermissionAndRevokedAtIsNull(
                groupId, userId, PermissionType.MEMBER_MANAGE.name())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "缺少成员管理权限");
        }
    }

    private List<String> activePermissions(String groupId, String userId) {
        return permissionRepository.findAllByGroupIdAndUserIdAndRevokedAtIsNull(groupId, userId).stream()
                .map(PermissionGrantEntity::getPermission)
                .toList();
    }

    private GroupEntity requireGroup(String groupId) {
        return groupRepository.findById(groupId)
                .filter(group -> "ACTIVE".equals(group.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "小组不存在"));
    }

    private void revokeAllPermissions(String groupId, String userId) {
        permissionRepository.findAllByGroupIdAndUserIdAndRevokedAtIsNull(groupId, userId)
                .forEach(PermissionGrantEntity::revoke);
    }

    private void deactivateTaskAssignments(String groupId, String userId) {
        taskAssignmentRepository.findAllByUserIdAndStatus(userId, "ACTIVE").stream()
                .filter(assignment -> taskRepository.findById(assignment.getTaskId())
                        .map(task -> task.getGroupId().equals(groupId)).orElse(false))
                .forEach(assignment -> assignment.deactivate());
    }

    private String newInvitationCode() {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        String code;
        do {
            StringBuilder builder = new StringBuilder(8);
            for (int i = 0; i < 8; i++) builder.append(alphabet.charAt(secureRandom.nextInt(alphabet.length())));
            code = builder.toString();
        } while (invitationRepository.existsByCode(code));
        return code;
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}
