package cn.edu.teamtoolbox.collaboration;

import cn.edu.teamtoolbox.common.api.ApiResponse;
import cn.edu.teamtoolbox.security.CurrentUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CollaborationController {
    private final CollaborationService service;

    public CollaborationController(CollaborationService service) { this.service = service; }

    @GetMapping("/groups/{groupId}/discussions")
    public ApiResponse<List<CollaborationPostView>> discussions(
            @PathVariable String groupId, @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.ok(service.listGroupPosts(groupId, CollaborationPostType.DISCUSSION, principal.id()));
    }

    @PostMapping("/groups/{groupId}/discussions")
    public ApiResponse<CollaborationPostView> createDiscussion(
            @PathVariable String groupId, @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody CreatePostRequest request
    ) {
        return ApiResponse.ok(service.createGroupPost(
                groupId, CollaborationPostType.DISCUSSION, principal.id(), request));
    }

    @GetMapping("/groups/{groupId}/announcements")
    public ApiResponse<List<CollaborationPostView>> announcements(
            @PathVariable String groupId, @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.ok(service.listGroupPosts(groupId, CollaborationPostType.ANNOUNCEMENT, principal.id()));
    }

    @PostMapping("/groups/{groupId}/announcements")
    public ApiResponse<CollaborationPostView> createAnnouncement(
            @PathVariable String groupId, @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody CreatePostRequest request
    ) {
        return ApiResponse.ok(service.createGroupPost(
                groupId, CollaborationPostType.ANNOUNCEMENT, principal.id(), request));
    }

    @GetMapping("/tasks/{taskId}/comments")
    public ApiResponse<List<CollaborationPostView>> comments(
            @PathVariable String taskId, @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.ok(service.listTaskComments(taskId, principal.id()));
    }

    @PostMapping("/tasks/{taskId}/comments")
    public ApiResponse<CollaborationPostView> createComment(
            @PathVariable String taskId, @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody CreatePostRequest request
    ) {
        return ApiResponse.ok(service.createTaskComment(taskId, principal.id(), request));
    }

    @PatchMapping("/collaboration-posts/{postId}/pin")
    public ApiResponse<CollaborationPostView> pin(
            @PathVariable String postId, @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody PinPostRequest request
    ) {
        return ApiResponse.ok(service.setPinned(postId, request.pinned(), principal.id()));
    }

    @DeleteMapping("/collaboration-posts/{postId}")
    public ApiResponse<Void> delete(
            @PathVariable String postId, @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        service.delete(postId, principal.id());
        return ApiResponse.ok(null);
    }
}
