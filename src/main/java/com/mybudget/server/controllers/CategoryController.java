package com.mybudget.server.controllers;

import com.mybudget.server.dto.category.CategoryOrderRequest;
import com.mybudget.server.dto.category.CategoryRequest;
import com.mybudget.server.dto.category.CategoryResponse;
import com.mybudget.server.services.category.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        return ResponseEntity.ok(categoryService.getCategories());
    }

    // body: { "type": "EXPENSE", "name": "Coffee", "emoji": "☕" }
    @PostMapping
    public ResponseEntity<CategoryResponse> addCategory(@RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.addCategory(request));
    }

    // body: any of { "name": "...", "emoji": "...", "hidden": true }
    @PatchMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable String categoryId,
                                                           @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(categoryId, request));
    }

    // body: { "type": "EXPENSE", "ids": ["...", "..."] }
    @PutMapping("/order")
    public ResponseEntity<List<CategoryResponse>> orderCategories(@RequestBody CategoryOrderRequest request) {
        return ResponseEntity.ok(categoryService.orderCategories(request));
    }

    // { "deleted": true } or, when transactions use it, { "deleted": false } and it is hidden
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Map<String, Boolean>> deleteCategory(@PathVariable String categoryId) {
        return ResponseEntity.ok(categoryService.deleteCategory(categoryId));
    }
}