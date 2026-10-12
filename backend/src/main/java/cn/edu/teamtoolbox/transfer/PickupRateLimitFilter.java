package cn.edu.teamtoolbox.transfer;

import cn.edu.teamtoolbox.common.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import cn.edu.teamtoolbox.security.ClientIpResolver;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class PickupRateLimitFilter extends OncePerRequestFilter {
    private record Window(long minute, int count) {}
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final int limit;
    public PickupRateLimitFilter(ObjectMapper objectMapper, int limit) { this.objectMapper = objectMapper; this.limit = limit; }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/v1/pickup/")) { chain.doFilter(request, response); return; }
        long minute = Instant.now().getEpochSecond() / 60;
        String pickupScope = request.getRequestURI().substring("/api/v1/pickup/".length()).split("/", 2)[0];
        String key = ClientIpResolver.resolve(request) + ':' + pickupScope;
        Window current = windows.compute(key, (ignored, old) ->
                old == null || old.minute() != minute ? new Window(minute, 1) : new Window(minute, old.count() + 1));
        if (current.count() > limit) {
            response.setStatus(429); response.setContentType(MediaType.APPLICATION_JSON_VALUE); response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), ApiResponse.error("RATE_LIMITED", "取件请求过于频繁，请稍后再试"));
            return;
        }
        chain.doFilter(request, response);
    }
}
