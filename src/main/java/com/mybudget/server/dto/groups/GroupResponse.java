package com.mybudget.server.dto.groups;

import com.mybudget.server.dto.accounts.AccountResponse;

import java.util.List;

// One basket with its accounts (one per currency).
// single = an account without a basket, shown as a basket of one currency; id is then null.
public record GroupResponse(
        String id,
        String name,
        String type,
        boolean single,
        List<AccountResponse> accounts
) {
}
