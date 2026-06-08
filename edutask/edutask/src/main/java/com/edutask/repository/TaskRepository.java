package com.edutask.repository;

import com.edutask.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByGroupGroupIdAndDeletedAtIsNull(Long groupId);
    List<Task> findByAssigneeUserIdAndDeletedAtIsNull(Long userId);
    List<Task> findByDeletedAtIsNull();
    long countByGroupGroupIdAndDeletedAtIsNull(Long groupId);
    long countByGroupGroupIdAndStatusAndDeletedAtIsNull(Long groupId, String status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignee.userId = :userId AND t.deletedAt IS NULL AND (t.status IS NULL OR UPPER(t.status) IN ('TODO', 'TO_DO', 'DOING', 'IN_PROGRESS'))")
    long countActiveTasksByUserId(@Param("userId") Long userId);
}
