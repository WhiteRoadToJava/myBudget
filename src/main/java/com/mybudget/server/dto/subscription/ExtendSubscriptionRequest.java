package com.mybudget.server.dto.subscription;

import com.mybudget.server.modules.enums.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

// Either months (added to the current end, or to today when it has ended) or an exact date
@Data
public class ExtendSubscriptionRequest {
    private Integer months;
    private LocalDate until;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod method;
    private String reference;
    private String note;
}
