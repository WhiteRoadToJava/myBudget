package com.mybudget.server.controllers;


import com.mybudget.server.modules.TotalBalance;
import com.mybudget.server.services.TotalBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/user/total-balance")
public class TotalBalanceController {
    private final TotalBalanceService totalBalanceService;



@GetMapping("/all-total-balance")
    public ResponseEntity<?> getAllTotalBalanceByUser() {
    List<TotalBalance> totalBalanceList = totalBalanceService.getAllTotalBalanceByUser();
        return ResponseEntity.status(HttpStatus.OK).body(totalBalanceList);
    }






}
