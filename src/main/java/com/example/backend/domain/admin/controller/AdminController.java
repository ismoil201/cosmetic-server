package com.example.backend.domain.admin.controller;

import com.example.backend.domain.order.dto.OrderResponse;
import com.example.backend.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        orderService.updateStatus(id, status);
        return ResponseEntity.ok(Map.of("message", "Status updated"));
    }

}
