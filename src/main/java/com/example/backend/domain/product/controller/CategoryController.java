package com.example.backend.domain.product.controller;


import com.example.backend.domain.product.entity.Category;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @GetMapping
    public List<String> list() {
        return Arrays.stream(Category.values())
                .map(Enum::name)
                .toList();
    }
}
