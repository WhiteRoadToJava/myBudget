package com.mybudget.server.controllers.account;

import com.mybudget.server.dto.Transaction;
import com.mybudget.server.dto.accounts.AllAccounts;
import com.mybudget.server.dto.accounts.UpdateAccountStatus;
import com.mybudget.server.modules.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mybudget.server.dto.accounts.AccountRequest;
import com.mybudget.server.dto.accounts.AccountResponse;
import com.mybudget.server.dto.accounts.CurrencyRequest;
import com.mybudget.server.services.account.AccountService;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user/accounts")
public class AccountController {

    public final AccountService accountService;


    @PostMapping("/add-account")
    public ResponseEntity<?> createAccount(@Valid @RequestBody AccountRequest accountRequest) {
        AccountResponse response = accountService.createAccount(accountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
@PatchMapping("/update-account")
    public ResponseEntity<?> updateAccount(@RequestBody Account account){
        AccountResponse response = accountService.updateAccount(account);
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
        List<Transaction> transactionList = accountService.getAllIncomeExpenseTransactions();
        return ResponseEntity.status(HttpStatus.OK).body(transactionList);
    }

    @GetMapping("/all-transactionsbetweenTwoDates/{fromDate}/{toDate}")
    public ResponseEntity<?> getTransactionsBetweenTwoDates(@PathVariable String fromDate, @PathVariable String toDate){
        List<Transaction> transactionList = accountService.getTransactionsBetweenTwoDates(fromDate, toDate);
        return ResponseEntity.status(HttpStatus.OK).body(transactionList);
    }



    @PatchMapping("/{accountId}/update-status")
    public ResponseEntity<?> updateAccountStatus(@PathVariable String accountId, @RequestBody UpdateAccountStatus statusSet){
        AccountResponse response = accountService.updateAccountStatus(accountId, statusSet);
        return  ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @GetMapping("/user-all-transactions")
    public ResponseEntity<?> getAllUserTransactions(){
        List<Transaction> transactions = accountService.getUserAllTransaction();
        return  ResponseEntity.status(HttpStatus.OK).body(transactions);
    }

    // ---------- currencies of an account ----------

    // body: { "currency": "USD", "openingBalance": 50 }
    @PostMapping("/{accountId}/currencies")
    public ResponseEntity<AccountResponse> addCurrency(@PathVariable String accountId, @RequestBody CurrencyRequest request) {
        AccountResponse response = accountService.addCurrency(accountId, request.getCurrency(), request.getOpeningBalance());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // body: { "openingBalance": 100 }
    @PatchMapping("/{accountId}/currencies/{currency}")
    public ResponseEntity<AccountResponse> setOpeningBalance(@PathVariable String accountId, @PathVariable String currency,
                                                             @RequestBody CurrencyRequest request) {
        return ResponseEntity.ok(accountService.setOpeningBalance(accountId, currency, request.getOpeningBalance()));
    }

    // only a currency without any transaction, and never the last one (409 otherwise)
    @DeleteMapping("/{accountId}/currencies/{currency}")
    public ResponseEntity<AccountResponse> removeCurrency(@PathVariable String accountId, @PathVariable String currency) {
        return ResponseEntity.ok(accountService.removeCurrency(accountId, currency));
    }
}
