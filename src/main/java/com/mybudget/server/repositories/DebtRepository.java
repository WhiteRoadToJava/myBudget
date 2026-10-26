package com.mybudget.server.repositories;

import com.mybudget.server.modules.Debt;
import com.mybudget.server.modules.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DebtRepository extends MongoRepository<Debt, String> {
    List<Debt> findByUserOrderByStartDateDesc(User user);

    Optional<Debt> findByIdAndUser(String id, User user);
}
