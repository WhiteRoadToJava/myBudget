package com.mybudget.server.dto.exchange;


import java.time.LocalDateTime;


public record ExchangeRateResponse(
        String id,
        String from,
        String to,
        double rate,
        LocalDateTime updatedAt,
        Long transferNumber,
        boolean inverted
){}
