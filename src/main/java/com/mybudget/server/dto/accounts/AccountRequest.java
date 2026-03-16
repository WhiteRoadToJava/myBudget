package com.mybudget.server.dto.accounts;

import com.mybudget.server.modules.User;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountRequest {
    private String name;
    private String currency;
    private String type;
    private String description;
    private Double balance;
    private User user;
}