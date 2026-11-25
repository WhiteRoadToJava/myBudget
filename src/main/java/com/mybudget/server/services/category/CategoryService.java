package com.mybudget.server.services.category;

import com.mybudget.server.dto.category.CategoryOrderRequest;
import com.mybudget.server.dto.category.CategoryRequest;
import com.mybudget.server.dto.category.CategoryResponse;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.modules.Category;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.enums.CategoryType;
import com.mybudget.server.modules.enums.TransactionType;
import com.mybudget.server.repositories.CategoryRepository;
import com.mybudget.server.repositories.ExpenseRepository;
import com.mybudget.server.repositories.IncomseRepository;
import com.mybudget.server.repositories.ScheduleRepository;
import com.mybudget.server.util.UserUtils;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final IncomseRepository incomseRepository;
    private final ExpenseRepository expenseRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserUtils userUtils;

    // two requests at the same moment must not give a user the ready-made list twice
    private final Object seedLock = new Object();

    // the user's categories, incomes first, each type in the user's order;
    // the first time, the ready-made ones are made
    public List<CategoryResponse> getCategories() {
        User user = userUtils.getCurrentAuthenticatedUser();
        return categoriesOf(user).stream()
                .sorted(Comparator.comparing(Category::getType).thenComparingInt(Category::getPosition))
                .map(CategoryResponse::of)
                .toList();
    }

    public CategoryResponse addCategory(CategoryRequest request) {
        User user = userUtils.getCurrentAuthenticatedUser();
        if (request.getType() == null) {
            throw new IllegalArgumentException("The category type is required");
        }
        String name = CategoryRules.name(request.getName());
        if (name == null) {
            throw new IllegalArgumentException("The category name is required");
        }
        categoriesOf(user); // the ready-made ones first, so the new one comes after them

        Category category = new Category();
        // the key is the id, so it is known before saving
        String id = new ObjectId().toHexString();
        category.setId(id);
        category.setKey(id);
        category.setUser(user);
        category.setType(request.getType());
        category.setName(name);
        category.setEmoji(CategoryRules.emoji(request.getEmoji()));
        category.setPosition(nextPosition(user, request.getType()));
        return CategoryResponse.of(categoryRepository.save(category));
    }

    // only what is sent changes; an empty name gives a ready-made category its translated name back
    public CategoryResponse updateCategory(String categoryId, CategoryRequest request) {
        User user = userUtils.getCurrentAuthenticatedUser();
        Category category = findOwned(categoryId, user);
        if (request.getName() != null) {
            String name = CategoryRules.name(request.getName());
            if (name == null && category.getDefaultKey() == null) {
                throw new IllegalArgumentException("The category name is required");
            }
            category.setName(name);
        }
        if (request.getEmoji() != null) {
            category.setEmoji(CategoryRules.emoji(request.getEmoji()));
        }
        if (request.getHidden() != null) {
            category.setHidden(request.getHidden());
        }
        return CategoryResponse.of(categoryRepository.save(category));
    }

    // the listed categories first, in that order; the others of the type keep their order after them
    public List<CategoryResponse> orderCategories(CategoryOrderRequest request) {
        User user = userUtils.getCurrentAuthenticatedUser();
        if (request.getType() == null || request.getIds() == null) {
            throw new IllegalArgumentException("The type and the order are required");
        }
        List<Category> ofType = categoryRepository.findByUserAndType(user, request.getType());
        List<String> ids = request.getIds();
        List<Category> ordered = ofType.stream()
                .sorted(Comparator
                        .comparingInt((Category c) -> ids.contains(c.getId()) ? ids.indexOf(c.getId()) : ids.size())
                        .thenComparingInt(Category::getPosition))
                .toList();
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setPosition(i);
        }
        categoryRepository.saveAll(ordered);
        return getCategories();
    }

    // deleted when nothing uses it; otherwise hidden, so its old transactions keep their category
    public Map<String, Boolean> deleteCategory(String categoryId) {
        User user = userUtils.getCurrentAuthenticatedUser();
        Category category = findOwned(categoryId, user);
        if (isUsed(user, category)) {
            category.setHidden(true);
            categoryRepository.save(category);
            return Map.of("deleted", false);
        }
        categoryRepository.delete(category);
        return Map.of("deleted", true);
    }

    private boolean isUsed(User user, Category category) {
        String key = category.getKey();
        if (category.getType() == CategoryType.INCOME) {
            return incomseRepository.existsByUserAndCategory(user, key)
                    || scheduleRepository.existsByUserAndCategoryAndTransactionTypesContaining(user, key, TransactionType.INCOMSE);
        }
        return expenseRepository.existsByUserAndCategory(user, key)
                || scheduleRepository.existsByUserAndCategoryAndTransactionTypesContaining(user, key, TransactionType.EXPENSE);
    }

    private List<Category> categoriesOf(User user) {
        synchronized (seedLock) {
            List<Category> categories = categoryRepository.findByUser(user);
            if (!categories.isEmpty()) {
                return categories;
            }
            return categoryRepository.saveAll(DefaultCategories.forUser(user));
        }
    }

    private int nextPosition(User user, CategoryType type) {
        return categoryRepository.findByUserAndType(user, type).stream()
                .mapToInt(Category::getPosition)
                .max()
                .orElse(-1) + 1;
    }

    private Category findOwned(String categoryId, User user) {
        return categoryRepository.findByIdAndUser(categoryId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }
}