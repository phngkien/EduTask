package com.edutask.controller;

import com.edutask.dto.request.TransactionRequest;
import com.edutask.dto.response.ApiResponse;
import com.edutask.dto.response.SubscriptionResponse;
import com.edutask.dto.response.TransactionResponse;
import com.edutask.entity.Plan;
import com.edutask.entity.User;
import com.edutask.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<Plan>>> getAllPlans() {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getAllPlans()));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> subscribe(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransactionRequest request) {
        User user = (User) userDetails;
        SubscriptionResponse sub = subscriptionService.subscribeUser(user, request);
        return ResponseEntity.ok(ApiResponse.success("Đăng ký gói thành công", sub));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getActiveSubscription(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        return subscriptionService.getActiveSubscription(user.getUserId())
                .map(sub -> ResponseEntity.ok(ApiResponse.success(sub)))
                .orElse(ResponseEntity.ok(ApiResponse.success("Chưa có gói đăng ký", null)));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactions(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getUserTransactions(user.getUserId())));
    }
}
