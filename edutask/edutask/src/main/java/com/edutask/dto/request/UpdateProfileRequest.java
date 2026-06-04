package com.edutask.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    private String fullName;
    private String avatarUrl;
    private String skills;
    private String availability;
}
