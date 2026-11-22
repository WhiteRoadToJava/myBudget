package com.mybudget.server.services.category;

import com.mybudget.server.modules.Category;
import com.mybudget.server.modules.User;
import com.mybudget.server.modules.enums.CategoryType;

import java.util.ArrayList;
import java.util.List;

// The categories every user starts with. The keys of the first ones are the ids of the
// old fixed lists in the frontend, so transactions made before keep their category.
// Add new ones at the end of a list, with the next number: never change or reuse a key.
public final class DefaultCategories {

    private DefaultCategories() {
    }

    public record Default(CategoryType type, String key, String defaultKey, String emoji) {
    }

    public static final List<Default> ALL = List.of(
            expense("1", "rent", "🏠"),
            expense("2", "food", "🍽️"),
            expense("3", "groceries", "🛒"),
            expense("4", "car", "🚗"),
            expense("5", "bus", "🚌"),
            expense("6", "check", "🧾"),
            expense("7", "currency", "💱"),
            expense("8", "delivery", "📦"),
            expense("9", "home", "🛋️"),
            expense("10", "gift", "🎁"),
            expense("11", "bills", "💡"),
            expense("12", "health", "🩺"),
            expense("13", "education", "🎓"),
            expense("14", "clothes", "👕"),
            expense("15", "phone", "📱"),
            expense("16", "travel", "✈️"),
            expense("17", "entertainment", "🎬"),
            expense("18", "other", "🗂️"),

            income("1", "salary", "💼"),
            income("2", "childAllowance", "👶"),
            income("3", "housingAllowance", "🏘️"),
            income("4", "gift", "🎁"),
            income("5", "wallet", "👛"),
            income("6", "bank", "🏦"),
            income("7", "investment", "📈"),
            income("8", "freelance", "💻"),
            income("9", "refund", "↩️"),
            income("10", "other", "🗂️")
    );

    private static Default expense(String key, String defaultKey, String emoji) {
        return new Default(CategoryType.EXPENSE, key, defaultKey, emoji);
    }

    private static Default income(String key, String defaultKey, String emoji) {
        return new Default(CategoryType.INCOME, key, defaultKey, emoji);
    }

    // a fresh copy for one user, in the order of the list
    public static List<Category> forUser(User user) {
        List<Category> categories = new ArrayList<>();
        int incomes = 0;
        int expenses = 0;
        for (Default d : ALL) {
            Category c = new Category();
            c.setUser(user);
            c.setType(d.type());
            c.setKey(d.key());
            c.setDefaultKey(d.defaultKey());
            c.setEmoji(d.emoji());
            c.setPosition(d.type() == CategoryType.INCOME ? incomes++ : expenses++);
            categories.add(c);
        }
        return categories;
    }
}