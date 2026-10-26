package com.mybudget.server.modules;

import com.mybudget.server.modules.enums.DebtMovementType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Document
public class DebtMovement {
    @Id
    private String id;
    @DBRef
    private Debt debt;
    @DBRef
    private User user;
    @DBRef
    private Account account;

    private DebtMovementType movementType;
    private double amount;
    private LocalDate date;
    private String description;
    private Map<String, String> image;

    private String mirroredTransactionId;
    private String mirroredTransactionType;

    @CreatedDate
    private LocalDateTime createdAt;
}
