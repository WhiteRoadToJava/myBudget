package com.mybudget.server.modules;

import com.mybudget.server.modules.enums.DebtDirection;
import com.mybudget.server.modules.enums.InterestCompounding;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "debts")
public class Debt {
    @Id
    private String id;
    @DBRef
    private User user;

    private String name;
    private String counterparty;
    private DebtDirection direction;
    private String current;
    private double annualRate;
    private InterestCompounding compounding = InterestCompounding.NONE;
    private LocalDate startDate;

    private boolean countInflowAsIncome;
    private boolean countOutflowAsExpense;

    private String note;
    @CreatedDate
    private LocalDateTime createdAt;
}
