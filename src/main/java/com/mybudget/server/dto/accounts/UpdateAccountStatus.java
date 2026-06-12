package com.mybudget.server.dto.accounts;

import com.mybudget.server.modules.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateAccountStatus {
    private Set<AccountStatus> status;
}
