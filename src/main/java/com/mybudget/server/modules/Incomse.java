package com.mybudget.server.modules;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

@Getter @Setter
@NoArgsConstructor
@Document(collection = "incomes")
public class Incomse {
    @Id
    private String id;
    private double amount;
    private String category;
    private String description;
    private Long number;     // the reference number shown to people and in the statement (ReferenceNumbers)
    // multi-currency (filled by scripts/migration; the services switch to it in the next phases)
    private String currency;
    @DBRef
    private User user;
    @DBRef
    private Account account;
    @CreatedDate
    private LocalDateTime createdAt;
    private Boolean cancelled;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private String type = "incomse";
    private Map<String, String> image;
}
