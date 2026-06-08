package com.edutask.service;

import com.edutask.dto.request.UpdateProfileRequest;
import com.edutask.dto.response.UserProfileResponse;
import com.edutask.entity.User;
import com.edutask.repository.UserRepository;
import com.edutask.repository.ActivityLogRepository;
import com.edutask.entity.ActivityLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;

    public List<ActivityLog> getActivities(Long userId) {
        // Giới hạn chỉ lấy tối đa 10 hoạt động gần nhất để hiển thị nhanh trên Dashboard
        return activityLogRepository.findByUserUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(10)
                .collect(Collectors.toList());
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getDeletedAt() == null)
                .map(this::toProfileResponse)
                .collect(Collectors.toList());
    }

    public UserProfileResponse getProfile(User user) {
        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getSkills() != null) {
            user.setSkills(request.getSkills());
        }
        if (request.getAvailability() != null) {
            user.setAvailability(request.getAvailability());
        }

        return toProfileResponse(userRepository.save(user));
    }

    @Transactional
    public void softDeleteUser(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setDeletedAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    public UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .skills(user.getSkills())
                .availability(user.getAvailability())
                .role(user.getRole() != null ? user.getRole().name() : "USER")
                .build();
    }
}
