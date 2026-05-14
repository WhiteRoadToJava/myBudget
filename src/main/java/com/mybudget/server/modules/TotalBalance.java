package com.mybudget.server.modules;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "totalBalance")
public class TotalBalance {
    @Id
    private String id;


    private Map<String, Double> totalBalanceByCurrency = new HashMap<>();

    private String user;

    @CreatedDate
    private LocalDateTime createdAt;
}