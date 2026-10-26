package com.mybudget.server.repositories;

import com.mybudget.server.modules.Debt;
import com.mybudget.server.modules.DebtMovement;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DebtMovementRepository extends MongoRepository<DebtMovement, String> {
    List<DebtMovement> findByDebtOrderByDateAscCreatedAtAsc(Debt debt);

    Optional<DebtMovement> findByIdAndDebt(String id, Debt debt);

    void deleteByDebt(Debt debt);
}
