package com.mybudget.server.dto.schedule;

import com.fasterxml.jackson.annotation.JsonProperty;
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
        private String debtId;
        // a currency of each account; empty = the account's first currency
        private String sourceCurrency;
        private String destinationCurrency;
        private LocalDateTime nextExecutionDate ;
        @JsonProperty("isActive")
        private boolean isActive;
}