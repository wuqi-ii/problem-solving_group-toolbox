package cn.edu.teamtoolbox.group;

import cn.edu.teamtoolbox.common.error.BusinessException;
import cn.edu.teamtoolbox.common.error.ErrorCode;
import cn.edu.teamtoolbox.permission.PermissionGrantEntity;
import cn.edu.teamtoolbox.permission.PermissionGrantRepository;
import cn.edu.teamtoolbox.permission.PermissionType;
import cn.edu.teamtoolbox.user.UserEntity;
import cn.edu.teamtoolbox.user.UserRepository;
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
    private final SecureRandom secureRandom = new SecureRandom();

    public GroupService(
            GroupRepository groupRepository,
            GroupMembershipRepository membershipRepository,
            UserRepository userRepository,
            GroupInvitationRepository invitationRepository,
            PermissionGrantRepository permissionRepository
    ) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional
    public GroupView create(String userId, CreateGroupRequest request) {
        GroupEntity group = groupRepository.save(new GroupEntity(
                request.name().trim(), normalizeDescription(request.description()), userId));
        GroupMembershipEntity membership = membershipRepository.save(
                new GroupMembershipEntity(group.getId(), userId, "LEADER"));
        return GroupView.from(group, membership.getRole());
    }

    @Transactional(readOnly = true)
    public List<GroupView> listMine(String userId) {
        return membershipRepository.findAllByUserIdAndStatus(userId, "ACTIVE").stream()
                .map(membership -> groupRepository.findById(membership.getGroupId())
                        .filter(group -> "ACTIVE".equals(group.getStatus()))
                        .map(group -> GroupView.from(group, membership.getRole()))
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
                    return new MemberView(user.getId(), user.getAccount(), user.getNickname(),
                            membership.getRole(), membership.getJoinedAt(),
                            permissions.getOrDefault(user.getId(), List.of()));
                })
                .toList();
    }

    @Transactional
    public InvitationView createInvitation(
            String groupId, String requesterId, CreateInvitationRequest request
    ) {
        requireLeader(groupId, requesterId);
        GroupInvitationEntity invitation = new GroupInvitationEntity(
                groupId, newInvitationCode(), requesterId,
                Instant.now().plus(request.validHours(), ChronoUnit.HOURS), request.maxUses());
        return InvitationView.from(invitationRepository.save(invitation));
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
        membershipRepository.save(new GroupMembershipEntity(invitation.getGroupId(), userId, "MEMBER"));
        GroupEntity group = groupRepository.findById(invitation.getGroupId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "小组不存在"));
        return GroupView.from(group, "MEMBER");
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
