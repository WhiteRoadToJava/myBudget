package com.mybudget.server.repositories;

import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Expense;
import com.mybudget.server.modules.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseRepository extends MongoRepository<Expense, String> {
    List<Expense> findAllByUser(User user);
    List<Expense> findAllByAccount(Account account);
    void deleteAllByAccount(Account account);

    List<Expense> findByUserAndCreatedAtBetween(User user, LocalDateTime date1, LocalDateTime date2);
}

