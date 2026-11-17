package com.mybudget.server.dto.exchange;


import lombok.Data;

@Data
public class ExchangeRateRequest {
    private String from;
    private String to;
    private Double rate;
}
