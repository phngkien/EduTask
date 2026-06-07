package com.edutask.service;

import com.edutask.dto.request.TaskRequest;
import com.edutask.dto.response.TaskDetailResponse;
import com.edutask.entity.Group;
import com.edutask.entity.Task;
import com.edutask.entity.User;
import com.edutask.repository.GroupRepository;
import com.edutask.repository.TaskRepository;
import com.edutask.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final GroupService groupService;

    public List<TaskDetailResponse> getAllTasks() {
        return taskRepository.findByDeletedAtIsNull().stream()
                .map(this::toDetailResponse)
                .collect(Collectors.toList());
    }

    public Optional<TaskDetailResponse> getTaskById(Long id) {
        return taskRepository.findById(id)
                .filter(t -> t.getDeletedAt() == null)
                .map(this::toDetailResponse);
    }

    public List<TaskDetailResponse> getTasksByGroup(Long groupId) {
        return taskRepository.findByGroupGroupIdAndDeletedAtIsNull(groupId).stream()
                .map(this::toDetailResponse)
                .collect(Collectors.toList());
    }

    public List<TaskDetailResponse> getTasksByAssignee(Long userId) {
        return taskRepository.findByAssigneeUserIdAndDeletedAtIsNull(userId).stream()
                .map(this::toDetailResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskDetailResponse createTask(TaskRequest request, User actor) {
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm"));

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người được phân công"));
        }

        Task task = Task.builder()
                .group(group)
                .taskName(request.getTaskName())
                .assignee(assignee)
                .dueDate(request.getDueDate())
                .status(request.getStatus() != null ? request.getStatus().toUpperCase() : "TODO")
                .build();

        task = taskRepository.save(task);
        activityLogService.logAction(actor, "CREATE_TASK",
                "Tạo nhiệm vụ: " + task.getTaskName() + " trong nhóm " + group.getGroupName());

        if (assignee != null) {
            notificationService.createNotification(assignee,
                    "Bạn được giao nhiệm vụ mới: \"" + task.getTaskName()
                    + "\" trong nhóm: " + group.getGroupName());
        }

        return toDetailResponse(task);
    }

    @Transactional
    public TaskDetailResponse updateTaskStatus(Long taskId, String newStatus, User actor) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhiệm vụ"));

        if (task.getDeletedAt() != null) {
            throw new RuntimeException("Nhiệm vụ đã bị xóa");
        }

        String oldStatus = task.getStatus();
        String normalizedStatus = newStatus.toUpperCase();
        task.setStatus(normalizedStatus);
        task = taskRepository.save(task);

        activityLogService.logAction(actor, "UPDATE_TASK_STATUS",
                "Cập nhật trạng thái \"" + task.getTaskName() + "\": " + oldStatus + " → " + normalizedStatus);

        if (task.getAssignee() != null) {
            notificationService.createNotification(task.getAssignee(),
                    "Nhiệm vụ \"" + task.getTaskName() + "\" đã được cập nhật trạng thái: " + normalizedStatus);
        }

        // Tặng điểm đóng góp khi hoàn thành nhiệm vụ
        if ("DONE".equals(normalizedStatus) && !"DONE".equals(oldStatus) && task.getAssignee() != null) {
            groupService.updateContributionScore(
                    task.getGroup().getGroupId(), task.getAssignee().getUserId(), 10);
            notificationService.createNotification(task.getAssignee(),
                    "🎉 Chúc mừng! Bạn nhận được +10 điểm đóng góp khi hoàn thành \""
                    + task.getTaskName() + "\"");
        }

        return toDetailResponse(task);
    }

    @Transactional
    public void softDeleteTask(Long taskId, User actor) {
        taskRepository.findById(taskId).ifPresent(task -> {
            task.setDeletedAt(LocalDateTime.now());
            task.setStatus("DELETED");
            taskRepository.save(task);
            activityLogService.logAction(actor, "DELETE_TASK", "Xóa nhiệm vụ: " + task.getTaskName());
        });
    }

    public TaskDetailResponse toDetailResponse(Task task) {
        return TaskDetailResponse.builder()
                .taskId(task.getTaskId())
                .taskName(task.getTaskName())
                .status(task.getStatus() != null ? task.getStatus().toLowerCase() : "todo")
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .groupId(task.getGroup() != null ? task.getGroup().getGroupId() : null)
                .groupName(task.getGroup() != null ? task.getGroup().getGroupName() : null)
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getUserId() : null)
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getFullName() : null)
                .assigneeAvatarUrl(task.getAssignee() != null ? task.getAssignee().getAvatarUrl() : null)
                .build();
    }
}
