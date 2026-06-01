package com.mybudget.server.controllers.account;


import com.mybudget.server.dto.Transaction;
import com.mybudget.server.services.account.AccountService;
import com.mybudget.server.services.account.RapportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user/accounts/search")
public class RapportController {
    private final RapportService searchService;
    private final AccountService accountService;

@GetMapping("/{fromDate}/{toDate}" )
    public ResponseEntity<?> getTransactionBetweenTwoDates(@PathVariable String fromDate, @PathVariable String toDate){
    List<Transaction> transactionList = accountService.getTransactionsBetweenTwoDates(fromDate, toDate);
    return ResponseEntity.status(HttpStatus.OK).body(transactionList);
}
}
