package com.mybudget.server.services;


import com.mybudget.server.exeptions.InvalidTokenException;
import com.mybudget.server.exeptions.InvalidTokenException.Reason;
import com.mybudget.server.modules.VerificationToken;
import com.mybudget.server.modules.enums.TokenType;
import com.mybudget.server.repositories.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class TokenService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final MongoTemplate mongoTemplate;
    private final VerificationTokenRepository repository;

    public TokenService(@Qualifier("mongoTemplate") MongoTemplate mongoTemplate
            , VerificationTokenRepository repository) {
        this.mongoTemplate = mongoTemplate;
        this.repository = repository;
    }

    public String createToken(String username, TokenType type, Duration ttl) {
        Instant now = Instant.now();

        mongoTemplate.updateMulti(
                new Query(Criteria.where("username").is(username)
                        .and("type").is(type)
                        .and("usedAt").is(null)),
                new Update().set("usedAt", now),
                VerificationToken.class);


        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        VerificationToken token = new VerificationToken();
        token.setTokenHash(hash(rawToken));
        token.setUsername(username);
        token.setType(type);
        token.setCreatedAt(now);
        token.setExpiresAt(now.plus(ttl));
        repository.save(token);

        return rawToken;
    }

    public VerificationToken consume(String rawToken, TokenType type) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidTokenException(Reason.TOKEN_INVALID);
        }
        String tokenHash = hash(rawToken.trim());
        Instant now = Instant.now();

        VerificationToken consumed = mongoTemplate.findAndModify(
                new Query(Criteria.where("tokenHash").is(tokenHash)
                        .and("type").is(type)
                        .and("usedAt").is(null)
                        .and("expiresAt").gt(now)),
                new Update().set("usedAt", now),
                FindAndModifyOptions.options().returnNew(true),
                VerificationToken.class);

        if (consumed != null) {
            return consumed;
        }

        boolean expired = repository.findByTokenHash(tokenHash)
                .filter(t -> t.getType() == type && t.getUsedAt() == null)
                .isPresent();
        throw new InvalidTokenException(expired ? Reason.TOKEN_EXPIRED : Reason.TOKEN_INVALID);

    }


    private static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // True if a token of this type was already issued to the user within the given window (resend cooldown).
    public boolean issuedWithin(String username, TokenType type, Duration window) {
        return mongoTemplate.exists(
                new Query(Criteria.where("username").is(username)
                        .and("type").is(type)
                        .and("createdAt").gt(Instant.now().minus(window))),
                VerificationToken.class);
    }
}