package com.mybudget.server.controllers;

import com.mybudget.server.dto.transfer.TransferRequest;
import com.mybudget.server.dto.transfer.TransferResponse;
import com.mybudget.server.services.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user/transfer")
public class TransferrController {
    private final TransferService transferService;

    @PostMapping("/execute")
public ResponseEntity<?> executeTranfer (@RequestBody TransferRequest request){
        TransferResponse response =  transferService.excuteTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/update/{transferId}")
    public ResponseEntity<?> updateTransfer(@PathVariable String transferId, @RequestBody TransferRequest request){
        TransferResponse response = transferService.updateTransfer(transferId, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    @GetMapping("/find-transfer/{transferId}")
    public ResponseEntity<?> getTransferById(@PathVariable String transferId){
        TransferResponse response = transferService.getTransferById(transferId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    @DeleteMapping("/delete-transfer/{transferId}")
    public ResponseEntity<?> deleteTransfer(@PathVariable String transferId){
        String response = transferService.deleteTransfer(transferId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
