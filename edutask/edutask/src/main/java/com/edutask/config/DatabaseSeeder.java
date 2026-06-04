package com.edutask.config;

import com.edutask.entity.*;
import com.edutask.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        seedPlans();
        seedUsersAndGroups();
    }

    private void seedPlans() {
        if (planRepository.count() == 0) {
            log.info("Seeding subscription plans...");
            Plan freePlan = Plan.builder()
                    .planName("FREE")
                    .price(BigDecimal.ZERO)
                    .currency("VND")
                    .durationDays(30)
                    .features("Quản lý tối đa 3 nhóm, 10 thành viên/nhóm")
                    .build();

            Plan studentPlan = Plan.builder()
                    .planName("STUDENT")
                    .price(new BigDecimal("49000"))
                    .currency("VND")
                    .durationDays(30)
                    .features("Quản lý tối đa 10 nhóm, 30 thành viên/nhóm")
                    .build();

            Plan proPlan = Plan.builder()
                    .planName("PRO")
                    .price(new BigDecimal("199000"))
                    .currency("VND")
                    .durationDays(30)
                    .features("Không giới hạn số lượng nhóm và thành viên, Báo cáo đóng góp thông minh")
                    .build();

            planRepository.saveAll(List.of(freePlan, studentPlan, proPlan));
            log.info("Subscription plans seeded successfully.");
        }
    }

    private void seedUsersAndGroups() {
        if (userRepository.count() == 0) {
            log.info("Seeding users, groups and tasks...");

            String encodedPassword = passwordEncoder.encode("password");

            // Seed Users
            User mainUser = User.builder()
                    .fullName("Nguyễn Minh Đức")
                    .email("minhduc@edutask.com")
                    .passwordHash(encodedPassword)
                    .role(Role.USER)
                    .avatarUrl("https://api.dicebear.com/7.x/adventurer/svg?seed=Duc")
                    .skills("Java, Spring Boot, React, SQL")
                    .availability("Full-time")
                    .build();

            User user2 = User.builder()
                    .fullName("Nguyễn Phương Nam")
                    .email("phuongnam@edutask.com")
                    .passwordHash(encodedPassword)
                    .role(Role.USER)
                    .avatarUrl("https://api.dicebear.com/7.x/adventurer/svg?seed=Nam")
                    .skills("React, HTML/CSS, Tailwind")
                    .availability("Part-time")
                    .build();

            User user3 = User.builder()
                    .fullName("Lê Thùy Trang")
                    .email("thuytrang@edutask.com")
                    .passwordHash(encodedPassword)
                    .role(Role.USER)
                    .avatarUrl("https://api.dicebear.com/7.x/adventurer/svg?seed=Trang")
                    .skills("UI/UX Design, Figma")
                    .availability("Part-time")
                    .build();

            User user4 = User.builder()
                    .fullName("Trần Hoàng Việt")
                    .email("hoangviet@edutask.com")
                    .passwordHash(encodedPassword)
                    .role(Role.USER)
                    .avatarUrl("https://api.dicebear.com/7.x/adventurer/svg?seed=Viet")
                    .skills("Python, Machine Learning")
                    .availability("Full-time")
                    .build();

            User user5 = User.builder()
                    .fullName("Phan Khánh Ly")
                    .email("khanhly@edutask.com")
                    .passwordHash(encodedPassword)
                    .role(Role.USER)
                    .avatarUrl("https://api.dicebear.com/7.x/adventurer/svg?seed=Ly")
                    .skills("Content Writer, SEO")
                    .availability("Part-time")
                    .build();

            mainUser = userRepository.save(mainUser);
            user2 = userRepository.save(user2);
            user3 = userRepository.save(user3);
            user4 = userRepository.save(user4);
            user5 = userRepository.save(user5);

            // Seed Group
            Group group = Group.builder()
                    .groupName("Dự án EXE201 - EduTask")
                    .creator(mainUser)
                    .deadline(LocalDateTime.now().plusDays(30))
                    .status("ACTIVE")
                    .build();

            group = groupRepository.save(group);

            // Seed Group Members
            GroupMember member1 = GroupMember.builder()
                    .id(new GroupMemberId(group.getGroupId(), mainUser.getUserId()))
                    .group(group)
                    .user(mainUser)
                    .role("ADMIN")
                    .contributionScore(100)
                    .build();

            GroupMember member2 = GroupMember.builder()
                    .id(new GroupMemberId(group.getGroupId(), user2.getUserId()))
                    .group(group)
                    .user(user2)
                    .role("MEMBER")
                    .contributionScore(10)
                    .build();

            GroupMember member3 = GroupMember.builder()
                    .id(new GroupMemberId(group.getGroupId(), user3.getUserId()))
                    .group(group)
                    .user(user3)
                    .role("MEMBER")
                    .contributionScore(0)
                    .build();

            groupMemberRepository.saveAll(List.of(member1, member2, member3));

            // Seed Tasks
            Task task1 = Task.builder()
                    .group(group)
                    .taskName("Thiết kế cơ sở dữ liệu")
                    .assignee(mainUser)
                    .dueDate(LocalDateTime.now().plusDays(5))
                    .status("DONE")
                    .build();

            Task task2 = Task.builder()
                    .group(group)
                    .taskName("Phát triển API Đăng nhập/Đăng ký")
                    .assignee(user2)
                    .dueDate(LocalDateTime.now().plusDays(10))
                    .status("IN_PROGRESS")
                    .build();

            Task task3 = Task.builder()
                    .group(group)
                    .taskName("Xây dựng giao diện React")
                    .assignee(user3)
                    .dueDate(LocalDateTime.now().plusDays(15))
                    .status("TODO")
                    .build();

            taskRepository.saveAll(List.of(task1, task2, task3));
            log.info("Users, groups and tasks seeded successfully.");
        }
    }
}
