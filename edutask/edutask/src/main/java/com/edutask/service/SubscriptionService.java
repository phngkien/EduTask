package com.edutask.service;

import com.edutask.dto.request.TransactionRequest;
import com.edutask.dto.response.SubscriptionResponse;
import com.edutask.dto.response.TransactionResponse;
import com.edutask.entity.Plan;
import com.edutask.entity.Transaction;
import com.edutask.entity.User;
import com.edutask.entity.UserSubscription;
import com.edutask.repository.PlanRepository;
import com.edutask.repository.TransactionRepository;
import com.edutask.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final PlanRepository planRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final TransactionRepository transactionRepository;
    private final ActivityLogService activityLogService;

    public List<Plan> getAllPlans() {
        return planRepository.findAll();
    }

    public Optional<Plan> getPlanById(Long planId) {
        return planRepository.findById(planId);
    }

    @Transactional
    public SubscriptionResponse subscribeUser(User user, TransactionRequest request) {
        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gói đăng ký"));

        Transaction transaction = Transaction.builder()
                .user(user)
                .plan(plan)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "ONLINE")
                .status("SUCCESS")
                .build();
        transactionRepository.save(transaction);

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(plan.getDurationDays() != null ? plan.getDurationDays() : 30);

        Optional<UserSubscription> existingOpt = subscriptionRepository
                .findFirstByUserUserIdAndStatusOrderByEndDateDesc(user.getUserId(), "ACTIVE");

        UserSubscription subscription;
        if (existingOpt.isPresent()) {
            subscription = existingOpt.get();
            subscription.setEndDate(subscription.getEndDate().plusDays(plan.getDurationDays()));
            subscription.setPlan(plan);
        } else {
            subscription = UserSubscription.builder()
                    .user(user)
                    .plan(plan)
                    .startDate(start)
                    .endDate(end)
                    .status("ACTIVE")
                    .build();
        }

        subscription = subscriptionRepository.save(subscription);
        activityLogService.logAction(user, "SUBSCRIBE", "Đăng ký gói: " + plan.getPlanName());

        return toSubscriptionResponse(subscription);
    }

    public Optional<SubscriptionResponse> getActiveSubscription(Long userId) {
        return subscriptionRepository
                .findFirstByUserUserIdAndStatusOrderByEndDateDesc(userId, "ACTIVE")
                .map(this::toSubscriptionResponse);
    }

    public List<TransactionResponse> getUserTransactions(Long userId) {
        return transactionRepository.findByUserUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());
    }

    private SubscriptionResponse toSubscriptionResponse(UserSubscription sub) {
        Plan plan = sub.getPlan();
        return SubscriptionResponse.builder()
                .subscriptionId(sub.getSubscriptionId())
                .status(sub.getStatus())
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .planId(plan.getPlanId())
                .planName(plan.getPlanName())
                .price(plan.getPrice())
                .currency(plan.getCurrency())
                .durationDays(plan.getDurationDays())
                .features(plan.getFeatures())
                .build();
    }

    private TransactionResponse toTransactionResponse(Transaction t) {
        return TransactionResponse.builder()
                .transactionId(t.getTransactionId())
                .amount(t.getAmount())
                .paymentMethod(t.getPaymentMethod())
                .status(t.getStatus())
                .createdAt(t.getCreatedAt())
                .planName(t.getPlan() != null ? t.getPlan().getPlanName() : null)
                .build();
    }
}
