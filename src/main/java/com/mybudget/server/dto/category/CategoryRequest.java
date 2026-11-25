package com.mybudget.server.dto.category;

import com.mybudget.server.modules.enums.CategoryType;
import lombok.Data;

// add: type, name and emoji; change: any of name, emoji, hidden
@Data
public class CategoryRequest {
    private CategoryType type;
    private String name;
    private String emoji;
    private Boolean hidden;
}