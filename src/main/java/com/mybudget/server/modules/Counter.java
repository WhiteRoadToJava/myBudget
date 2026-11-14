package com.mybudget.server.modules;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@Document(collection = "counters")
public class Counter {
    @Id
    private String id;
    private long seq;
}
