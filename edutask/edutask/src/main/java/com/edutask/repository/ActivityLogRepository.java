package com.edutask.repository;

import com.edutask.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COUNT(a) FROM ActivityLog a WHERE a.user.userId = :userId AND a.action = :action AND a.createdAt >= :startOfDay")
    long countActionsToday(@Param("userId") Long userId, @Param("action") String action, @Param("startOfDay") LocalDateTime startOfDay);
}
