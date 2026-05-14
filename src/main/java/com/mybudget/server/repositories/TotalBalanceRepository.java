package com.mybudget.server.repositories;

import com.mybudget.server.modules.TotalBalance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TotalBalanceRepository extends MongoRepository<TotalBalance, String> {
    List<TotalBalance> findAllByUser(String userId);
}
