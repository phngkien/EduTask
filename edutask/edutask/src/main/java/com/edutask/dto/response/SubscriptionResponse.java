package com.edutask.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    private Long subscriptionId;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    // Plan info
    private Long planId;
    private String planName;
    private BigDecimal price;
    private String currency;
    private Integer durationDays;
    private String features;
}
