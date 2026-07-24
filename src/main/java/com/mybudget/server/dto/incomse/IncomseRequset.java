package com.mybudget.server.dto.incomse;

import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class IncomseRequset {
    private double amount;
    private String category;
    private User user;
    private Account account;
    private  String description;
    private Map<String, String> image;
}
