package cn.edu.teamtoolbox.group;

import cn.edu.teamtoolbox.common.error.BusinessException;
import cn.edu.teamtoolbox.common.error.ErrorCode;
import cn.edu.teamtoolbox.permission.PermissionGrantRepository;
import cn.edu.teamtoolbox.permission.PermissionType;
import org.springframework.stereotype.Service;

@Service
public class GroupAuthorizationService {
    private final GroupMembershipRepository membershipRepository;
    private final PermissionGrantRepository permissionRepository;

    public GroupAuthorizationService(
            GroupMembershipRepository membershipRepository,
            PermissionGrantRepository permissionRepository
    ) {
        this.membershipRepository = membershipRepository;
        this.permissionRepository = permissionRepository;
    }

    public GroupMembershipEntity requireMember(String groupId, String userId) {
        return membershipRepository.findByGroupIdAndUserIdAndStatus(groupId, userId, "ACTIVE")
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "你不是该小组成员"));
    }

    public boolean isLeaderOrHas(String groupId, String userId, PermissionType permission) {
        GroupMembershipEntity membership = requireMember(groupId, userId);
        return "LEADER".equals(membership.getRole())
                || permissionRepository.existsByGroupIdAndUserIdAndPermissionAndRevokedAtIsNull(
                groupId, userId, permission.name());
    }

    public void requireLeaderOrPermission(String groupId, String userId, PermissionType permission) {
        if (!isLeaderOrHas(groupId, userId, permission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "缺少执行该操作的权限");
        }
    }
}
