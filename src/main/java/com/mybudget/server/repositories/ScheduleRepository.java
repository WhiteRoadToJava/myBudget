package com.mybudget.server.repositories;

import com.mybudget.server.modules.Scheduae;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends MongoRepository<Scheduae, String> {
    List<Scheduae> findByNextExecutionDateBetweenAndIsActiveTrue(LocalDateTime start, LocalDateTime end);
}
