package com.edutask.service;

import com.edutask.dto.request.TransactionRequest;
import com.edutask.dto.request.CassoWebhookRequest;
import com.edutask.dto.response.SubscriptionResponse;
import com.edutask.dto.response.TransactionResponse;
import com.edutask.entity.Plan;
import com.edutask.entity.Transaction;
import com.edutask.entity.User;
import com.edutask.entity.UserSubscription;
import com.edutask.repository.PlanRepository;
import com.edutask.repository.TransactionRepository;
import com.edutask.repository.UserSubscriptionRepository;
import com.edutask.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.cache.annotation.Cacheable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final PlanRepository planRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @Value("${app.casso.secure-token}")
    private String cassoSecureToken;

    @Cacheable(value = "plans")
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

    @Transactional
    public void processCassoWebhook(String receivedToken, CassoWebhookRequest request) {
        if (cassoSecureToken == null || !cassoSecureToken.equals(receivedToken)) {
            throw new RuntimeException("Casso secure token không khớp!");
        }

        if (request.getData() == null) {
            return;
        }

        Pattern pattern = Pattern.compile("EDUTASKSUB(\\d+)USER(\\d+)");

        for (CassoWebhookRequest.CassoTransaction trans : request.getData()) {
            String desc = trans.getDescription();
            if (desc == null) continue;
            
            desc = desc.replaceAll("\\s+", "").toUpperCase();
            Matcher matcher = pattern.matcher(desc);
            
            if (matcher.find()) {
                Long planId = Long.parseLong(matcher.group(1));
                Long userId = Long.parseLong(matcher.group(2));
                
                User user = userRepository.findById(userId).orElse(null);
                Plan plan = planRepository.findById(planId).orElse(null);
                
                if (user != null && plan != null) {
                    if (trans.getAmount().compareTo(plan.getPrice()) >= 0) {
                        Transaction transaction = Transaction.builder()
                                .user(user)
                                .plan(plan)
                                .amount(trans.getAmount())
                                .paymentMethod("CASSO_WEBHOOK")
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
                        subscriptionRepository.save(subscription);
                        
                        activityLogService.logAction(user, "SUBSCRIBE_WEBHOOK", 
                                "Thanh toán tự động thành công cho gói: " + plan.getPlanName() + " số tiền: " + trans.getAmount());
                    } else {
                        System.err.println("Số tiền thanh toán " + trans.getAmount() + " không đủ cho gói " + plan.getPrice());
                    }
                }
            }
        }
    }
}
