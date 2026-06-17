package com.edutask.controller;

import com.edutask.dto.request.AdminUserRequest;
import com.edutask.dto.response.ApiResponse;
import com.edutask.dto.response.AdminStatsResponse;
import com.edutask.dto.response.UserProfileResponse;
import com.edutask.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAdminStats()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserProfileResponse>>> getUsers(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(userService.getAdminUsers(query, page, size)));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserProfileResponse>> createUser(
            @RequestBody AdminUserRequest request) {
        UserProfileResponse created = userService.createUserByAdmin(request);
        return ResponseEntity.ok(ApiResponse.success("Thêm người dùng thành công", created));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUser(
            @PathVariable Long id,
            @RequestBody AdminUserRequest request) {
        UserProfileResponse updated = userService.updateUserByAdmin(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật người dùng thành công", updated));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.softDeleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("Đã vô hiệu hóa tài khoản thành công", null));
    }

    @PutMapping("/users/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreUser(@PathVariable Long id) {
        userService.restoreUser(id);
        return ResponseEntity.ok(ApiResponse.success("Đã kích hoạt lại tài khoản thành công", null));
    }
}
