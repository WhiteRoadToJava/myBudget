package com.mybudget.server.dto.category;

import com.mybudget.server.modules.Category;
import com.mybudget.server.modules.enums.CategoryType;

public record CategoryResponse(
        String id,
        CategoryType type,
        String key,
        String defaultKey,
        String name,
        String emoji,
        int position,
        boolean hidden
) {
    public static CategoryResponse of(Category c) {
        return new CategoryResponse(c.getId(), c.getType(), c.getKey(), c.getDefaultKey(),
                c.getName(), c.getEmoji(), c.getPosition(), c.isHidden());
    }
}