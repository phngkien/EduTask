package com.edutask.service;

import com.edutask.entity.ActivityLog;
import com.edutask.entity.User;
import com.edutask.repository.ActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public void logAction(User user, String action, String description) {
        String ipAddress = "0.0.0.0";
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ipAddress = request.getRemoteAddr();
            }
        } catch (Exception e) {
            // Context might not be available in async jobs
        }

        ActivityLog log = ActivityLog.builder()
                .user(user)
                .action(action)
                .description(description)
                .ipAddress(ipAddress)
                .build();

        activityLogRepository.save(log);
    }

    public long countAiRequestsToday(User user) {
        if (user == null) return 0;
        java.time.LocalDateTime startOfDay = java.time.LocalDateTime.now().with(java.time.LocalTime.MIN);
        return activityLogRepository.countActionsToday(user.getUserId(), "AI_REQUEST", startOfDay);
    }
}
