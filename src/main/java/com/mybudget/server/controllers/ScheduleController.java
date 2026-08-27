package com.mybudget.server.controllers;

import com.mybudget.server.dto.schedule.ScheduleRequest;
import com.mybudget.server.dto.schedule.ScheduleResponse;
import com.mybudget.server.dto.schedule.ScheduleUpdateRequest;
import com.mybudget.server.services.schedule.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user/schedules")
public class ScheduleController {
    private final ScheduleService scheduleService;


    @PostMapping("/execute")
    public ResponseEntity<ScheduleResponse> executeSchedule(@RequestBody ScheduleRequest request) {
        ScheduleResponse response = scheduleService.executeSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/all-schedules")
    public ResponseEntity<List<ScheduleResponse>> getAllSchedules() {
        List<ScheduleResponse> response  = scheduleService.getAllSchedules();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("delete-schedule/{scheduleId}")
        public ResponseEntity<?> delete(@PathVariable String scheduleId ){
        scheduleService.deleteSchedule(scheduleId);
        return  ResponseEntity.status(HttpStatus.OK).body("delete is successfuly");
    }

    @PatchMapping("/{scheduleId}/update-schedule")
    public ResponseEntity<?> updateSchedule(@PathVariable String scheduleId, @RequestBody ScheduleUpdateRequest request){
        ScheduleResponse response = scheduleService.updateSchedule(scheduleId, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
