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
@Getter @Setter
@NoArgsConstructor
@Document(collection = "incomes")
public class Incomse {
    @Id
    private String id;
    private double amount;
    private String category;
    @DBRef
    private User user;
    @DBRef
    private Account account;
    @CreatedDate
    private LocalDateTime createdAt;
    private String type = "incomse";
}
