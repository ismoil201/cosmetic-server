package com.example.backend.domain.order.controller;

import com.example.backend.domain.order.dto.OrderStatusHistoryResponse;
import com.example.backend.domain.order.service.OrderStatusHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@PreAuthorize("isAuthenticated()")
public class OrderStatusHistoryController {

    private final OrderStatusHistoryService historyService;

    /**
     * 📜 Order status timeline
     * GET /api/orders/{orderId}/history
     */
    @GetMapping("/{orderId}/history")
    public List<OrderStatusHistoryResponse> history(
            @PathVariable Long orderId
    ) {
        return historyService.history(orderId);
    }
}
