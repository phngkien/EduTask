package com.edutask.assignment;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentSuggestionResponse {

    private Long userId;
    private String fullName;
    private String skills;
    private Integer activeTaskCount;
    private Integer maxActiveTasks;

    private Double totalScore;
    private Double skillScore;
    private Double workloadScore;
    private Double priorityScore;
    private Double availabilityScore;

    private String reason;
}