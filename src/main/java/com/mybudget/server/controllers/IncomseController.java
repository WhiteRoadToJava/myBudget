package com.mybudget.server.controllers;


import com.mybudget.server.dto.incomse.IncomseRequset;
import com.mybudget.server.dto.incomse.IncomseResponse;
import com.mybudget.server.modules.Account;
import com.mybudget.server.dto.common.CancelRequest;
import com.mybudget.server.services.transactions.IncomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/user/incomse")
public class IncomseController {
    private final IncomeService incomeService;


    @PostMapping("/add-incomse")
    public ResponseEntity<?> addIncomse(@RequestBody IncomseRequset incomseRequest){
        IncomseResponse response = incomeService.addInccme(incomseRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @PatchMapping("/update-incomse/{incomseId}")
    public ResponseEntity<?> updateIncomse(@RequestBody IncomseRequset incomseRequest, @PathVariable String incomseId)   {
        IncomseResponse response = incomeService.updateIncome(incomseId, incomseRequest);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    @DeleteMapping("/delete-incomse/{incomseId}")
    public ResponseEntity<?> deleteIncomse(@PathVariable String incomseId){
        String response = incomeService.deleteIncome(incomseId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/cancel/{incomeId}")
    public ResponseEntity<IncomseResponse> cancelIncome(@PathVariable String incomeId,
                                                        @RequestBody(required = false) CancelRequest request){
        return ResponseEntity.ok(incomeService.cancelIncome(incomeId,
                request == null ? null : request.getReason(),
                request == null ? null : request.getCancelledAt()));
    }

    @PatchMapping("/restore/{incomeId}")
    public ResponseEntity<IncomseResponse> restoreIncome(@PathVariable String incomeId){
        return ResponseEntity.ok(incomeService.restoreIncome(incomeId));
    }







    @GetMapping("/find-incomse/{incomseId}")
    public ResponseEntity<?> getIncomseById(@PathVariable String incomseId){
        IncomseResponse response = incomeService.getIncomeById(incomseId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    @GetMapping("/account")
    public ResponseEntity<?> getAllIncomseAccount(@RequestBody Account account){
        List<IncomseResponse> list = incomeService.getAllIncomseByAccount(account);
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }


    @GetMapping("/user")
    public ResponseEntity<?> getAllIncomseByUser(){
        List<IncomseResponse> list = incomeService.getAllIncomseByUser();
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }
}
