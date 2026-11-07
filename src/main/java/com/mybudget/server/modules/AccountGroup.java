package com.mybudget.server.modules;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

// A basket: several accounts, one per currency, shown to the user as one account.
// Balances, transactions and transfers stay on the accounts; the basket only groups them.
@Data
@NoArgsConstructor
@Document(collection = "account_groups")
public class AccountGroup {
    @Id
    private String id;
    private String name;
    private String type;
    @DBRef
    private User user;
    private Instant createdAt;
}
