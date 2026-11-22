package com.mybudget.server.services.category;

import com.mybudget.server.modules.Category;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.enums.CategoryType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultCategoriesTest {

    private static String defaultKeyOf(CategoryType type, String key) {
        return DefaultCategories.ALL.stream()
                .filter(d -> d.type() == type && d.key().equals(key))
                .findFirst().orElseThrow().defaultKey();
    }

    @Test
    void theOldIdsKeepTheirCategory() {
        // the fixed lists the frontend had before user categories
        Map<String, String> oldExpenses = Map.ofEntries(
                Map.entry("1", "rent"), Map.entry("2", "food"), Map.entry("3", "groceries"),
                Map.entry("4", "car"), Map.entry("5", "bus"), Map.entry("6", "check"),
                Map.entry("7", "currency"), Map.entry("8", "delivery"), Map.entry("9", "home"),
                Map.entry("10", "gift"), Map.entry("11", "bills"));
        Map<String, String> oldIncomes = Map.of(
                "1", "salary", "2", "childAllowance", "3", "housingAllowance", "4", "gift",
                "5", "wallet", "6", "bank", "7", "investment");
        oldExpenses.forEach((key, name) -> assertEquals(name, defaultKeyOf(CategoryType.EXPENSE, key)));
        oldIncomes.forEach((key, name) -> assertEquals(name, defaultKeyOf(CategoryType.INCOME, key)));
    }

    @Test
    void keysAreUniqueInEachType() {
        Set<String> seen = new HashSet<>();
        for (DefaultCategories.Default d : DefaultCategories.ALL) {
            assertTrue(seen.add(d.type() + "/" + d.key()), "twice: " + d.type() + " " + d.key());
        }
    }

    @Test
    void aUserGetsTheListInOrderWithoutNames() {
        User user = new User();
        List<Category> categories = DefaultCategories.forUser(user);

        assertEquals(DefaultCategories.ALL.size(), categories.size());
        List<Category> incomes = categories.stream().filter(c -> c.getType() == CategoryType.INCOME).toList();
        for (int i = 0; i < incomes.size(); i++) {
            assertEquals(i, incomes.get(i).getPosition());
        }
        assertTrue(categories.stream().allMatch(c -> c.getUser() == user && c.getName() == null && !c.isHidden()));
    }
}