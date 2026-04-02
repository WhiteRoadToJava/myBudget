package com.mybudget.server.dto.transfer;

import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.User;
import com.mybudget.server.services.AccountService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferRequest {
    private Account sourceAccount;
    private Account destinationAccount;

    private Double amountSent;
    private double exChangeRate;
    private double amountReceived ;

    private String description;
}
