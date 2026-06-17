package com.edutask.service;

import com.edutask.dto.request.UpdateProfileRequest;
import com.edutask.dto.request.AdminUserRequest;
import com.edutask.dto.response.UserProfileResponse;
import com.edutask.dto.response.AdminStatsResponse;
import com.edutask.entity.User;
import com.edutask.entity.Role;
import com.edutask.repository.UserRepository;
import com.edutask.repository.ActivityLogRepository;
import com.edutask.entity.ActivityLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

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

    @Transactional
    public void restoreUser(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setDeletedAt(null);
            userRepository.save(user);
        });
    }

    public Page<UserProfileResponse> getAdminUsers(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("userId").descending());
        return userRepository.searchUsers(query, pageable).map(this::toProfileResponse);
    }

    @Transactional
    public UserProfileResponse createUserByAdmin(AdminUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống");
        }
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : Role.USER)
                .build();
        return toProfileResponse(userRepository.save(user));
    }

    @Transactional
    public UserProfileResponse updateUserByAdmin(Long id, AdminUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (!user.getEmail().equalsIgnoreCase(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng bởi người dùng khác");
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole() != null ? request.getRole() : Role.USER);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        return toProfileResponse(userRepository.save(user));
    }

    public AdminStatsResponse getAdminStats() {
        long total = userRepository.count();
        long active = userRepository.findAll().stream().filter(u -> u.getDeletedAt() == null).count();
        long admins = userRepository.findAll().stream().filter(u -> Role.ADMIN.equals(u.getRole())).count();
        return AdminStatsResponse.builder()
                .totalUsers(total)
                .activeUsers(active)
                .adminUsers(admins)
                .build();
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
                .enabled(user.getDeletedAt() == null)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
