package com.edutask.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "plan_name", nullable = false)
    private String planName;

    @Column(nullable = false)
    private BigDecimal price;

    private String currency;

    @Column(name = "duration_days")
    private Integer durationDays;

    private String features;
}
