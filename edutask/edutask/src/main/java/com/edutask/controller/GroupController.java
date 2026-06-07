package com.edutask.controller;

import com.edutask.dto.request.GroupRequest;
import com.edutask.dto.response.ApiResponse;
import com.edutask.dto.response.GroupDetailResponse;
import com.edutask.dto.response.GroupMemberResponse;
import com.edutask.entity.User;
import com.edutask.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GroupDetailResponse>>> getAllGroups() {
        return ResponseEntity.ok(ApiResponse.success(groupService.getAllGroups()));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<GroupDetailResponse>>> getMyGroups(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        return ResponseEntity.ok(ApiResponse.success(groupService.getGroupsByMember(user.getUserId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GroupDetailResponse>> getGroupById(@PathVariable Long id) {
        return groupService.getGroupById(id)
                .map(g -> ResponseEntity.ok(ApiResponse.success(g)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GroupDetailResponse>> createGroup(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GroupRequest request) {
        User creator = (User) userDetails;
        GroupDetailResponse created = groupService.createGroup(request, creator);
        return ResponseEntity.ok(ApiResponse.success("Tạo nhóm thành công", created));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<GroupMemberResponse>> addMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long groupId,
            @RequestParam Long userId,
            @RequestParam(required = false) String role) {
        User actor = (User) userDetails;
        GroupMemberResponse member = groupService.addMemberToGroup(groupId, userId, role, actor);
        return ResponseEntity.ok(ApiResponse.success("Thêm thành viên thành công", member));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<GroupMemberResponse>>> getMembers(
            @PathVariable("id") Long groupId) {
        return ResponseEntity.ok(ApiResponse.success(groupService.getGroupMembers(groupId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User actor = (User) userDetails;
        groupService.softDeleteGroup(id, actor);
        return ResponseEntity.ok(ApiResponse.success("Xóa nhóm thành công", null));
    }
}
