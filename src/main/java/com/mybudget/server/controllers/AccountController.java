package com.mybudget.server.controllers;

import com.mybudget.server.dto.Transaction;
import com.mybudget.server.dto.accounts.AllAccounts;
import com.mybudget.server.modules.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mybudget.server.dto.accounts.AccountRequest;
import com.mybudget.server.dto.accounts.AccountResponse;
import com.mybudget.server.services.AccountService;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user/accounts")
public class AccountController {

    public final AccountService accountService;


    @PostMapping("/add-account")
    public ResponseEntity<?> createAccount(@Valid @RequestBody AccountRequest accountRequest) {
        AccountResponse response = accountService.createAccount(accountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
@PatchMapping("/update-account")
    public ResponseEntity<?> updateAccount(@RequestBody Account account){
        AccountResponse response = accountService.updateAcoount(account);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    @DeleteMapping("/delete-account/{accountId}")
    public ResponseEntity<?> deleteAccount(@PathVariable String accountId) {
         accountService.deleteAccount(accountId);
        return ResponseEntity.status(HttpStatus.OK).body("Account deleted successfully");
    }

    @GetMapping("/all-accounts")
    public ResponseEntity<?> getAllAccounts() {
        AllAccounts response = accountService.getAllAccounts();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    @GetMapping("/{accountId}")
    public ResponseEntity<?> getAccountById(@PathVariable String accountId){
        AccountResponse response = accountService.getAccountById(accountId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/allaccount-transactions")
    public ResponseEntity<?> getAllaccountTransations(@RequestBody Account account){
        List<Transaction> response = accountService.getAllAccountTransactions(account);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    @GetMapping("/all-incomse-and-expense-transactions")
    public ResponseEntity<?> getAllTransations(){
        List<Transaction> transactionList = accountService.getAllTransactions();
        return ResponseEntity.status(HttpStatus.OK).body(transactionList);
    }
}
