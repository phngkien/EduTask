package com.edutask.assignment;

import com.edutask.entity.Task;
import com.edutask.entity.User;
import com.edutask.repository.TaskRepository;
import com.edutask.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.edutask.entity.GroupMember;
import com.edutask.repository.GroupMemberRepository;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignmentService {

  private final UserRepository userRepository;
private final TaskRepository taskRepository;
private final GroupMemberRepository groupMemberRepository;
private final AssignmentSuggestionRepository suggestionRepository;
private final AssignmentLogRepository logRepository;
    
   @Transactional
public List<AssignmentSuggestionResponse> suggestAssignees(Long taskId) {
    Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy task"));

    Long groupId = task.getGroup().getGroupId();

    List<GroupMember> groupMembers = groupMemberRepository.findByIdGroupId(groupId);

    List<User> users = groupMembers.stream()
            .map(GroupMember::getUser)
            .toList();

    List<AssignmentSuggestionResponse> result = new ArrayList<>();

    for (User user : users) {
        if (user.getDeletedAt() != null) {
            continue;
        }

        if (user.getAvailability() != null && !user.getAvailability()) {
            continue;
        }

        int activeTaskCount = countActiveTasks(user.getUserId());
        int maxActiveTasks = user.getMaxActiveTasks() != null ? user.getMaxActiveTasks() : 5;

        if (activeTaskCount >= maxActiveTasks) {
            continue;
        }

        double skillScore = calculateSkillScore(user, task);
        double workloadScore = calculateWorkloadScore(activeTaskCount, maxActiveTasks);
        double priorityScore = calculatePriorityScore(task, activeTaskCount);
        double availabilityScore = 10.0;

        double totalScore = skillScore + workloadScore + priorityScore + availabilityScore;

        String reason = buildReason(
                user,
                task,
                activeTaskCount,
                maxActiveTasks,
                skillScore,
                workloadScore,
                priorityScore,
                totalScore
        );

        AssignmentSuggestionResponse response = AssignmentSuggestionResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .skills(user.getSkills())
                .activeTaskCount(activeTaskCount)
                .maxActiveTasks(maxActiveTasks)
                .totalScore(round(totalScore))
                .skillScore(round(skillScore))
                .workloadScore(round(workloadScore))
                .priorityScore(round(priorityScore))
                .availabilityScore(round(availabilityScore))
                .reason(reason)
                .build();

        result.add(response);
    }

    result = result.stream()
            .sorted(Comparator.comparing(AssignmentSuggestionResponse::getTotalScore).reversed())
            .limit(3)
            .collect(Collectors.toList());

    suggestionRepository.deleteByTaskId(taskId);

    int rank = 1;
    for (AssignmentSuggestionResponse item : result) {
        suggestionRepository.save(AssignmentSuggestion.builder()
                .taskId(taskId)
                .userId(item.getUserId())
                .score(item.getTotalScore())
                .skillScore(item.getSkillScore())
                .workloadScore(item.getWorkloadScore())
                .priorityScore(item.getPriorityScore())
                .availabilityScore(item.getAvailabilityScore())
                .reason(item.getReason())
                .rankNo(rank++)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build());
    }

    if (!result.isEmpty()) {
        AssignmentSuggestionResponse best = result.get(0);

        task.setSuggestedAssigneeId(best.getUserId());
        task.setAssignmentScore(best.getTotalScore());
        task.setAssignmentReason(best.getReason());
        task.setAssignmentMode("SUGGESTED");
        taskRepository.save(task);

        logRepository.save(AssignmentLog.builder()
                .taskId(taskId)
                .selectedUserId(best.getUserId())
                .action("SUGGESTED")
                .reason(best.getReason())
                .createdAt(LocalDateTime.now())
                .build());
    }

    return result;
}

    
   @Transactional
public AssignmentTaskResponse approveAssignment(Long taskId, Long assigneeId) {
    Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy task"));

    User assignee = userRepository.findById(assigneeId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy người được giao"));

    task.setAssignee(assignee);
    task.setAssignmentMode("AUTO_APPROVED");
    task.setStatus("TO_DO");
    task.setUpdatedAt(LocalDateTime.now());

    Task savedTask = taskRepository.save(task);

    logRepository.save(AssignmentLog.builder()
            .taskId(taskId)
            .selectedUserId(assigneeId)
            .action("APPROVED")
            .reason("Leader đã chấp nhận đề xuất phân công cho user_id = " + assigneeId)
            .createdAt(LocalDateTime.now())
            .build());

    return toAssignmentTaskResponse(savedTask);
}
    @Transactional
   public AssignmentTaskResponse autoAssign(Long taskId) {
    List<AssignmentSuggestionResponse> suggestions = suggestAssignees(taskId);

    if (suggestions.isEmpty()) {
        throw new RuntimeException("Không có thành viên phù hợp để phân công");
    }

    AssignmentSuggestionResponse best = suggestions.get(0);
    return approveAssignment(taskId, best.getUserId());
}

    public List<AssignmentSuggestion> getSuggestions(Long taskId) {
        return suggestionRepository.findByTaskIdOrderByRankNoAsc(taskId);
    }

    public List<AssignmentLog> getAssignmentLogs(Long taskId) {
        return logRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
    }

   private int countActiveTasks(Long userId) {
    List<Task> tasks = taskRepository.findAll();

    return (int) tasks.stream()
            .filter(task -> task.getDeletedAt() == null)
            .filter(task -> task.getAssignee() != null)
            .filter(task -> task.getAssignee().getUserId().equals(userId))
            .filter(task -> {
                String status = task.getStatus();
                return status == null
                        || status.equalsIgnoreCase("TO_DO")
                        || status.equalsIgnoreCase("IN_PROGRESS");
            })
            .count();
}

   private double calculateSkillScore(User user, Task task) {
    String userSkills = user.getSkills() == null ? "" : user.getSkills().toLowerCase();

    String taskName = safe(task.getTaskName()).toLowerCase();
    String description = safe(task.getDescription()).toLowerCase();
    String category = safe(task.getCategory()).toLowerCase();

    String taskText = taskName + " " + description + " " + category;

    double score = 0;

    // DATABASE phải ưu tiên mạnh nếu task liên quan database/sql
    if (containsAny(taskText, "database", "sql", "data", "cơ sở dữ liệu")) {
        if (containsAny(userSkills, "database", "sql", "data analysis", "python")) {
            score += 35;
        }
        if (containsAny(userSkills, "backend", "java", "spring boot")) {
            score += 10;
        }
    }

    // BACKEND
    if (containsAny(taskText, "backend", "api", "server", "spring", "java")) {
        if (containsAny(userSkills, "backend", "java", "spring boot", "api")) {
            score += 35;
        }
        if (containsAny(userSkills, "database", "sql")) {
            score += 8;
        }
    }

    // FRONTEND/UI
    if (containsAny(taskText, "frontend", "react", "ui", "html", "css", "javascript", "giao diện")) {
        if (containsAny(userSkills, "frontend", "react", "reactjs", "ui", "html", "css", "javascript")) {
            score += 35;
        }
        if (containsAny(userSkills, "design")) {
            score += 8;
        }
    }

    // DESIGN chỉ tính khi task thật sự là UI/visual/design, không tính nhầm Database Design
    if (containsAny(taskText, "logo", "poster", "banner", "figma", "ux", "visual", "interface", "giao diện", "thiết kế ảnh")) {
        if (containsAny(userSkills, "design", "figma", "ui", "ux", "presentation")) {
            score += 35;
        }
    }

    // TESTING
    if (containsAny(taskText, "testing", "test", "kiểm thử", "bug", "qa")) {
        if (containsAny(userSkills, "testing", "test", "qa", "bug")) {
            score += 35;
        }
    }

    // DOCUMENTATION / RESEARCH / REPORT
    if (containsAny(taskText, "documentation", "document", "report", "research", "content", "báo cáo", "tài liệu")) {
        if (containsAny(userSkills, "documentation", "research", "content", "presentation", "marketing")) {
            score += 35;
        }
    }

    return Math.min(score, 40);
}
private boolean containsAny(String text, String... keywords) {
    if (text == null) {
        return false;
    }

    for (String keyword : keywords) {
        if (text.contains(keyword.toLowerCase())) {
            return true;
        }
    }

    return false;
}

    private double calculateWorkloadScore(int activeTaskCount, int maxActiveTasks) {
        if (maxActiveTasks <= 0) {
            maxActiveTasks = 5;
        }

        double score = 30 - ((double) activeTaskCount / maxActiveTasks * 30);
        return Math.max(score, 0);
    }

    private double calculatePriorityScore(Task task, int activeTaskCount) {
        String priority = task.getPriority() == null ? "MEDIUM" : task.getPriority().toUpperCase();

        double score;

        switch (priority) {
            case "URGENT":
                score = activeTaskCount <= 1 ? 20 : 10;
                break;
            case "HIGH":
                score = activeTaskCount <= 2 ? 18 : 9;
                break;
            case "MEDIUM":
                score = 12;
                break;
            case "LOW":
                score = 8;
                break;
            default:
                score = 10;
        }

        if (task.getDueDate() != null) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), task.getDueDate());

            if (daysLeft <= 1 && activeTaskCount <= 1) {
                score += 5;
            } else if (daysLeft <= 1 && activeTaskCount >= 3) {
                score -= 5;
            }
        }

        return Math.max(score, 0);
    }

    private String buildReason(
            User user,
            Task task,
            int activeTaskCount,
            int maxActiveTasks,
            double skillScore,
            double workloadScore,
            double priorityScore,
            double totalScore
    ) {
        return user.getFullName() + " được đề xuất vì có kỹ năng phù hợp với công việc \""
                + task.getTaskName()
                + "\", hiện đang có "
                + activeTaskCount
                + "/"
                + maxActiveTasks
                + " task đang xử lý. "
                + "Điểm kỹ năng: "
                + round(skillScore)
                + ", điểm tải việc: "
                + round(workloadScore)
                + ", điểm ưu tiên/deadline: "
                + round(priorityScore)
                + ". Tổng điểm phù hợp: "
                + round(totalScore)
                + "/100.";
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    private double round(double number) {
        return Math.round(number * 10.0) / 10.0;
    }
    private AssignmentTaskResponse toAssignmentTaskResponse(Task task) {
    Long assigneeId = null;
    String assigneeName = null;

    if (task.getAssignee() != null) {
        assigneeId = task.getAssignee().getUserId();
        assigneeName = task.getAssignee().getFullName();
    }

    return AssignmentTaskResponse.builder()
            .taskId(task.getTaskId())
            .taskName(task.getTaskName())
            .assigneeId(assigneeId)
            .assigneeName(assigneeName)
            .suggestedAssigneeId(task.getSuggestedAssigneeId())
            .assignmentMode(task.getAssignmentMode())
            .assignmentScore(task.getAssignmentScore())
            .assignmentReason(task.getAssignmentReason())
            .status(task.getStatus())
            .build();
}
}