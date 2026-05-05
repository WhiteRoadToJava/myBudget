package com.mybudget.server.repositories;

import com.mybudget.server.modules.Secheduale;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchedualeRepository extends MongoRepository<Secheduale, String> {
}
