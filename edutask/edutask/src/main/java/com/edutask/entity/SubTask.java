package com.edutask.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "subtasks", indexes = {
    @Index(name = "idx_subtasks_task", columnList = "task_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subtask_id")
    private Long subTaskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @JsonIgnore
    private Task task;

    @Column(name = "content", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String content;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted;
}
