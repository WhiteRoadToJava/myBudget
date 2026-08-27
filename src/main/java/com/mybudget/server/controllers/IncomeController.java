package com.mybudget.server.controllers;


import com.mybudget.server.dto.income.IncomeRequest;
import com.mybudget.server.dto.income.IncomeResponse;
import com.mybudget.server.modules.Account;
import com.mybudget.server.services.IncomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user/income")
public class IncomeController {
    private final IncomeService incomeService;


    @PostMapping("/add-income")
    public ResponseEntity<?> addIncome(@RequestBody IncomeRequest incomeRequest){
        IncomeResponse response = incomeService.addIncome(incomeRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @PatchMapping("/update-income/{incomeId}")
    public ResponseEntity<?> updateIncome(@RequestBody IncomeRequest incomeRequest, @PathVariable String incomeId)   {
        IncomeResponse response = incomeService.updateIncome(incomeId, incomeRequest);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    @DeleteMapping("/delete-income/{incomeId}")
    public ResponseEntity<?> deleteIncome(@PathVariable String incomeId){
        String response = incomeService.deleteIncome(incomeId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }







    @GetMapping("/find-income/{incomeId}")
    public ResponseEntity<?> getIncomeById(@PathVariable String incomeId){
        IncomeResponse response = incomeService.getIncomeById(incomeId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    @GetMapping("/account")
    public ResponseEntity<?> getAllIncomeAccount(@RequestBody Account account){
        List<IncomeResponse> list = incomeService.getAllIncomeByAccount(account);
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }


    @GetMapping("/user")
    public ResponseEntity<?> getAllIncomeByUser(){
        List<IncomeResponse> list = incomeService.getAllIncomeByUser();
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }
}
