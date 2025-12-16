package com.example.backend.controller;

import com.example.backend.dto.OrderCreateRequest;
import com.example.backend.dto.OrderResponse;
import com.example.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrderController {

    @GetMapping
    public List<OrderResponse> history() {
        return orderService.myOrders();
    }

    @PostMapping
    public OrderResponse create(@RequestBody OrderCreateRequest req) {
        return orderService.create(req);
    }

    @GetMapping("/{orderId}")
    public OrderResponse detail(@PathVariable Long orderId) {
        return orderService.detail(orderId);
    }

    private final OrderService orderService;
}
