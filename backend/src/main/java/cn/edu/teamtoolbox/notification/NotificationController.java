package cn.edu.teamtoolbox.notification;

import cn.edu.teamtoolbox.common.api.ApiResponse;
import cn.edu.teamtoolbox.security.CurrentUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<NotificationView>> list(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.list(principal.id()));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.unreadCount(principal.id()));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable String id, @AuthenticationPrincipal CurrentUserPrincipal principal) {
        service.markRead(id, principal.id());
        return ApiResponse.ok(null);
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        service.markAllRead(principal.id());
        return ApiResponse.ok(null);
    }
}
