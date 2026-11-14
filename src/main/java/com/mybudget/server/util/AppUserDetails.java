package com.mybudget.server.util;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.time.Instant;
import java.util.Collection;

public class AppUserDetails extends User {

    private final Instant passwordChangedAt;

    public AppUserDetails(String username,
                          String password,
                          boolean enabled,
                          Collection<? extends GrantedAuthority> authorities,
                          Instant passwordChangedAt) {
        super(username, password, enabled, true, true, true, authorities);
        this.passwordChangedAt = passwordChangedAt;
    }

    public Instant getPasswordChangedAt(){
        return passwordChangedAt;
    }
}
