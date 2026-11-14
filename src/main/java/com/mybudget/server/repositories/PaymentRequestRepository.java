package com.mybudget.server.repositories;

import com.mybudget.server.modules.PaymentRequest;
import com.mybudget.server.modules.enums.PaymentRequestStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PaymentRequestRepository extends MongoRepository<PaymentRequest, String> {
    List<PaymentRequest> findAllByStatusOrderByCreatedAtAsc(PaymentRequestStatus status);

    List<PaymentRequest> findAllByUserIdOrderByCreatedAtDesc(String userId);

    boolean existsByUserIdAndStatus(String userId, PaymentRequestStatus status);

    long countByStatus(PaymentRequestStatus status);
}
