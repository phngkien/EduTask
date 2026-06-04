package com.edutask.repository;

import com.edutask.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    List<Group> findByDeletedAtIsNull();
    List<Group> findByCreatorUserIdAndDeletedAtIsNull(Long userId);
}
