package com.edutask.controller;

import com.edutask.dto.response.ApiResponse;
import com.edutask.dto.response.NotificationResponse;
import com.edutask.entity.User;
import com.edutask.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly) {
        User user = (User) userDetails;
        List<NotificationResponse> list = unreadOnly
                ? notificationService.getUnreadNotifications(user)
                : notificationService.getMyNotifications(user);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        long count = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = (User) userDetails;
        NotificationResponse updated = notificationService.markAsRead(id, user);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đã đọc", updated));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        int count = notificationService.markAllAsRead(user);
        return ResponseEntity.ok(ApiResponse.success(Map.of("updated", count)));
    }
}
