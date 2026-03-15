package com.mybudget.server.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.mybudget.server.module.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
}