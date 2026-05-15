package com.mybudget.server.repositories;

import com.mybudget.server.modules.Schedule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends MongoRepository<Schedule, String> {
    List<Schedule> findByNextExecutionDateBetweenAndIsActiveTrue(LocalDateTime start, LocalDateTime end);
    List<Schedule> findAllByUser(String userId);
}
