package com.edutask.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {
    @NotBlank(message = "Tên nhiệm vụ không được để trống")
    private String taskName;

    @NotNull(message = "Nhóm không được để trống")
    private Long groupId;

    private Long assigneeId;

    private LocalDateTime dueDate;

    private String status;
}
