package com.mybudget.server.controllers.admin;

import com.mybudget.server.dto.subscription.AdminUserDetails;
import com.mybudget.server.dto.subscription.HandlePaymentRequest;
import com.mybudget.server.modules.PaymentRequest;
import com.mybudget.server.services.subscription.PaymentRequestService;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Admin only: SecurityConfig protects /api/admin/** with ROLE_ADMIN
@RestController
@RequestMapping("/api/admin/payment-requests")
@RequiredArgsConstructor
public class AdminPaymentRequestController {

    private final PaymentRequestService paymentRequestService;
    private final UserUtils userUtils;

    // oldest first: the user who waited longest is handled first
    @GetMapping
    public ResponseEntity<List<PaymentRequest>> pending() {
        return ResponseEntity.ok(paymentRequestService.pending());
    }

    // body (optional): { "months": 3 } or { "until": "2027-01-31" }, and "adminNote"
    @PostMapping("/{requestId}/approve")
    public ResponseEntity<AdminUserDetails> approve(@PathVariable String requestId,
                                                    @RequestBody(required = false) HandlePaymentRequest decision) {
        String admin = userUtils.getCurrentAuthenticatedUser().getUsername();
        return ResponseEntity.ok(paymentRequestService.approve(requestId, decision, admin));
    }

    // body: { "adminNote": "No transfer with this number" }
    @PostMapping("/{requestId}/reject")
    public ResponseEntity<PaymentRequest> reject(@PathVariable String requestId,
                                                 @RequestBody HandlePaymentRequest decision) {
        String admin = userUtils.getCurrentAuthenticatedUser().getUsername();
        return ResponseEntity.ok(paymentRequestService.reject(requestId, decision, admin));
    }
}
