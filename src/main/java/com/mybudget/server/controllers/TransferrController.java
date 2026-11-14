package com.mybudget.server.controllers;

import com.mybudget.server.dto.common.CancelRequest;
import com.mybudget.server.dto.transfer.TransferRequest;
import com.mybudget.server.dto.transfer.TransferResponse;
import com.mybudget.server.services.transactions.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user/transfer")
public class TransferrController {
    private final TransferService transferService;

    @PostMapping("/execute")
public ResponseEntity<?> executeTranfer (@RequestBody TransferRequest request){
        TransferResponse response =  transferService.executeTransfer(request);
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

    // body (optional): { "reason": "..." }; the fees are given back too
    @PatchMapping("/cancel/{transferId}")
    public ResponseEntity<TransferResponse> cancelTransfer(@PathVariable String transferId,
                                                           @RequestBody(required = false) CancelRequest request) {
        return ResponseEntity.ok(transferService.cancelTransfer(transferId, request == null ? null : request.getReason()));
    }

    @PatchMapping("/restore/{transferId}")
    public ResponseEntity<TransferResponse> restoreTransfer(@PathVariable String transferId) {
        return ResponseEntity.ok(transferService.restoreTransfer(transferId));
    }

}
