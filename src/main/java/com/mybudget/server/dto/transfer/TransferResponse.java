package com.mybudget.server.dto.transfer;

import com.mybudget.server.modules.Account;
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
}
