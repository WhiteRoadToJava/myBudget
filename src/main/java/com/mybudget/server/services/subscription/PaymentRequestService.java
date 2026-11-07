package com.mybudget.server.services.subscription;

import com.mybudget.server.dto.subscription.AdminUserDetails;
import com.mybudget.server.dto.subscription.CreatePaymentRequest;
import com.mybudget.server.dto.subscription.ExtendSubscriptionRequest;
import com.mybudget.server.dto.subscription.HandlePaymentRequest;
import com.mybudget.server.exeptions.ConflictException;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.modules.PaymentRequest;
import com.mybudget.server.modules.SubscriptionPayment;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.enums.PaymentMethod;
import com.mybudget.server.modules.enums.PaymentRequestStatus;
import com.mybudget.server.repositories.PaymentRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Users tell the admin they have paid (method and transaction number);
 * the admin checks the transfer app and approves or rejects.
 * One pending request per user at a time, so a double click sends nothing twice.
 */
@Service
@RequiredArgsConstructor
public class PaymentRequestService {

    private final PaymentRequestRepository repository;
    private final SubscriptionService subscriptionService;

    // ---------- user ----------

    public PaymentRequest create(User user, CreatePaymentRequest request) {
        validate(request);
        if (repository.existsByUserIdAndStatus(user.getId(), PaymentRequestStatus.PENDING)) {
            throw new ConflictException("You already have a payment waiting for approval");
        }
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setUserId(user.getId());
        paymentRequest.setUsername(user.getUsername());
        paymentRequest.setMonths(request.getMonths());
        paymentRequest.setAmount(request.getAmount());
        paymentRequest.setCurrency(request.getCurrency() == null ? null : request.getCurrency().trim().toUpperCase());
        paymentRequest.setMethod(request.getMethod());
        paymentRequest.setReference(request.getReference().trim());
        paymentRequest.setNote(clean(request.getNote()));
        paymentRequest.setStatus(PaymentRequestStatus.PENDING);
        paymentRequest.setCreatedAt(Instant.now());
        return repository.save(paymentRequest);
    }

    public List<PaymentRequest> mine(User user) {
        return repository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
    }

    private void validate(CreatePaymentRequest request) {
        if (request.getMonths() == null || request.getMonths() < 1 || request.getMonths() > 12) {
            throw new IllegalArgumentException("Months must be between 1 and 12");
        }
        if (request.getMethod() == null || request.getMethod() == PaymentMethod.FREE) {
            throw new IllegalArgumentException("Choose how you paid");
        }
        if (request.getReference() == null || request.getReference().isBlank()) {
            throw new IllegalArgumentException("Give the transaction or receipt number");
        }
        if (request.getReference().length() > 100 || (request.getNote() != null && request.getNote().length() > 500)) {
            throw new IllegalArgumentException("The text is too long");
        }
        if (request.getAmount() != null && request.getAmount().signum() < 0) {
            throw new IllegalArgumentException("The amount can not be negative");
        }
    }

    // ---------- admin ----------

    public List<PaymentRequest> pending() {
        return repository.findAllByStatusOrderByCreatedAtAsc(PaymentRequestStatus.PENDING);
    }

    public long pendingCount() {
        return repository.countByStatus(PaymentRequestStatus.PENDING);
    }

    // records the payment and extends, exactly like a payment the admin enters by hand
    public AdminUserDetails approve(String requestId, HandlePaymentRequest decision, String adminUsername) {
        PaymentRequest request = findPending(requestId);

        ExtendSubscriptionRequest extend = new ExtendSubscriptionRequest();
        if (decision != null && decision.getUntil() != null) {
            extend.setUntil(decision.getUntil());
        } else {
            extend.setMonths(decision != null && decision.getMonths() != null ? decision.getMonths() : request.getMonths());
        }
        extend.setAmount(request.getAmount());
        extend.setCurrency(request.getCurrency());
        extend.setMethod(request.getMethod());
        extend.setReference(request.getReference());
        extend.setNote(joinNotes(request.getNote(), decision == null ? null : decision.getAdminNote()));

        AdminUserDetails details = subscriptionService.extend(request.getUserId(), extend, adminUsername);

        request.setStatus(PaymentRequestStatus.APPROVED);
        request.setHandledBy(adminUsername);
        request.setHandledAt(Instant.now());
        request.setAdminNote(decision == null ? null : clean(decision.getAdminNote()));
        request.setPaymentId(details.payments().isEmpty() ? null : details.payments().get(0).getId());
        repository.save(request);
        return details;
    }

    public PaymentRequest reject(String requestId, HandlePaymentRequest decision, String adminUsername) {
        PaymentRequest request = findPending(requestId);
        String reason = decision == null ? null : clean(decision.getAdminNote());
        if (reason == null) {
            throw new IllegalArgumentException("Write the reason, the user will see it");
        }
        request.setStatus(PaymentRequestStatus.REJECTED);
        request.setHandledBy(adminUsername);
        request.setHandledAt(Instant.now());
        request.setAdminNote(reason);
        return repository.save(request);
    }

    private PaymentRequest findPending(String requestId) {
        PaymentRequest request = repository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment request not found"));
        if (request.getStatus() != PaymentRequestStatus.PENDING) {
            throw new ConflictException("This payment request was already handled");
        }
        return request;
    }

    private String joinNotes(String userNote, String adminNote) {
        String a = clean(userNote);
        String b = clean(adminNote);
        if (a == null) return b;
        if (b == null) return a;
        return a + " | " + b;
    }

    private String clean(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
