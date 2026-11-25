package com.mybudget.server.repositories;

import com.mybudget.server.modules.SupportedCurrency;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SupportedCurrencyRepository extends MongoRepository<SupportedCurrency, String> {
    List<SupportedCurrency> findAllByOrderByPositionAsc();
}
