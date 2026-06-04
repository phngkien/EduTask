package com.edutask.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupRequest {
    @NotBlank(message = "Tên nhóm không được để trống")
    private String groupName;

    private LocalDateTime deadline;
}
