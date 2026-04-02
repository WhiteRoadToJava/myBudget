package com.mybudget.server.modules;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "transfers")
public class Transfer {
    @Id
    private String id;
    @DBRef
    private Account sourceAccount;
    @DBRef
    private Account destinationAccount;
    @DBRef
    private User user;
    private Double amountSent;
    private double amountReceived ;

    private String description;
    @CreatedDate
    private LocalDateTime createdAt;
    private String currency;
    private String type = "transfer";
    private double exChangeRate;

}
