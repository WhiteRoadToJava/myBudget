package com.mybudget.server.modules;

import com.mybudget.server.modules.enums.TokenType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "verification_tokens")
public class VerificationToken {
    @Id
    private String id;

    private String tokenHash;
    private String username;
    private TokenType type;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant usedAt;
}
