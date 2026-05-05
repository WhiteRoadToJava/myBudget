package com.mybudget.server.dto.scheduale;

import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.enums.ScheduleInterval;
import com.mybudget.server.modules.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SechedualeRequest {

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