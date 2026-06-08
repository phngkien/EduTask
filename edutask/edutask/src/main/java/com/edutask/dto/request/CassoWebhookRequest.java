package com.edutask.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CassoWebhookRequest {
    private int error;
    private String message;
    private List<CassoTransaction> data;

    @Data
    public static class CassoTransaction {
        private Long id;
        private String when;
        private BigDecimal amount;
        private String description;
        private String subAccount;
    }
}
