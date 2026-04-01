package com.mybudget.server.modules;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;

public class Transfer {
    @Id
    private String id;
    @DBRef
    private Account fromAccountId;
    @DBRef
    private Account toAccountId;
    private Double amount;
    private String description;
    @CreatedDate
    private String createdAt;
}
