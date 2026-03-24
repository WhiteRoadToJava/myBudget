package com.mybudget.server.dto.incomse;

import com.mybudget.server.modules.User;

import java.util.Date;

public class IncomseRequset {
    private  String name;
    private double amount;
    private String category;
    private Date createdAt;
    private User user;
}
