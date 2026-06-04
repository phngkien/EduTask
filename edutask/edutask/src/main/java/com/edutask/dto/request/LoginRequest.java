package com.edutask.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "Email không được để trống")
    private String username; // email

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
