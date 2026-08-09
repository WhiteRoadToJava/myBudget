package com.mybudget.server.dto.schedule;

import com.mybudget.server.modules.enums.ScheduleInterval;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleuUpdateRequest {
    private String name;
    private String description;
    private String Category;
    private Set<ScheduleInterval> scheduleIntervalSet;
    private BigDecimal amountSend;
    private BigDecimal exChangeRate;
    private BigDecimal amountReceived;
    private LocalDateTime nextExecutionDate;
    private boolean isActive;
}

