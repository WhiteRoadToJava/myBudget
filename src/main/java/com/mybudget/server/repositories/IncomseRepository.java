package com.mybudget.server.repositories;

import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Incomse;
import com.mybudget.server.modules.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface IncomseRepository extends MongoRepository<Incomse, String> {

    List<Incomse> findAllByAccount(Account account);
    List<Incomse> findAllByUser(User user);
}
