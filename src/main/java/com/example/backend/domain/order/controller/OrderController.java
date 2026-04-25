package com.example.backend.domain.order.controller;

import com.example.backend.domain.order.dto.OrderCreateRequest;
import com.example.backend.domain.order.dto.OrderResponse;
import com.example.backend.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/orders")
@PreAuthorize("isAuthenticated()")
public class OrderController {

    private final OrderService orderService;

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
}

