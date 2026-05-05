package com.mybudget.server.dto.schedule;

import com.mybudget.server.modules.enums.ScheduleInterval;
import com.mybudget.server.modules.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleRequest {

        private  String name;
        private String description;;
        private String sourceAccountId;
        private  String destinationAccountId;
        private String category;
        private Set<TransactionType> transactionTypes;
        private  Set<ScheduleInterval> scheduleIntervals;
        private BigDecimal amountSend;
        private BigDecimal exChangeRate;
        private BigDecimal amountReceived;
        private LocalDateTime nextExecutionDate ;
        private boolean isActive;
}