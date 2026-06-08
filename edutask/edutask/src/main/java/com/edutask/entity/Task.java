package com.edutask.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import jakarta.persistence.Column;

@Entity
@Table(name = "tasks", indexes = {
    @Index(name = "idx_tasks_group", columnList = "group_id"),
    @Index(name = "idx_tasks_assignee", columnList = "assignee_id"),
    @Index(name = "idx_tasks_status", columnList = "status"),
    @Index(name = "idx_tasks_deleted", columnList = "deleted_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(name = "task_name", nullable = false)
    private String taskName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    private String status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
private String description;

@Column(name = "category")
private String category;

@Column(name = "priority")
private String priority;

@Column(name = "assignment_mode")
private String assignmentMode;

@Column(name = "suggested_assignee_id")
private Long suggestedAssigneeId;

@Column(name = "assignment_score")
private Double assignmentScore;

@Column(name = "assignment_reason", columnDefinition = "NVARCHAR(MAX)")
private String assignmentReason;
}
