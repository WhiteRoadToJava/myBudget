package com.mybudget.server.modules;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;

public class Transfer {
    @Id
    private String id;
    @DBRef
    private String fromAccountId;
    @DBRef
    private String toAccountId;
    private Double amount;
    private String description;
}
