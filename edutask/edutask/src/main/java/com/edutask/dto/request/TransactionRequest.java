package com.edutask.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {
    @NotNull(message = "Gói đăng ký không được để trống")
    private Long planId;

    @NotNull(message = "Số tiền không được để trống")
    private BigDecimal amount;

    private String paymentMethod;
}
