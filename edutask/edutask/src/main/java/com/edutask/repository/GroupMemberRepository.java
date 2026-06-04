package com.edutask.repository;

import com.edutask.entity.GroupMember;
import com.edutask.entity.GroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {
    List<GroupMember> findByIdGroupId(Long groupId);
    List<GroupMember> findByIdUserId(Long userId);
    Optional<GroupMember> findByIdGroupIdAndIdUserId(Long groupId, Long userId);
    long countByIdGroupId(Long groupId);
    boolean existsByIdGroupIdAndIdUserId(Long groupId, Long userId);
}
