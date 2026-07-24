package com.mybudget.server.dto.transfer;

import java.time.LocalDateTime;
import java.util.Map;

import com.mybudget.server.modules.Account;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferRequest {
    private Account sourceAccount;
    private Account destinationAccount;

    private Double amountSent;
    private Double exChangeRate;
    private Double amountReceived ;

    private String description;
    private LocalDateTime createdAt;
    private Map<String, String> image;
}
