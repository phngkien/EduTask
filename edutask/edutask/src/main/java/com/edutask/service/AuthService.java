package com.edutask.service;

import com.edutask.dto.request.LoginRequest;
import com.edutask.dto.request.RefreshTokenRequest;
import com.edutask.dto.request.RegisterRequest;
import com.edutask.dto.response.AuthResponse;
import com.edutask.entity.Token;
import com.edutask.entity.User;
import com.edutask.entity.Role;
import com.edutask.repository.TokenRepository;
import com.edutask.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ActivityLogService activityLogService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        user = userRepository.save(user);

        String accessToken = jwtService.generateToken(user.getEmail());
        String refreshToken = generateAndSaveRefreshToken(user);

        activityLogService.logAction(user, "REGISTER", "Người dùng đăng ký thành công");

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .userId(user.getUserId())
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (Exception e) {
            throw new BadCredentialsException("Email hoặc mật khẩu không chính xác");
        }

        User user = userRepository.findByEmail(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (user.getDeletedAt() != null) {
            throw new RuntimeException("Tài khoản đã bị vô hiệu hóa");
        }

        String accessToken = jwtService.generateToken(user.getEmail());
        String refreshToken = generateAndSaveRefreshToken(user);

        activityLogService.logAction(user, "LOGIN", "Người dùng đăng nhập thành công");

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .userId(user.getUserId())
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        Token token = tokenRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ"));

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(token);
            throw new RuntimeException("Refresh token đã hết hạn, vui lòng đăng nhập lại");
        }

        User user = token.getUser();
        String accessToken = jwtService.generateToken(user.getEmail());

        // Rotate refresh token
        tokenRepository.delete(token);
        String newRefreshToken = generateAndSaveRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .userId(user.getUserId())
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public void logout(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            tokenRepository.deleteByUser(user);
            activityLogService.logAction(user, "LOGOUT", "Người dùng đăng xuất");
        });
    }

    private String generateAndSaveRefreshToken(User user) {
        // Xóa refresh token cũ trước khi tạo mới
        tokenRepository.deleteByUser(user);

        String tokenStr = UUID.randomUUID().toString();
        Token token = Token.builder()
                .user(user)
                .refreshToken(tokenStr)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();
        tokenRepository.save(token);
        return tokenStr;
    }
}
