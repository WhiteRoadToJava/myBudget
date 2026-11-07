package com.mybudget.server.dto.subscription;

import com.mybudget.server.modules.enums.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;

// sent by the user from the "My subscription" page
@Data
public class CreatePaymentRequest {
    private Integer months;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod method;
    private String reference;
    private String note;
}
