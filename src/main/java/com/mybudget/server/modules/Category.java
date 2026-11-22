package com.mybudget.server.modules;


import com.mybudget.server.modules.enums.CategoryType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.awt.*;

@Data
@NoArgsConstructor
@Document(collection = "categories")
public class Category {
    @Id
    private String id;
    @DBRef
    private User user;
    private CategoryType type;
    private String key;
    private String defaultKey;
    private String name;
    private String emoji;
    private int position;
    private boolean hidden;
}
