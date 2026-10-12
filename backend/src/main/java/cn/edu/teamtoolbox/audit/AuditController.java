package cn.edu.teamtoolbox.audit;

import cn.edu.teamtoolbox.common.api.ApiResponse;
import cn.edu.teamtoolbox.security.CurrentUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditController {
    private final AuditService service;
    public AuditController(AuditService service) { this.service = service; }
    @GetMapping
    public ApiResponse<List<AuditLogView>> list(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.listMine(principal.id()));
    }
}
