package com.mybudget.server.repositories;

import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Expense;
import com.mybudget.server.modules.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ExpenseRepository extends MongoRepository<Expense, String> {
    List<Expense> findAllByUser(User user);
    List<Expense> findAllByAccount(Account account);
}

