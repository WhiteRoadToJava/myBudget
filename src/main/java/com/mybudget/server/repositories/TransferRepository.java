package com.mybudget.server.repositories;

import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Transfer;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TransferRepository extends MongoRepository<Transfer, String> {
    List<Transfer> findAllBySourceAccountOrDestinationAccount(Account fromAccount, Account toAccount);
}
