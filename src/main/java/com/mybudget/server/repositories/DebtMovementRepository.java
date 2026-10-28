package com.mybudget.server.repositories;

import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Debt;
import com.mybudget.server.modules.DebtMovement;
import com.mybudget.server.modules.User;
import org.springframework.data.domain.Range;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DebtMovementRepository extends MongoRepository<DebtMovement, String> {
    List<DebtMovement> findByDebtOrderByDateAscCreatedAtAsc(Debt debt);

    Optional<DebtMovement> findByIdAndDebt(String id, Debt debt);

    void deleteByDebt(Debt debt);

    boolean existsByAccount(Account account);

    List<DebtMovement> findAllByAccountAndMirroredTransactionIdIsNull(Account account);

    List<DebtMovement> findAllByUserAndMirroredTransactionIdIsNull(User user);

    List<DebtMovement> findAllByUserAndDateBetweenAndMirroredTransactionIdIsNull(
            User user, Range<LocalDate> dates);
}
