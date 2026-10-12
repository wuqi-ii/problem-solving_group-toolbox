package cn.edu.teamtoolbox.audit;

import cn.edu.teamtoolbox.security.CurrentUserPrincipal;
import cn.edu.teamtoolbox.security.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Set;

public class AuditMutationFilter extends OncePerRequestFilter {
    private static final Set<String> MUTATIONS = Set.of("POST", "PATCH", "PUT", "DELETE");
    private final AuditService auditService;
    public AuditMutationFilter(AuditService auditService) { this.auditService = auditService; }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CurrentUserPrincipal principal = authentication != null && authentication.getPrincipal() instanceof CurrentUserPrincipal value
                ? value : null;
        chain.doFilter(request, response);
        if (principal != null && MUTATIONS.contains(request.getMethod()) && request.getRequestURI().startsWith("/api/v1/")) {
            try { auditService.record(principal.id(), request.getMethod(), request.getRequestURI(), response.getStatus(), ClientIpResolver.resolve(request)); }
            catch (RuntimeException ignored) { /* 审计失败不覆盖原业务响应 */ }
        }
    }
}
