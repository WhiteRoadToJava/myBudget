package com.mybudget.server.dto.accounts;

import com.mybudget.server.modules.User;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountRequest {
    @NotNull(message = "Name is required")
    private String name;
    @NotNull(message = "Currency is required")
    private String currency;
    @NotNull(message = "Type is required")
    private String type;
    private String description;
    @NotNull(message = "Balance is required")
    private Double balance;
    private User user;
}