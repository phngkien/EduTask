package com.edutask.assignment;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentTaskResponse {

    private Long taskId;
    private String taskName;

    private Long assigneeId;
    private String assigneeName;

    private Long suggestedAssigneeId;
    private String assignmentMode;
    private Double assignmentScore;
    private String assignmentReason;
    private String status;
}