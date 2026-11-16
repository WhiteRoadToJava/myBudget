package com.mybudget.server.repositories;

import com.mybudget.server.modules.ExchangeRate;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends MongoRepository<ExchangeRate, String> {
    Optional<ExchangeRate> findByUserIdAndFromCurrencyAndToCurrency(String userId, String fromCurrency, String toCurrency);
    List<ExchangeRate> findAllByUserIdOrderByUpdateAtDes(String userId);

    Optional<ExchangeRate> findByIdAndUserId(String id, String userId);
}
