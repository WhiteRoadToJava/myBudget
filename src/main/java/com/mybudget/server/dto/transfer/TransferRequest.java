package com.mybudget.server.dto.transfer;

import com.mybudget.server.modules.User;

import java.util.Date;

public class TransferRequest {
    private String fromAccountId;
    private String toAccountId;
    private Double amount;
    private String description;
    private Date createdAt;
    private User user;
}
