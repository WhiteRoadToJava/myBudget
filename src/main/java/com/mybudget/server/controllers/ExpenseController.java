package com.mybudget.server.controllers;

import com.mybudget.server.dto.common.CancelRequest;
import com.mybudget.server.dto.expense.ExpenseRequest;
import com.mybudget.server.dto.expense.ExpenseResponse;
import com.mybudget.server.modules.Account;
import com.mybudget.server.services.transactions.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/expense")
@RequiredArgsConstructor
public class ExpenseController {
    private  final ExpenseService expenseService;

    @PostMapping("/add-expense")
    public ResponseEntity<?> addExpense(@RequestBody ExpenseRequest expenseRequest){
        ExpenseResponse response = expenseService.addExpense(expenseRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/user")
    public ResponseEntity<?> getAllExpensesByUser(){
        List<ExpenseResponse> list = expenseService.getAllExpensesByUser();
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }

    @PatchMapping("/update-expense/{expenseId}")
    public ResponseEntity<?> updateExpense(@RequestBody ExpenseRequest expenseRequest, @PathVariable String expenseId){
        ExpenseResponse response = expenseService.updateExpense(expenseId, expenseRequest);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/delete-expense/{expenseId}")
    public ResponseEntity<?> deleteExpense(@PathVariable String expenseId){
        String response = expenseService.deleteExpense(expenseId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    // body (optional): { "reason": "..." }
    @PatchMapping("/cancel/{expenseId}")
    public ResponseEntity<ExpenseResponse> cancelExpense(@PathVariable String expenseId,
                                                         @RequestBody(required = false) CancelRequest request) {
        return ResponseEntity.ok(expenseService.cancelExpense(expenseId, request == null ? null : request.getReason()));
    }

    @PatchMapping("/restore/{expenseId}")
    public ResponseEntity<ExpenseResponse> restoreExpense(@PathVariable String expenseId) {
        return ResponseEntity.ok(expenseService.restoreExpense(expenseId));
    }

    @GetMapping("/account")
    public ResponseEntity<?> getAllExpensesByAccount(@RequestBody Account account){
        List<ExpenseResponse> list = expenseService.getAllExpensesByAccount(account);
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }
    @GetMapping("/find-expense/{expenseId}")
    public ResponseEntity<?> getExpenseById(@PathVariable String expenseId){
        ExpenseResponse response = expenseService.getExpenseById(expenseId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
