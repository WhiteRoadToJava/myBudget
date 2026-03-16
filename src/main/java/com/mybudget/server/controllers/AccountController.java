package com.mybudget.server.controllers;

import com.mybudget.server.dto.accounts.AllAccounts;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mybudget.server.dto.accounts.AccountRequest;
import com.mybudget.server.dto.accounts.AccountResponse;
import com.mybudget.server.services.AccountService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/user/accounts")
public class AccountController {

    public final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody AccountRequest accountRequest) {
        AccountResponse response = accountService.createAccount(accountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping
    public ResponseEntity<AllAccounts> getAllAccounts() {
        AllAccounts response = accountService.getAllAccounts();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}