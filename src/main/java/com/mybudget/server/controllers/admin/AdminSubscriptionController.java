package com.mybudget.server.controllers.admin;

import com.mybudget.server.dto.subscription.AdminUserDetails;
import com.mybudget.server.dto.subscription.AdminUserSummary;
import com.mybudget.server.dto.subscription.ExtendSubscriptionRequest;
import com.mybudget.server.dto.subscription.PageResult;
import com.mybudget.server.dto.subscription.SubscriptionStats;
import com.mybudget.server.services.subscription.PaymentRequestService;
import com.mybudget.server.services.subscription.SubscriptionService;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Admin only: SecurityConfig protects /api/admin/** with ROLE_ADMIN
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserUtils userUtils;
    private final PaymentRequestService paymentRequestService;

    // ?q=ali&filter=EXPIRING&page=0&size=20  (filter: TRIAL, ACTIVE, EXPIRING, EXPIRED, NOT_SET)
    @GetMapping("/search")
    public ResponseEntity<PageResult<AdminUserSummary>> search(
            @RequestParam(name = "q", required = false) String text,
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(subscriptionService.search(text, filter, page, size));
    }

    @GetMapping("/stats")
    public ResponseEntity<SubscriptionStats> stats() {
        return ResponseEntity.ok(subscriptionService.stats(paymentRequestService.pendingCount()));
    }

    @GetMapping("/{userId}/subscription")
    public ResponseEntity<AdminUserDetails> details(@PathVariable String userId) {
        return ResponseEntity.ok(subscriptionService.details(userId));
    }

    @PostMapping("/{userId}/subscription/extend")
    public ResponseEntity<AdminUserDetails> extend(@PathVariable String userId,
                                                   @RequestBody ExtendSubscriptionRequest request) {
        String admin = userUtils.getCurrentAuthenticatedUser().getUsername();
        return ResponseEntity.ok(subscriptionService.extend(userId, request, admin));
    }

    // body: { "enabled": false }
    @PatchMapping("/{userId}/enabled")
    public ResponseEntity<AdminUserDetails> setEnabled(@PathVariable String userId,
                                                       @RequestBody Map<String, Boolean> body) {
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        return ResponseEntity.ok(subscriptionService.setEnabled(userId, enabled));
    }
}
