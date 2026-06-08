package com.edutask.assignment;

import com.edutask.entity.Task;
import com.edutask.entity.User;
import com.edutask.repository.TaskRepository;
import com.edutask.repository.UserRepository;
import com.edutask.service.AiService;
import com.edutask.service.SubscriptionService;
import com.edutask.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.edutask.entity.GroupMember;
import com.edutask.repository.GroupMemberRepository;
import org.springframework.security.core.context.SecurityContextHolder;

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
    private final AiService aiService;
    private final SubscriptionService subscriptionService;
    private final ActivityLogService activityLogService;
    
   @Transactional
    public List<AssignmentSuggestionResponse> suggestAssignees(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy task"));

        // Lấy thông tin user hiện tại đang đăng nhập
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User actor = null;
        if (principal instanceof User) {
            actor = (User) principal;
        }

        // Kiểm tra giới hạn dùng AI (tối đa 3 lượt/ngày) của Free Tier
        if (actor != null && !subscriptionService.hasActivePaidSubscription(actor)) {
            long dailyAiCount = activityLogService.countAiRequestsToday(actor);
            if (dailyAiCount >= 3) {
                throw new RuntimeException("Gói miễn phí giới hạn tối đa 3 lượt dùng AI mỗi ngày. Vui lòng nâng cấp gói dịch vụ để sử dụng không giới hạn.");
            }
        }

        Long groupId = task.getGroup().getGroupId();
        List<GroupMember> groupMembers = groupMemberRepository.findByIdGroupId(groupId);
        List<User> users = groupMembers.stream()
                .map(GroupMember::getUser)
                .toList();

        List<Map<String, Object>> candidatesList = new ArrayList<>();
        Map<Long, User> userMap = new HashMap<>();
        Map<Long, Integer> activeCounts = new HashMap<>();
        Map<Long, Integer> maxLimits = new HashMap<>();

        for (User user : users) {
            if (user.getDeletedAt() != null) {
                continue;
            }
            if (user.getAvailability() != null && 
                (user.getAvailability().equalsIgnoreCase("false") || 
                 user.getAvailability().equalsIgnoreCase("bận") ||
                 user.getAvailability().equalsIgnoreCase("busy") ||
                 user.getAvailability().equalsIgnoreCase("bận hoàn toàn"))) {
                continue;
            }

            int activeTaskCount = countActiveTasks(user.getUserId());
            int maxActiveTasks = user.getMaxActiveTasks() != null ? user.getMaxActiveTasks() : 5;

            if (activeTaskCount >= maxActiveTasks) {
                continue;
            }

            userMap.put(user.getUserId(), user);
            activeCounts.put(user.getUserId(), activeTaskCount);
            maxLimits.put(user.getUserId(), maxActiveTasks);

            Map<String, Object> candidate = new HashMap<>();
            candidate.put("userId", user.getUserId());
            candidate.put("fullName", user.getFullName());
            candidate.put("skills", user.getSkills() != null ? user.getSkills() : "");
            candidate.put("activeTaskCount", activeTaskCount);
            candidate.put("maxActiveTasks", maxActiveTasks);
            candidatesList.add(candidate);
        }

        List<AssignmentSuggestionResponse> result = new ArrayList<>();
        boolean parsedWithAi = false;

        if (!candidatesList.isEmpty()) {
            try {
                List<Map<String, Object>> aiSuggestions = aiService.getAiAssignmentSuggestions(
                        task.getTaskName(),
                        task.getDescription(),
                        task.getPriority(),
                        task.getCategory(),
                        candidatesList
                );

                if (aiSuggestions != null && !aiSuggestions.isEmpty()) {
                    for (Map<String, Object> item : aiSuggestions) {
                        if (item.get("userId") == null) {
                            continue;
                        }
                        Long userId = ((Number) item.get("userId")).longValue();
                        User user = userMap.get(userId);
                        if (user != null) {
                            double totalScore = ((Number) item.getOrDefault("totalScore", 0.0)).doubleValue();
                            double skillScore = ((Number) item.getOrDefault("skillScore", 0.0)).doubleValue();
                            double workloadScore = ((Number) item.getOrDefault("workloadScore", 0.0)).doubleValue();
                            double priorityScore = ((Number) item.getOrDefault("priorityScore", 0.0)).doubleValue();
                            double availabilityScore = ((Number) item.getOrDefault("availabilityScore", 0.0)).doubleValue();
                            String reason = (String) item.getOrDefault("reason", "");

                            AssignmentSuggestionResponse response = AssignmentSuggestionResponse.builder()
                                    .userId(userId)
                                    .fullName(user.getFullName())
                                    .skills(user.getSkills())
                                    .activeTaskCount(activeCounts.get(userId))
                                    .maxActiveTasks(maxLimits.get(userId))
                                    .totalScore(round(totalScore))
                                    .skillScore(round(skillScore))
                                    .workloadScore(round(workloadScore))
                                    .priorityScore(round(priorityScore))
                                    .availabilityScore(round(availabilityScore))
                                    .reason(reason)
                                    .build();
                            result.add(response);
                        }
                    }
                    parsedWithAi = !result.isEmpty();
                    if (parsedWithAi && actor != null) {
                        activityLogService.logAction(actor, "AI_REQUEST", "Đề xuất phân công công việc bằng AI cho task: " + task.getTaskName());
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi gợi ý phân công bằng AI: " + e.getMessage());
            }
        }

        // Fallback: nếu AI trống hoặc gặp lỗi, sử dụng giải thuật heuristic cũ
        if (!parsedWithAi) {
            result.clear();
            for (User user : users) {
                if (user.getDeletedAt() != null) {
                    continue;
                }
                if (user.getAvailability() != null && 
                    (user.getAvailability().equalsIgnoreCase("false") || 
                     user.getAvailability().equalsIgnoreCase("bận") ||
                     user.getAvailability().equalsIgnoreCase("busy") ||
                     user.getAvailability().equalsIgnoreCase("bận hoàn toàn"))) {
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
    task.setStatus("TODO");
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
       return (int) taskRepository.countActiveTasksByUserId(userId);
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