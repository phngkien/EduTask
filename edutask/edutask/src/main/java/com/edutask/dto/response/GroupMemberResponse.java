package com.edutask.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberResponse {
    private Long groupId;
    private String groupName;
    private Long userId;
    private String fullName;
    private String email;
    private String avatarUrl;
    private String role;
    private Integer contributionScore;
    private LocalDateTime joinedAt;
}
