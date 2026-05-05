package com.mybudget.server.controllers;

import com.mybudget.server.dto.schedule.ScheduleRequest;
import com.mybudget.server.dto.schedule.ScheduleResponse;
import com.mybudget.server.services.ScheduleService;
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
    private final ScheduleService sechedualeService;


    @PostMapping("/excute")
    public ResponseEntity<ScheduleResponse> executeSchedule(@RequestBody ScheduleRequest request) {
        ScheduleResponse response = sechedualeService.executeSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
