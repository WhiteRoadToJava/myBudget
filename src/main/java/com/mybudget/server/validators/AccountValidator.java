package com.mybudget.server.validators;

import com.mybudget.server.dto.accounts.AccountRequest;


public class AccountValidator {

    public boolean validateAccount(AccountRequest q){
        boolean valid = false;
        if(q.getName().isEmpty() || (q.getName() == null)){
            throw new IllegalArgumentException("The account's name cannot be empty");
        }
        if(q.getCurrency().isEmpty() || (q.getCurrency() == null)){
            throw new IllegalArgumentException("The account's currency cannot be empty");
        }
        if(q.getType().isEmpty() || (q.getType() == null)){
            throw new IllegalArgumentException("The account's type cannot be empty");
        }
        if(q.getDescription().isEmpty() || (q.getDescription() == null)){
            throw new IllegalArgumentException("The account's description cannot be empty");
        }
        if(q.getBalance() == null){
            throw new IllegalArgumentException("The account's balance cannot be empty");
        }
        if(q.getUser() == null){
            throw new IllegalArgumentException("The account's user cannot be empty");
        }

        return valid = true;
    }

}
