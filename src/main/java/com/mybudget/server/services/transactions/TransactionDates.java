package com.mybudget.server.services.transactions;

import java.time.LocalDate;
import java.time.LocalDateTime;

final class TransactionDates {
 private TransactionDates(){
 }



 static void ensureNtFuture(LocalDateTime date){
     if (date != null && date.toLocalDate().isAfter(LocalDate.now())){
         throw new IllegalArgumentException("The date can not be in the future");
     }

 }
}
