package com.mybudget.server.dto.incomse;

import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;


@Getter @Setter
@AllArgsConstructor
public class IncomseResponse {
    private String id;
    private double amount;
    private String category;
    private User user;
    private Account account;
    private String type = "incomse";

}