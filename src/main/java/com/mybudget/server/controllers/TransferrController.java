package com.mybudget.server.controllers;

import com.mybudget.server.dto.transfer.TransferRequest;
import com.mybudget.server.dto.transfer.TransferResponse;
import com.mybudget.server.services.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user/transfer")
public class TransferrController {
    private final TransferService transferService;

    @PostMapping("/execute")
public ResponseEntity<?> executeTranfer (@RequestBody TransferRequest request){
        TransferResponse response =  transferService.executeTransfet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
