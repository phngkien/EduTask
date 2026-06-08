package com.edutask.controller;

import com.edutask.dto.request.LoginRequest;
import com.edutask.dto.request.RefreshTokenRequest;
import com.edutask.dto.request.RegisterRequest;
import com.edutask.dto.response.ApiResponse;
import com.edutask.dto.response.AuthResponse;
import com.edutask.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse data = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng ký thành công", data));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse data = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", data));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse data = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserDetails userDetails) {
        authService.logout(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestBody java.util.Map<String, String> request,
            jakarta.servlet.http.HttpServletRequest servletRequest) {
        String email = request.get("email");
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email không được để trống");
        }
        String origin = servletRequest.getHeader("Origin");
        if (origin == null || origin.trim().isEmpty()) {
            origin = "http://localhost:5500"; // Fallback local
        }
        authService.forgotPassword(email.trim(), origin);
        return ResponseEntity.ok(ApiResponse.success("Đã gửi liên kết khôi phục mật khẩu. Vui lòng kiểm tra email của bạn.", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody java.util.Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException("Mã token không hợp lệ");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new RuntimeException("Mật khẩu mới không được để trống");
        }
        if (newPassword.trim().length() < 6) {
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }
        authService.resetPassword(token.trim(), newPassword.trim());
        return ResponseEntity.ok(ApiResponse.success("Đặt lại mật khẩu thành công!", null));
    }
}

