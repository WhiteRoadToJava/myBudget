package com.mybudget.server.controllers;


import com.mybudget.server.dto.debt.DebtDetailsResponse;
import com.mybudget.server.dto.debt.DebtMovementRequest;
import com.mybudget.server.dto.debt.DebtRequest;
import com.mybudget.server.dto.debt.DebtResponse;
import com.mybudget.server.services.debt.DebtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/debts")
@RequiredArgsConstructor
public class DebtController {
    private final DebtService debtService;

    @PostMapping("/add-debt")
    public ResponseEntity<?> addDebt(@RequestBody DebtRequest request){
        DebtResponse response = debtService.createDebt(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/all-debts")
    public ResponseEntity<?> getAllDebts(){
        List<DebtResponse> list = debtService.getAllDebts();
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }


    @GetMapping("/find-debt/{debtId}")
    public ResponseEntity<?> getDebt(@PathVariable String debtId){
        DebtDetailsResponse response = debtService.getDebt(debtId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
@PatchMapping("/update-debt/{debtId}")
    public ResponseEntity<?> updateDept(@PathVariable String debtId, @RequestBody DebtRequest request){
        DebtResponse response = debtService.updateDebt(debtId, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
}

@DeleteMapping("/delete-debt/{debtId}")
    public ResponseEntity<?> deleteDebt(@PathVariable String debtId){
        String response = debtService.deleteDebt(debtId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
}

@PostMapping("/{debtId}/add-movement")
    public ResponseEntity<?> addMovement(@PathVariable String debtId, @RequestBody DebtMovementRequest request){
    DebtDetailsResponse response = debtService.addMovement(debtId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}

@DeleteMapping("/{debtId}/delete-movement/{movementId}")
    public ResponseEntity<?> deleteMovement(@PathVariable String debtId, @PathVariable String movementId){
        DebtDetailsResponse response = debtService.deleteMovement(debtId, movementId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
}

}
