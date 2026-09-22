package cn.edu.teamtoolbox.task;

import cn.edu.teamtoolbox.common.api.ApiResponse;
import cn.edu.teamtoolbox.security.CurrentUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/groups/{groupId}/tasks")
    public ApiResponse<TaskView> create(
            @PathVariable String groupId,
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        return ApiResponse.ok(taskService.create(groupId, principal.id(), request));
    }

    @GetMapping("/groups/{groupId}/tasks")
    public ApiResponse<List<TaskView>> list(
            @PathVariable String groupId,
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.ok(taskService.listByGroup(groupId, principal.id()));
    }

    @PostMapping("/tasks/{taskId}/start")
    public ApiResponse<TaskView> start(
            @PathVariable String taskId,
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.ok(taskService.start(taskId, principal.id()));
    }

    @PostMapping("/tasks/{taskId}/submissions")
    public ApiResponse<TaskView> submit(
            @PathVariable String taskId,
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody SubmitTaskRequest request
    ) {
        return ApiResponse.ok(taskService.submit(taskId, principal.id(), request));
    }

    @PostMapping("/tasks/{taskId}/reviews")
    public ApiResponse<TaskView> review(
            @PathVariable String taskId,
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody ReviewTaskRequest request
    ) {
        return ApiResponse.ok(taskService.review(taskId, principal.id(), request));
    }
}
