package cn.edu.teamtoolbox.system;

import cn.edu.teamtoolbox.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    @GetMapping("/health")
    public ApiResponse<HealthView> health() {
        return ApiResponse.ok(new HealthView("UP", "team-toolbox-backend", Instant.now()));
    }

    public record HealthView(String status, String service, Instant time) {
    }
}
