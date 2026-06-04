package com.edutask.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String avatarUrl;
    private String skills;
    private String availability;
}
