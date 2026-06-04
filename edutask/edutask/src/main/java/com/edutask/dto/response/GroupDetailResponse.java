package com.edutask.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDetailResponse {
    private Long groupId;
    private String groupName;
    private String status;
    private LocalDateTime deadline;
    private LocalDateTime createdAt;
    // Creator info
    private Long creatorId;
    private String creatorName;
    // Stats (computed)
    private int membersCount;
    private int totalTasks;
    private int completedTasks;
    private int progress; // percentage 0-100
}
