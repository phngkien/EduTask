package com.edutask.controller;

import com.edutask.dto.request.TaskRequest;
import com.edutask.dto.response.ApiResponse;
import com.edutask.dto.response.TaskDetailResponse;
import com.edutask.entity.User;
import com.edutask.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskDetailResponse>>> getAllTasks() {
        return ResponseEntity.ok(ApiResponse.success(taskService.getAllTasks()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskDetailResponse>> getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id)
                .map(t -> ResponseEntity.ok(ApiResponse.success(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<TaskDetailResponse>>> getMyTasks(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        return ResponseEntity.ok(ApiResponse.success(taskService.getTasksByAssignee(user.getUserId())));
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse<List<TaskDetailResponse>>> getGroupTasks(
            @PathVariable Long groupId) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTasksByGroup(groupId)));
    }

    @GetMapping("/assignee/{userId}")
    public ResponseEntity<ApiResponse<List<TaskDetailResponse>>> getAssigneeTasks(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTasksByAssignee(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TaskDetailResponse>> createTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TaskRequest request) {
        User actor = (User) userDetails;
        TaskDetailResponse created = taskService.createTask(request, actor);
        return ResponseEntity.ok(ApiResponse.success("Tạo nhiệm vụ thành công", created));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TaskDetailResponse>> updateStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam String status) {
        User actor = (User) userDetails;
        TaskDetailResponse updated = taskService.updateTaskStatus(id, status, actor);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User actor = (User) userDetails;
        taskService.softDeleteTask(id, actor);
        return ResponseEntity.ok(ApiResponse.success("Xóa nhiệm vụ thành công", null));
    }
}
