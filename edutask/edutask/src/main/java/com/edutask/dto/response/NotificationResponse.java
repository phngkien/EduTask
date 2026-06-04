package com.edutask.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long notificationId;
    private String content;
    private boolean isRead;
    private LocalDateTime createdAt;
}
