package com.edutask.service;

import com.edutask.dto.request.GroupRequest;
import com.edutask.dto.response.GroupDetailResponse;
import com.edutask.dto.response.GroupMemberResponse;
import com.edutask.entity.Group;
import com.edutask.entity.GroupMember;
import com.edutask.entity.GroupMemberId;
import com.edutask.entity.User;
import com.edutask.repository.GroupMemberRepository;
import com.edutask.repository.GroupRepository;
import com.edutask.repository.TaskRepository;
import com.edutask.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ActivityLogService activityLogService;
    private final SubscriptionService subscriptionService;

    @Transactional(readOnly = true)
    public List<GroupDetailResponse> getAllGroups() {
        return groupRepository.findByDeletedAtIsNull().stream()
                .map(this::toDetailResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GroupDetailResponse> getGroupsByMember(Long userId) {
        return groupMemberRepository.findByIdUserId(userId).stream()
                .map(GroupMember::getGroup)
                .filter(group -> group != null && "ACTIVE".equals(group.getStatus()))
                .map(this::toDetailResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<GroupDetailResponse> getGroupById(Long id) {
        return groupRepository.findById(id)
                .filter(g -> g.getDeletedAt() == null)
                .map(this::toDetailResponse);
    }

    @Transactional
    public GroupDetailResponse createGroup(GroupRequest request, User creator) {
        // Kiểm tra giới hạn gói miễn phí (tối đa 1 nhóm)
        if (!subscriptionService.hasActivePaidSubscription(creator)) {
            long groupCount = groupRepository.countByCreatorUserIdAndDeletedAtIsNull(creator.getUserId());
            if (groupCount >= 1) {
                throw new RuntimeException("Gói miễn phí giới hạn tối đa 1 nhóm học. Vui lòng nâng cấp gói để tạo thêm nhóm.");
            }
        }

        Group group = Group.builder()
                .groupName(request.getGroupName())
                .creator(creator)
                .deadline(request.getDeadline())
                .status("ACTIVE")
                .build();

        group = groupRepository.save(group);

        GroupMemberId memberId = new GroupMemberId(group.getGroupId(), creator.getUserId());
        GroupMember adminMember = GroupMember.builder()
                .id(memberId)
                .group(group)
                .user(creator)
                .role("ADMIN")
                .contributionScore(100)
                .build();
        groupMemberRepository.save(adminMember);

        activityLogService.logAction(creator, "CREATE_GROUP", "Tạo nhóm: " + group.getGroupName());

        return toDetailResponse(group);
    }

    @Transactional
    public GroupMemberResponse addMemberToGroup(Long groupId, Long userId, String role, User actor) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm"));

        if (group.getCreator() == null || !group.getCreator().getUserId().equals(actor.getUserId())) {
            throw new RuntimeException("Chỉ trưởng nhóm mới có quyền mời thành viên mới vào nhóm.");
        }

        if (group.getDeletedAt() != null) {
            throw new RuntimeException("Nhóm đã bị xóa");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        GroupMemberId memberId = new GroupMemberId(groupId, userId);
        if (groupMemberRepository.existsById(memberId)) {
            throw new RuntimeException("Người dùng đã là thành viên của nhóm này");
        }

        GroupMember member = GroupMember.builder()
                .id(memberId)
                .group(group)
                .user(user)
                .role(role != null ? role : "MEMBER")
                .contributionScore(0)
                .build();

        member = groupMemberRepository.save(member);
        activityLogService.logAction(actor, "ADD_MEMBER",
                "Thêm " + user.getEmail() + " vào nhóm " + group.getGroupName());

        return toMemberResponse(member);
    }

    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getGroupMembers(Long groupId) {
        return groupMemberRepository.findByIdGroupId(groupId).stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public GroupMemberResponse updateContributionScore(Long groupId, Long userId, int scoreDelta) {
        GroupMemberId memberId = new GroupMemberId(groupId, userId);
        GroupMember member = groupMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên trong nhóm"));

        member.setContributionScore(member.getContributionScore() + scoreDelta);
        return toMemberResponse(groupMemberRepository.save(member));
    }

    @Transactional
    public void softDeleteGroup(Long groupId, User actor) {
        groupRepository.findById(groupId).ifPresent(group -> {
            if (group.getCreator() == null || !group.getCreator().getUserId().equals(actor.getUserId())) {
                throw new RuntimeException("Chỉ trưởng nhóm mới có quyền xóa nhóm học này.");
            }
            group.setDeletedAt(LocalDateTime.now());
            group.setStatus("DELETED");
            groupRepository.save(group);
            activityLogService.logAction(actor, "DELETE_GROUP", "Xóa nhóm: " + group.getGroupName());
        });
    }

    // ===== Mapping helpers =====

    public GroupDetailResponse toDetailResponse(Group group) {
        long total = taskRepository.countByGroupGroupIdAndDeletedAtIsNull(group.getGroupId());
        long done = taskRepository.countByGroupGroupIdAndStatusAndDeletedAtIsNull(group.getGroupId(), "DONE");
        long members = groupMemberRepository.countByIdGroupId(group.getGroupId());
        int progress = total > 0 ? (int) Math.round((done * 100.0) / total) : 0;

        return GroupDetailResponse.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .status(group.getStatus())
                .deadline(group.getDeadline())
                .createdAt(group.getCreatedAt())
                .creatorId(group.getCreator() != null ? group.getCreator().getUserId() : null)
                .creatorName(group.getCreator() != null ? group.getCreator().getFullName() : null)
                .membersCount((int) members)
                .totalTasks((int) total)
                .completedTasks((int) done)
                .progress(progress)
                .build();
    }

    private GroupMemberResponse toMemberResponse(GroupMember member) {
        return GroupMemberResponse.builder()
                .groupId(member.getId().getGroupId())
                .groupName(member.getGroup().getGroupName())
                .userId(member.getUser().getUserId())
                .fullName(member.getUser().getFullName())
                .email(member.getUser().getEmail())
                .avatarUrl(member.getUser().getAvatarUrl())
                .role(member.getRole())
                .contributionScore(member.getContributionScore())
                .joinedAt(member.getJoinedAt())
                .build();
    }

    @Transactional
    public void removeMemberFromGroup(Long groupId, Long userId, User actor) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm"));

        if (group.getDeletedAt() != null) {
            throw new RuntimeException("Nhóm đã bị xóa");
        }

        boolean isCreator = group.getCreator() != null && group.getCreator().getUserId().equals(actor.getUserId());
        boolean isSelf = actor.getUserId().equals(userId);

        if (!isCreator && !isSelf) {
            throw new RuntimeException("Bạn không có quyền thực hiện hành động này. Chỉ trưởng nhóm mới có quyền xóa thành viên, hoặc bạn chỉ có thể tự rời nhóm.");
        }

        if (isCreator && isSelf) {
             throw new RuntimeException("Trưởng nhóm không thể tự xóa bản thân khỏi nhóm. Vui lòng chọn tính năng Xóa nhóm.");
        }

        GroupMemberId memberId = new GroupMemberId(groupId, userId);
        GroupMember member = groupMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên này trong nhóm"));

        List<com.edutask.entity.Task> tasks = taskRepository.findByGroupGroupIdAndDeletedAtIsNull(groupId);
        for (com.edutask.entity.Task t : tasks) {
            if (t.getAssignee() != null && t.getAssignee().getUserId().equals(userId)) {
                t.setAssignee(null);
                taskRepository.save(t);
            }
        }

        groupMemberRepository.delete(member);
        
        String actionMsg = isSelf ? "đã tự rời khỏi nhóm" : ("đã xóa thành viên " + member.getUser().getEmail() + " khỏi nhóm");
        activityLogService.logAction(actor, "REMOVE_MEMBER", actionMsg + " " + group.getGroupName());
    }
}
