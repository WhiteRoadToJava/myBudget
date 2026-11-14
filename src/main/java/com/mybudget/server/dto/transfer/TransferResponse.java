package com.mybudget.server.dto.transfer;

import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.enums.FeeDirection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferResponse {
    private String id;
    private Account sourceAccount;
    private Account destinationAccount;

    private Double amountSent;
    private double exChangeRate;
    private double amountReceived ;

    private String description;
    private LocalDateTime createdAt;
    private Map<String, String> image;
    private Double sendFee;
    private FeeDirection sendFeeDirection;
    private Account sendFeeAccount;
    private Double receiveFee;
    private FeeDirection receiveFeeDirection;
    private Account receiveFeeAccount;
    private String sourceCurrency;
    private String destinationCurrency;
    private String sendFeeCurrency;
    private String receiveFeeCurrency;
}
