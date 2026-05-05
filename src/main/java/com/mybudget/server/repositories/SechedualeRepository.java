package com.mybudget.server.repositories;

import com.mybudget.server.modules.Scheduale;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SechedualeRepository extends MongoRepository<Scheduale, String> {
    List<Scheduale> findByNextExecutionDateBetweenAndIsActiveTrue(LocalDateTime start, LocalDateTime end);
}
