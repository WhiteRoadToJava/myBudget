package com.mybudget.server.dto.schedule;

import com.mybudget.server.modules.enums.ScheduleInterval;
import com.mybudget.server.modules.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleResponse {
private String id;
private String name;
private String description;
private AccountSummary sourceAccount;
private AccountSummary destinationAccount;
private TransactionType transactionType;
private ScheduleInterval scheduleInterval;
private BigDecimal amountSend;
private BigDecimal exChangeRate;
private BigDecimal amountReceived;
private LocalDateTime executionDate;
private boolean active;
private UserSummary createdBy;
private LocalDateTime createdAt;
private LocalDateTime updatedAt;

@Data
public static class AccountSummary {
    private String id;
    private String name;
    private Double totalBalance;
}

@Data
public static class UserSummary {
    private String id;
    private String name;
    private String email;
}
}