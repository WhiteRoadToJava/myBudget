package com.mybudget.server.repositories;

import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.Transfer;
import com.mybudget.server.modules.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransferRepository extends MongoRepository<Transfer, String> {
    // every transfer that touches the account: as source, destination or fee account
    List<Transfer> findAllBySourceAccountOrDestinationAccountOrSendFeeAccountOrReceiveFeeAccount(
            Account source, Account destination, Account sendFeeAccount, Account receiveFeeAccount);
    List<Transfer> findAllByUser(String userId);

    List<Transfer> findByUserAndCreatedAtBetween(User user, LocalDateTime date1, LocalDateTime date2);
}
