package cn.edu.teamtoolbox.agent;

import cn.edu.teamtoolbox.common.api.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agent")
public class AgentCapabilityController {
    private final boolean enabled;
    private final String provider;

    public AgentCapabilityController(
            @Value("${app.ai.enabled:false}") boolean enabled,
            @Value("${app.ai.provider:deepseek}") String provider
    ) {
        this.enabled = enabled;
        this.provider = provider;
    }

    @GetMapping("/capabilities")
    public ApiResponse<AgentCapabilitiesView> capabilities() {
        return ApiResponse.ok(new AgentCapabilitiesView(enabled, provider, "RESERVED", List.of(
                new AgentToolDescriptor("list_group_tasks", "查询有权访问的小组任务", "L1", false),
                new AgentToolDescriptor("create_task_draft", "生成可修改的任务草稿", "L2", false),
                new AgentToolDescriptor("publish_task_drafts", "确认后批量发布任务", "L3", true),
                new AgentToolDescriptor("create_file_transfer", "确认后创建临时文件中转", "L3", true),
                new AgentToolDescriptor("summarize_selected_messages", "总结明确选择的讨论范围", "L3", true),
                new AgentToolDescriptor("query_group_knowledge", "查询已授权的小组资料", "L3", true)
        )));
    }
}
