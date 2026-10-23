package com.mybudget.server.dto.debt;

import java.time.LocalDate;

public record DebtSummeryResponse (
    double principal,
    double accruedInterest,
    double totalOwed,
    double totalDisbursed,
    double totalRepaid,
    double interestPaid,
    double principalPaid,
    LocalDate asOf
    ){
}
