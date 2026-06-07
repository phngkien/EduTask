package com.edutask.assignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentSuggestionRepository extends JpaRepository<AssignmentSuggestion, Long> {

    List<AssignmentSuggestion> findByTaskIdOrderByRankNoAsc(Long taskId);

    void deleteByTaskId(Long taskId);
}