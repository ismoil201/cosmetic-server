package com.example.backend.controller;

import com.example.backend.dto.OrderResponse;
import com.example.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final OrderService orderService;

    @GetMapping("/orders")
    public List<OrderResponse> allOrders() {
        return orderService.allOrders();
    }

    @PutMapping("/orders/{id}/status")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        orderService.updateStatus(id, status);
        return "Status updated";
    }
}
