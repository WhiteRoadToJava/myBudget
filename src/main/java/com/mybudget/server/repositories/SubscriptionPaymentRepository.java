package com.mybudget.server.repositories;

import com.mybudget.server.modules.SubscriptionPayment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SubscriptionPaymentRepository extends MongoRepository<SubscriptionPayment, String> {
    List<SubscriptionPayment> findAllByUserIdOrderByCreatedAtDesc(String userId);
}
