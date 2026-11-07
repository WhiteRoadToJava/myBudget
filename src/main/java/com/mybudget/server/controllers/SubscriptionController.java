package com.mybudget.server.controllers;

import com.mybudget.server.dto.subscription.SubscriptionInfo;
import com.mybudget.server.services.subscription.SubscriptionService;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// the signed-in user's own subscription (banner and settings page)
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserUtils userUtils;

    @GetMapping("/subscription")
    public ResponseEntity<SubscriptionInfo> mySubscription() {
        return ResponseEntity.ok(subscriptionService.infoOf(userUtils.getCurrentAuthenticatedUser()));
    }
}
