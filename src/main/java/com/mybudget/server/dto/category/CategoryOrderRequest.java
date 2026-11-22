package com.mybudget.server.dto.category;

import com.mybudget.server.modules.enums.CategoryType;
import lombok.Data;

import java.util.List;

// the categories of one type, in the order the user wants them
@Data
public class CategoryOrderRequest {
    private CategoryType type;
    private List<String> ids;
}