package com.edutask.service;

import com.edutask.dto.request.TaskRequest;
import com.edutask.dto.response.TaskDetailResponse;
import com.edutask.entity.Group;
import com.edutask.entity.Task;
import com.edutask.entity.User;
import com.edutask.repository.GroupMemberRepository;
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
    private final GroupMemberRepository groupMemberRepository;
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

        // BOLA Check: Kiểm tra xem người dùng hiện tại có thuộc nhóm này không
        boolean isMember = groupMemberRepository.existsByIdGroupIdAndIdUserId(group.getGroupId(), actor.getUserId());
        if (!isMember) {
            throw new RuntimeException("Bạn không có quyền tạo nhiệm vụ trong nhóm này");
        }

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người được phân công"));

            // Đảm bảo người được phân công cũng thuộc nhóm này
            boolean isAssigneeMember = groupMemberRepository.existsByIdGroupIdAndIdUserId(group.getGroupId(), assignee.getUserId());
            if (!isAssigneeMember) {
                throw new RuntimeException("Người được phân công không thuộc nhóm này");
            }
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

        // BOLA Check: Chỉ thành viên của nhóm mới được phép cập nhật trạng thái nhiệm vụ
        boolean isMember = groupMemberRepository.existsByIdGroupIdAndIdUserId(task.getGroup().getGroupId(), actor.getUserId());
        if (!isMember) {
            throw new RuntimeException("Bạn không có quyền cập nhật nhiệm vụ của nhóm này");
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
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhiệm vụ"));

        if (task.getDeletedAt() != null) {
            throw new RuntimeException("Nhiệm vụ đã bị xóa");
        }

        // BOLA Check: Chỉ người tạo nhóm, quản trị viên nhóm (ADMIN) hoặc người được giao nhiệm vụ mới được xóa nhiệm vụ
        boolean isCreator = task.getGroup().getCreator() != null && task.getGroup().getCreator().getUserId().equals(actor.getUserId());
        boolean isAdmin = groupMemberRepository.findByIdGroupIdAndIdUserId(task.getGroup().getGroupId(), actor.getUserId())
                .map(gm -> "ADMIN".equalsIgnoreCase(gm.getRole()))
                .orElse(false);
        boolean isAssignee = task.getAssignee() != null && task.getAssignee().getUserId().equals(actor.getUserId());

        if (!isCreator && !isAdmin && !isAssignee) {
            throw new RuntimeException("Bạn không có quyền xóa nhiệm vụ này");
        }

        task.setDeletedAt(LocalDateTime.now());
        task.setStatus("DELETED");
        taskRepository.save(task);
        activityLogService.logAction(actor, "DELETE_TASK", "Xóa nhiệm vụ: " + task.getTaskName());
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
