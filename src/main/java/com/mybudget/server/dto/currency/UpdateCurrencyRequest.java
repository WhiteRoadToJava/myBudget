package com.mybudget.server.dto.currency;


import lombok.Data;




// any of { "enabled": false, "payment": true }; a missing field is not changed
@Data
public class UpdateCurrencyRequest {
    private Boolean enabled;
    private Boolean payment;
}