package com.edutask.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupResponse {
    private Long groupId;
    private String groupName;
    private UserResponse creator;
    private LocalDateTime deadline;
    private String status;
    private LocalDateTime createdAt;
}
