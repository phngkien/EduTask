package com.edutask.controller;

import com.edutask.dto.request.TaskRequest;
import com.edutask.dto.response.ApiResponse;
import com.edutask.dto.response.TaskDetailResponse;
import com.edutask.entity.SubTask;
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

    @GetMapping("/{id}/subtasks")
    public ResponseEntity<ApiResponse<List<SubTask>>> getSubTasks(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getSubTasks(id)));
    }

    @PostMapping("/{id}/subtasks/generate-ai")
    public ResponseEntity<ApiResponse<List<SubTask>>> generateAiChecklist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User actor = (User) userDetails;
        List<SubTask> created = taskService.generateAiChecklist(id, actor);
        return ResponseEntity.ok(ApiResponse.success("Sinh checklist bằng AI thành công", created));
    }

    @PutMapping("/subtasks/{subTaskId}/toggle")
    public ResponseEntity<ApiResponse<SubTask>> toggleSubTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long subTaskId) {
        User actor = (User) userDetails;
        SubTask updated = taskService.toggleSubTask(subTaskId, actor);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", updated));
    }

    @PostMapping("/{id}/subtasks")
    public ResponseEntity<ApiResponse<SubTask>> createSubTaskManually(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> request) {
        User actor = (User) userDetails;
        String content = request.get("content");
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Nội dung không được trống");
        }
        SubTask subTask = taskService.createSubTaskManually(id, content.trim(), actor);
        return ResponseEntity.ok(ApiResponse.success("Thêm mục checklist thành công", subTask));
    }

    @DeleteMapping("/subtasks/{subTaskId}")
    public ResponseEntity<ApiResponse<Void>> deleteSubTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long subTaskId) {
        User actor = (User) userDetails;
        taskService.deleteSubTask(subTaskId, actor);
        return ResponseEntity.ok(ApiResponse.success("Xóa mục checklist thành công", null));
    }

    @PutMapping("/subtasks/{subTaskId}/content")
    public ResponseEntity<ApiResponse<SubTask>> updateSubTaskContent(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long subTaskId,
            @RequestBody java.util.Map<String, String> request) {
        User actor = (User) userDetails;
        String content = request.get("content");
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Nội dung không được trống");
        }
        SubTask subTask = taskService.updateSubTaskContent(subTaskId, content.trim(), actor);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật mục checklist thành công", subTask));
    }
}
