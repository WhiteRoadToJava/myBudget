package com.mybudget.server.dto.admin;

// one admin in the admin list; pending = invited, password not chosen yet
public record AdminAccount(
        String id,
        String username,
        String firstName,
        String lastName,
        boolean pending,
        String createdAt
) {
}
