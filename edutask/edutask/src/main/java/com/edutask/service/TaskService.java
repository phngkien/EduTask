package com.edutask.service;

import com.edutask.dto.request.TaskRequest;
import com.edutask.dto.response.TaskDetailResponse;
import com.edutask.entity.Group;
import com.edutask.entity.Task;
import com.edutask.entity.User;
import com.edutask.repository.GroupRepository;
import com.edutask.entity.SubTask;
import com.edutask.repository.SubTaskRepository;
import com.edutask.repository.TaskRepository;
import com.edutask.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final SubTaskRepository subTaskRepository;
    private final AiService aiService;
    private final SubscriptionService subscriptionService;

    @Transactional(readOnly = true)
    public List<TaskDetailResponse> getAllTasks() {
        return taskRepository.findByDeletedAtIsNull().stream()
                .map(this::toDetailResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<TaskDetailResponse> getTaskById(Long id) {
        return taskRepository.findById(id)
                .filter(t -> t.getDeletedAt() == null)
                .map(this::toDetailResponse);
    }

    @Transactional(readOnly = true)
    public List<TaskDetailResponse> getTasksByGroup(Long groupId) {
        return taskRepository.findByGroupGroupIdAndDeletedAtIsNull(groupId).stream()
                .map(this::toDetailResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDetailResponse> getTasksByAssignee(Long userId) {
        return taskRepository.findByAssigneeUserIdAndDeletedAtIsNull(userId).stream()
                .map(this::toDetailResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskDetailResponse createTask(TaskRequest request, User actor) {
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm"));

        if (request.getDueDate() != null && group.getDeadline() != null) {
            if (request.getDueDate().isAfter(group.getDeadline())) {
                throw new RuntimeException("Hạn chót của công việc không thể muộn hơn hạn chót của nhóm học (" + group.getDeadline().toLocalDate() + ")");
            }
        }

        // Kiểm tra giới hạn gói miễn phí của chủ nhóm (tối đa 5 công việc)
        User groupCreator = group.getCreator();
        if (groupCreator != null && !subscriptionService.hasActivePaidSubscription(groupCreator)) {
            long taskCount = taskRepository.countByGroupGroupIdAndDeletedAtIsNull(group.getGroupId());
            if (taskCount >= 5) {
                throw new RuntimeException("Gói miễn phí giới hạn tối đa 5 công việc trong mỗi nhóm. Vui lòng nâng cấp gói để tạo thêm công việc.");
            }
        }

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

    public List<SubTask> getSubTasks(Long taskId) {
        return subTaskRepository.findByTaskTaskId(taskId);
    }

    @Transactional
    public List<SubTask> generateAiChecklist(Long taskId, User actor) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhiệm vụ"));

        if (task.getDeletedAt() != null) {
            throw new RuntimeException("Nhiệm vụ đã bị xóa");
        }

        // Kiểm tra giới hạn dùng AI (tối đa 3 lượt/ngày) của Free Tier
        if (!subscriptionService.hasActivePaidSubscription(actor)) {
            long dailyAiCount = activityLogService.countAiRequestsToday(actor);
            if (dailyAiCount >= 3) {
                throw new RuntimeException("Gói miễn phí giới hạn tối đa 3 lượt dùng AI mỗi ngày. Vui lòng nâng cấp gói dịch vụ để sử dụng không giới hạn.");
            }
        }

        // Xóa các subtasks cũ nếu có
        subTaskRepository.deleteByTaskTaskId(taskId);

        // Sinh checklist mới từ AI
        List<String> items = aiService.generateChecklist(task.getTaskName(), task.getDescription());
        List<SubTask> subTasks = new ArrayList<>();
        
        for (String item : items) {
            SubTask subTask = SubTask.builder()
                    .task(task)
                    .content(item)
                    .isCompleted(false)
                    .build();
            subTasks.add(subTaskRepository.save(subTask));
        }

        activityLogService.logAction(actor, "AI_REQUEST",
                "Tự động tạo checklist bằng AI cho nhiệm vụ: " + task.getTaskName());

        return subTasks;
    }

    @Transactional
    public SubTask toggleSubTask(Long subTaskId, User actor) {
        SubTask subTask = subTaskRepository.findById(subTaskId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mục checklist"));

        subTask.setIsCompleted(!subTask.getIsCompleted());
        subTask = subTaskRepository.save(subTask);

        activityLogService.logAction(actor, "TOGGLE_SUBTASK",
                "Cập nhật trạng thái mục \"" + subTask.getContent() + "\": " +
                (subTask.getIsCompleted() ? "Đã xong" : "Chưa xong"));

        return subTask;
    }

    @Transactional
    public SubTask createSubTaskManually(Long taskId, String content, User actor) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhiệm vụ"));

        if (task.getDeletedAt() != null) {
            throw new RuntimeException("Nhiệm vụ đã bị xóa");
        }

        SubTask subTask = SubTask.builder()
                .task(task)
                .content(content)
                .isCompleted(false)
                .build();

        subTask = subTaskRepository.save(subTask);
        activityLogService.logAction(actor, "CREATE_SUBTASK",
                "Thêm mục checklist thủ công: \"" + content + "\" cho nhiệm vụ " + task.getTaskName());

        return subTask;
    }

    @Transactional
    public void deleteSubTask(Long subTaskId, User actor) {
        SubTask subTask = subTaskRepository.findById(subTaskId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mục checklist"));

        subTaskRepository.delete(subTask);
        activityLogService.logAction(actor, "DELETE_SUBTASK",
                "Xóa mục checklist: \"" + subTask.getContent() + "\"");
    }

    @Transactional
    public SubTask updateSubTaskContent(Long subTaskId, String content, User actor) {
        SubTask subTask = subTaskRepository.findById(subTaskId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mục checklist"));

        String oldContent = subTask.getContent();
        subTask.setContent(content);
        subTask = subTaskRepository.save(subTask);

        activityLogService.logAction(actor, "UPDATE_SUBTASK",
                "Sửa mục checklist: \"" + oldContent + "\" -> \"" + content + "\"");

        return subTask;
    }
}
