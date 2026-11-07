package com.mybudget.server.dto.groups;

import lombok.Data;

// a new basket with its first currency
@Data
public class CreateGroupRequest {
    private String name;
    private String type;
    private String currency;
    private Double openingBalance;
}
