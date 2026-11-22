package com.mybudget.server.repositories;

import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Schedule;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.enums.TransactionType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends MongoRepository<Schedule, String> {
    List<Schedule> findByNextExecutionDateLessThanEqualAndIsActiveTrue(LocalDateTime end);

    List<Schedule> findAllByUser(String userId);

    boolean existsByUserAndCategoryAndTransactionTypesContaining(User user, String category, TransactionType type);

    void deleteAllBySourceAccountOrDestinationAccount(Account sourceAccount, Account destinationAccount);

}
