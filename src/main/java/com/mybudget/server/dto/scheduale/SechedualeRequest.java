package com.mybudget.server.dto.scheduale;

import com.mybudget.server.modules.enums.ScheduleInterval;
import com.mybudget.server.modules.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class SechedualeRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotBlank(message = "Source account is required")
    private String sourceAccountId;

    private String destinationAccountId;   // optional, depends on transaction type

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @NotNull(message = "Schedule interval is required")
    private ScheduleInterval scheduleInterval;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Execution date is required")
    private LocalDateTime executionDate;


}