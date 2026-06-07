package com.edutask.assignment;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping("/tasks/{taskId}/suggest")
    public List<AssignmentSuggestionResponse> suggestAssignees(@PathVariable Long taskId) {
        return assignmentService.suggestAssignees(taskId);
    }

   @PostMapping("/tasks/{taskId}/auto-assign")
public AssignmentTaskResponse autoAssign(@PathVariable Long taskId) {
    return assignmentService.autoAssign(taskId);
}

  @PutMapping("/tasks/{taskId}/approve")
public AssignmentTaskResponse approveAssignment(
        @PathVariable Long taskId,
        @RequestBody Map<String, Long> request
) {
    Long assigneeId = request.get("assigneeId");
    return assignmentService.approveAssignment(taskId, assigneeId);
}
    @GetMapping("/tasks/{taskId}/suggestions")
    public List<AssignmentSuggestion> getSuggestions(@PathVariable Long taskId) {
        return assignmentService.getSuggestions(taskId);
    }

    @GetMapping("/tasks/{taskId}/logs")
    public List<AssignmentLog> getAssignmentLogs(@PathVariable Long taskId) {
        return assignmentService.getAssignmentLogs(taskId);
    }
}