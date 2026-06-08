package com.edutask.repository;

import com.edutask.entity.SubTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SubTaskRepository extends JpaRepository<SubTask, Long> {
    List<SubTask> findByTaskTaskId(Long taskId);
    void deleteByTaskTaskId(Long taskId);
}
