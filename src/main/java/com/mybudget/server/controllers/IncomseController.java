package com.mybudget.server.controllers;


import com.mybudget.server.dto.incomse.IncomseRequset;
import com.mybudget.server.dto.incomse.IncomseResponse;
import com.mybudget.server.modules.Account;
import com.mybudget.server.services.IncomseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user/incomse")
public class IncomseController {
    private final IncomseService incomseService;


    @PostMapping("/add-incomse")
    public ResponseEntity<?> addIncomse(@RequestBody IncomseRequset incomseRequest){
        IncomseResponse response = incomseService.addInccmse(incomseRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @PatchMapping("/update-incomse/{incomseId}")
    public ResponseEntity<?> updateIncomse(@RequestBody IncomseRequset incomseRequest, @PathVariable String incomseId)   {
        IncomseResponse response = incomseService.updateIncomse(incomseId, incomseRequest);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }







    @GetMapping("/find-incomse/{incomseId}")
    public ResponseEntity<?> getIncomseById(@PathVariable String incomseId){
        IncomseResponse response = incomseService.getIncomseById(incomseId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    @GetMapping("/account")
    public ResponseEntity<?> getAllIncomseAccount(@RequestBody Account account){
        List<IncomseResponse> list = incomseService.getAllIncomseByAccount(account);
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }


    @GetMapping("/user")
    public ResponseEntity<?> getAllIncomseByUser(){
        List<IncomseResponse> list = incomseService.getAllIncomseByUser();
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }
}
