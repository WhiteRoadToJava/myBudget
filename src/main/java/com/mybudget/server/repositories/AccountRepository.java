package com.mybudget.server.repositories;

import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends MongoRepository<Account ,  String> {
    Optional<Account> findByNameAndUser(String name, User user);
    List<Account> findAllByUser(User user);
}
