package com.mybudget.server.repositories;

import com.mybudget.server.modules.AccountGroup;
import com.mybudget.server.modules.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AccountGroupRepository extends MongoRepository<AccountGroup, String> {
    List<AccountGroup> findAllByUser(User user);

    AccountGroup findByIdAndUser(String id, User user);
}
