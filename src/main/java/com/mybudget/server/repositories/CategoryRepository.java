package com.mybudget.server.repositories;

import com.mybudget.server.modules.Category;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.enums.CategoryType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends MongoRepository<Category, String> {
    List<Category> findByUser(User user);

    List<Category> findByUserAndType(User user, CategoryType type);

    Optional<Category> findByIdAndUser(String id, User user);
}
