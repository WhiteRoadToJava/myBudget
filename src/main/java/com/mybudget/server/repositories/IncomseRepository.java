package com.mybudget.server.repositories;

import com.mybudget.server.modules.Incomse;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IncomseRepository extends MongoRepository<Incomse, String> {
}
