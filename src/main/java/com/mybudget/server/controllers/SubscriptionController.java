package com.mybudget.server.controllers;

import com.mybudget.server.dto.subscription.CreatePaymentRequest;
import com.mybudget.server.dto.subscription.SubscriptionInfo;
import com.mybudget.server.modules.PaymentRequest;
import com.mybudget.server.services.subscription.PaymentRequestService;
import com.mybudget.server.services.subscription.SubscriptionService;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// the signed-in user's own subscription (banner and settings page)
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserUtils userUtils;
    private final PaymentRequestService paymentRequestService;

    @GetMapping("/subscription")
    public ResponseEntity<SubscriptionInfo> mySubscription() {
        return ResponseEntity.ok(subscriptionService.infoOf(userUtils.getCurrentAuthenticatedUser()));
    }

    // "I have paid": allowed even in read-only mode (WebConfig excludes /api/user/subscription/**)
    @PostMapping("/subscription/payment-requests")
    public ResponseEntity<PaymentRequest> requestApproval(@RequestBody CreatePaymentRequest request) {
        PaymentRequest saved = paymentRequestService.create(userUtils.getCurrentAuthenticatedUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/subscription/payment-requests")
    public ResponseEntity<List<PaymentRequest>> myRequests() {
        return ResponseEntity.ok(paymentRequestService.mine(userUtils.getCurrentAuthenticatedUser()));
    }
}
