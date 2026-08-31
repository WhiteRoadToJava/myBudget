package com.mybudget.server.controllers.account;

import com.mybudget.server.dto.Transaction;
import com.mybudget.server.dto.accounts.AllAccounts;
import com.mybudget.server.dto.accounts.UpdateAccountStatus;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.enums.AccountStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mybudget.server.dto.accounts.AccountRequest;
import com.mybudget.server.dto.accounts.AccountResponse;
import com.mybudget.server.services.account.AccountService;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Set;

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
        List<Transaction> transactionList = accountService.getAllIncomseExpenseTransactins();
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
}
