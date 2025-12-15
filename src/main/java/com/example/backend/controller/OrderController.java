package com.example.backend.controller;

import com.example.backend.dto.OrderCreateRequest;
import com.example.backend.dto.OrderResponse;
import com.example.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // CREATE
    @PostMapping
    public OrderResponse create(@RequestBody OrderCreateRequest req) {
        return orderService.create(req);
    }

    // HISTORY
    @GetMapping
    public List<OrderResponse> history() {
        return orderService.myOrders();
    }

    // DETAIL
    @GetMapping("/{orderId}")
    public OrderResponse detail(@PathVariable Long orderId) {
        return orderService.detail(orderId);
    }
}

