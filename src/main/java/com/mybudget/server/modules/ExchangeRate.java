package com.mybudget.server.modules;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document(collection = "exchange_rate")
@CompoundIndex(name = "user_pair", def = "{'userId': 1, 'fromCurrency': 1, 'toCurrency': 1}", unique = true)
public class ExchangeRate {
    @Id
    private String id;
    private String userId;
    private String fromCurrency;
    private String toCurrency;
    private double rate;
    private LocalDateTime updatedAt;
    private Long transferNumber;
}
