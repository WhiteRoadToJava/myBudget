package com.mybudget.server.modules;

import com.mybudget.server.modules.enums.PaymentMethod;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

// One extension of a user's subscription, recorded by an admin.
// The history is the reference when a user asks "I paid, why is it not active?".
// A future online payment creates the same record automatically.
@Data
@NoArgsConstructor
@Document(collection = "subscription_payments")
public class SubscriptionPayment {
    @Id
    private String id;
    @Indexed
    private String userId;
    private String username;
    private LocalDate previousEndsAt;
    private LocalDate newEndsAt;
    private Integer months;          // null when an exact date was chosen
    private BigDecimal amount;       // null for a free extension
    private String currency;         // SYP, USD...
    private PaymentMethod method;
    private String reference;        // transaction or receipt number
    private String note;
    private String recordedBy;       // admin username
    private Instant createdAt;
}
