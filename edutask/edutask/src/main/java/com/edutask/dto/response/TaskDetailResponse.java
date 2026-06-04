package com.edutask.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDetailResponse {
    private Long taskId;
    private String taskName;
    private String status;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
    // Group info
    private Long groupId;
    private String groupName;
    // Assignee info
    private Long assigneeId;
    private String assigneeName;
    private String assigneeAvatarUrl;
}
