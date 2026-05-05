package com.mybudget.server.controllers;

import com.mybudget.server.dto.scheduale.SechedualeRequest;
import com.mybudget.server.dto.scheduale.SechedualeResponse;
import com.mybudget.server.services.SechedualeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user/schedules")
public class SechedualeController {
    private final SechedualeService sechedualeService;


    @PostMapping("/excute")
    public ResponseEntity<SechedualeResponse> executeSchedule(@RequestBody SechedualeRequest request) {
        SechedualeResponse response = sechedualeService.executeSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
