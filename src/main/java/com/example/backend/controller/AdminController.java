package com.example.backend.controller;

import com.example.backend.dto.OrderResponse;
import com.example.backend.service.OrderService;
import com.example.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final OrderService orderService;
    private final ProductService productService;

    // ALL ORDERS
    @GetMapping("/orders")
    public List<OrderResponse> allOrders() {
        return orderService.allOrders();
    }

    // UPDATE ORDER STATUS
    @PutMapping("/orders/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        orderService.updateStatus(id, status);
        return ResponseEntity.ok("Status updated");
    }
}

