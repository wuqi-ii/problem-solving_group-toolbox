package cn.edu.teamtoolbox.group;

import cn.edu.teamtoolbox.common.api.ApiResponse;
import cn.edu.teamtoolbox.security.CurrentUserPrincipal;
import cn.edu.teamtoolbox.permission.GrantPermissionRequest;
import cn.edu.teamtoolbox.permission.PermissionType;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ApiResponse<GroupView> create(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody CreateGroupRequest request
    ) {
        return ApiResponse.ok(groupService.create(principal.id(), request));
    }

    @GetMapping
    public ApiResponse<List<GroupView>> listMine(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(groupService.listMine(principal.id()));
    }

    @GetMapping("/{groupId}/members")
    public ApiResponse<List<MemberView>> listMembers(
            @PathVariable String groupId,
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.ok(groupService.listMembers(groupId, principal.id()));
    }

    @PostMapping("/{groupId}/invitations")
    public ApiResponse<InvitationView> createInvitation(
            @PathVariable String groupId,
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody CreateInvitationRequest request
    ) {
        return ApiResponse.ok(groupService.createInvitation(groupId, principal.id(), request));
    }

    @PostMapping("/join")
    public ApiResponse<GroupView> join(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody JoinGroupRequest request
    ) {
        return ApiResponse.ok(groupService.join(principal.id(), request));
    }

    @PostMapping("/{groupId}/members/{userId}/permissions")
    public ApiResponse<Void> grantPermission(
            @PathVariable String groupId,
            @PathVariable String userId,
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody GrantPermissionRequest request
    ) {
        groupService.grantPermission(groupId, userId, request.permission(), principal.id());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{groupId}/members/{userId}/permissions/{permission}")
    public ApiResponse<Void> revokePermission(
            @PathVariable String groupId,
            @PathVariable String userId,
            @PathVariable PermissionType permission,
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        groupService.revokePermission(groupId, userId, permission, principal.id());
        return ApiResponse.ok(null);
    }
}
