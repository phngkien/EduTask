package com.edutask.repository;

import com.edutask.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByGroupGroupIdAndDeletedAtIsNull(Long groupId);
    List<Task> findByAssigneeUserIdAndDeletedAtIsNull(Long userId);
    List<Task> findByDeletedAtIsNull();
    long countByGroupGroupIdAndDeletedAtIsNull(Long groupId);
    long countByGroupGroupIdAndStatusAndDeletedAtIsNull(Long groupId, String status);
}
