package com.edutask.service;

import com.edutask.dto.request.LoginRequest;
import com.edutask.dto.request.RefreshTokenRequest;
import com.edutask.dto.request.RegisterRequest;
import com.edutask.dto.response.AuthResponse;
import com.edutask.entity.Token;
import com.edutask.entity.User;
import com.edutask.entity.Role;
import com.edutask.entity.UserSubscription;
import com.edutask.repository.TokenRepository;
import com.edutask.repository.UserRepository;
import com.edutask.repository.PlanRepository;
import com.edutask.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.mail.javamail.JavaMailSender;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ActivityLogService activityLogService;
    private final PlanRepository planRepository;
    private final UserSubscriptionRepository subscriptionRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private JavaMailSender mailSender;



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

        // Tự động tặng gói dùng thử 7 ngày (Free Trial)
        final User finalUser = user;
        planRepository.findAll().stream().findFirst().ifPresent(plan -> {
            UserSubscription sub = UserSubscription.builder()
                    .user(finalUser)
                    .plan(plan)
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusDays(7))
                    .status("ACTIVE")
                    .build();
            subscriptionRepository.save(sub);
        });

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

    @Transactional
    public String forgotPassword(String email, String origin) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email này"));

        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        activityLogService.logAction(user, "FORGOT_PASSWORD_REQUEST", "Yêu cầu khôi phục mật khẩu qua liên kết");

        // Gửi email
        if (mailSender != null) {
            try {
                org.springframework.mail.SimpleMailMessage mailMessage = new org.springframework.mail.SimpleMailMessage();
                mailMessage.setTo(user.getEmail());
                mailMessage.setSubject("[EduTask] Khôi phục mật khẩu tài khoản");
                mailMessage.setText("Chào bạn,\n\n" +
                        "Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản EduTask. Vui lòng bấm vào liên kết dưới đây để đặt lại mật khẩu mới (liên kết có hiệu lực trong 15 phút):\n\n" +
                        origin + "/reset-password.html?token=" + token + "\n\n" +
                        "Nếu bạn không yêu cầu điều này, vui lòng bỏ qua email này.\n\n" +
                        "Trân trọng,\n" +
                        "Đội ngũ EduTask");
                mailSender.send(mailMessage);
                System.out.println(">>> Email khôi phục mật khẩu đã được gửi đến: " + user.getEmail());
            } catch (Exception e) {
                System.err.println(">>> LỖI GỬI EMAIL KHÔI PHỤC (Có thể chưa cấu hình SMTP): " + e.getMessage());
                System.out.println(">>> LIÊN KẾT ĐẶT LẠI MẬT KHẨU (Dùng để test): " + origin + "/reset-password.html?token=" + token);
            }
        } else {
            System.out.println(">>> [MAIL SENDER CHƯA CẤU HÌNH] LIÊN KẾT ĐẶT LẠI MẬT KHẨU: " + origin + "/reset-password.html?token=" + token);
        }


        return token;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new RuntimeException("Mã khôi phục không hợp lệ hoặc đã được sử dụng"));

        if (user.getResetPasswordTokenExpiry() == null || user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Liên kết khôi phục đã hết hạn, vui lòng gửi lại yêu cầu");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);

        activityLogService.logAction(user, "RESET_PASSWORD", "Người dùng đặt lại mật khẩu thành công qua liên kết");
    }
}
