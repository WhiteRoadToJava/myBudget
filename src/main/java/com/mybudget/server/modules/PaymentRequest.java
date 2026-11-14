package com.mybudget.server.modules;

import com.mybudget.server.modules.enums.PaymentMethod;
import com.mybudget.server.modules.enums.PaymentRequestStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

// "I have paid": sent by a user, approved or rejected by an admin.
// Approving records a SubscriptionPayment and extends the subscription.
@Data
@NoArgsConstructor
@Document(collection = "payment_requests")
public class PaymentRequest {
    @Id
    private String id;
    @Indexed
    private String userId;
    private String username;
    private Integer months;          // what the user paid for
    private BigDecimal amount;
    private String currency;
    private PaymentMethod method;
    private String reference;        // transaction number from the transfer app or receipt
    private String note;
    @Indexed
    private PaymentRequestStatus status;
    private Instant createdAt;

    private String handledBy;        // admin username
    private Instant handledAt;
    private String adminNote;        // shown to the user, e.g. the reason for a rejection
    private String paymentId;        // the SubscriptionPayment created on approval
}
