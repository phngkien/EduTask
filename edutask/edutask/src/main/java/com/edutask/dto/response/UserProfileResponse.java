package com.edutask.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String avatarUrl;
    private String skills;
    private String availability;
    private String role;
}
