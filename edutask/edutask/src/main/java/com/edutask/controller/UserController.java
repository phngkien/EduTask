package com.edutask.controller;

import com.edutask.dto.request.UpdateProfileRequest;
import com.edutask.dto.response.ApiResponse;
import com.edutask.dto.response.UserProfileResponse;
import com.edutask.entity.User;
import com.edutask.entity.ActivityLog;
import com.edutask.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        return ResponseEntity.ok(ApiResponse.success(userService.getProfile(user)));
    }

    @GetMapping("/activities")
    public ResponseEntity<ApiResponse<List<ActivityLog>>> getMyActivities(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        return ResponseEntity.ok(ApiResponse.success(userService.getActivities(user.getUserId())));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateProfileRequest request) {
        User user = (User) userDetails;
        UserProfileResponse updated = userService.updateProfile(user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ thành công", updated));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> softDeleteUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        userService.softDeleteUser(user.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Tài khoản đã được vô hiệu hóa", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(u -> ResponseEntity.ok(ApiResponse.success(userService.toProfileResponse(u))))
                .orElse(ResponseEntity.notFound().build());
    }
}
