package com.mybudget.server.modules;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Collection;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "currencies")
public class SupportedCurrency {
    @Id
    private String code;
    private boolean enabled;
    private boolean payment;
    private int position;
    private Instant addedAt;


    public SupportedCurrency(String code, boolean enabled, boolean payment, int position) {
        this.code = code;
        this.enabled = enabled;
        this.payment = payment;
        this.position = position;
        this.addedAt = Instant.now();
    }
}
