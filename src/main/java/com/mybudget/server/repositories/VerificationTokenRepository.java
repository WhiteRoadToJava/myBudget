package com.mybudget.server.repositories;

import com.mybudget.server.modules.VerificationToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface VerificationTokenRepository extends MongoRepository<VerificationToken, String> {
    Optional<VerificationToken> findByTokenHash(String tokenHash);
}
