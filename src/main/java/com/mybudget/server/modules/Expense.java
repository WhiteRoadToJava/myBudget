package com.mybudget.server.modules;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "expenses")
public class Expense {
    @Id
    private String id;
    private double amount;
    private String category;
    @CreatedDate
    private LocalDateTime createdAt;
    @DBRef
    private User user;
    @DBRef
    private Account account;
    private Map<String, String> image;
    private String type= "expense";
}
