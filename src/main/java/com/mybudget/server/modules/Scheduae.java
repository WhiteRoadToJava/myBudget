package com.mybudget.server.modules;


import com.mybudget.server.modules.enums.ScheduleInterval;
import com.mybudget.server.modules.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "schedules")
public class Scheduae {
    @Id
    private  String id;
    private  String name;
    private String description;;
    @DBRef
    private  Account sourceAccount;
    @DBRef
    private  Account destinationAccount;
    @DBRef
    private  User user;
    private String category;
    private Set<TransactionType> transactionTypes;
    private  Set<ScheduleInterval> scheduleIntervals;
    private BigDecimal amountSend;
    private BigDecimal exChangeRate;
    private BigDecimal amountReceived;
    private LocalDateTime nextExecutionDate ;
    private boolean isActive;
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
