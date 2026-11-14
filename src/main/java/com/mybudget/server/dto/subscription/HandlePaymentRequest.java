package com.mybudget.server.dto.subscription;

import lombok.Data;

import java.time.LocalDate;

// Admin decision. Approve: months / until default to what the user asked for.
// Reject: adminNote is the reason shown to the user.
@Data
public class HandlePaymentRequest {
    private Integer months;
    private LocalDate until;
    private String adminNote;
}
