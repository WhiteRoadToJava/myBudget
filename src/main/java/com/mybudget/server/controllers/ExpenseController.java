package com.mybudget.server.controllers;

import com.mybudget.server.dto.expense.ExpenseRequset;
import com.mybudget.server.dto.expense.ExpenseResponse;
import com.mybudget.server.modules.Account;
import com.mybudget.server.services.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/expense")
@RequiredArgsConstructor
public class ExpenseController {
    private  final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<?> addExpense(@RequestBody ExpenseRequset expenseRequest){
        ExpenseResponse response = expenseService.addExpense(expenseRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/user")
    public ResponseEntity<?> getAllExpensesByUser(){
        List<ExpenseResponse> list = expenseService.getAllExpensesByUser();
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }

    @GetMapping("/account")
    public ResponseEntity<?> getAllExpensesByAccount(@RequestBody Account account){
        List<ExpenseResponse> list = expenseService.getAllExpensesByAccount(account);
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }
}
