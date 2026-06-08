package com.edutask;

import com.edutask.entity.Role;
import com.edutask.entity.User;
import com.edutask.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EdutaskApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void contextLoads() {
	}

	@Test
	void testForgotPasswordAndResetPasswordFlow() throws Exception {
		String testEmail = "test_reset_flow@gmail.com";
		
		// Clean up user if any
		userRepository.findByEmail(testEmail).ifPresent(userRepository::delete);

		// Create test user
		User user = User.builder()
				.fullName("Test Reset Flow User")
				.email(testEmail)
				.passwordHash(passwordEncoder.encode("oldPassword123"))
				.role(Role.USER)
				.build();
		userRepository.save(user);

		try {
			// 1. Request forgot-password link
			mockMvc.perform(post("/api/auth/forgot-password")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"email\": \"" + testEmail + "\"}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true));

			// Fetch user from DB to get the generated token
			User updatedUser = userRepository.findByEmail(testEmail).orElseThrow();
			String token = updatedUser.getResetPasswordToken();
			
			assertNotNull(token);
			assertNotNull(updatedUser.getResetPasswordTokenExpiry());
			assertTrue(updatedUser.getResetPasswordTokenExpiry().isAfter(LocalDateTime.now()));

			// 2. Perform reset password using the token
			mockMvc.perform(post("/api/auth/reset-password")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"token\": \"" + token + "\", \"newPassword\": \"newPassword123\"}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true));

			// Verify user password is updated and token is cleared
			User resetUser = userRepository.findByEmail(testEmail).orElseThrow();
			assertNull(resetUser.getResetPasswordToken());
			assertNull(resetUser.getResetPasswordTokenExpiry());
			assertTrue(passwordEncoder.matches("newPassword123", resetUser.getPasswordHash()));

			// 3. Test login with the new password
			mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"username\": \"" + testEmail + "\", \"password\": \"newPassword123\"}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true));

		} finally {
			// Clean up user
			userRepository.findByEmail(testEmail).ifPresent(userRepository::delete);
		}
	}
}
