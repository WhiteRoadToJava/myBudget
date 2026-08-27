package com.mybudget.server.repositories;

import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Income;
import com.mybudget.server.modules.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface IncomeRepository extends MongoRepository<Income, String> {

    List<Income> findAllByAccount(Account account);
    List<Income> findAllByUser(User user);
    void deleteAllByAccount(Account account);

    List<Income> findByUserAndCreatedAtBetween(User user, LocalDateTime fromDate, LocalDateTime toDate);

}
