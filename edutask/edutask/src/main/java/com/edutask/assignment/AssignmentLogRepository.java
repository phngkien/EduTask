package com.edutask.assignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentLogRepository extends JpaRepository<AssignmentLog, Long> {

    List<AssignmentLog> findByTaskIdOrderByCreatedAtDesc(Long taskId);
}