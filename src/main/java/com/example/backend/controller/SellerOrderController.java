package com.example.backend.controller;

import com.example.backend.entity.SellerOrder;
import com.example.backend.entity.SellerOrderStatusHistory;
import com.example.backend.service.SellerOrderService;
import com.example.backend.service.SellerOrderStatusHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller/orders")
@RequiredArgsConstructor
public class SellerOrderController {

    private final SellerOrderService sellerOrderService;
    private final SellerOrderStatusHistoryService historyService;

    // ================= LIST =================
    @GetMapping
    public Page<SellerOrder> myOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return sellerOrderService.mySellerOrders(PageRequest.of(page, size));
    }

    // ================= BY STATUS =================
    @GetMapping("/status/{status}")
    public Page<SellerOrder> myOrdersByStatus(
            @PathVariable SellerOrder.SellerOrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return sellerOrderService.mySellerOrdersByStatus(
                status,
                PageRequest.of(page, size)
        );
    }

    // ================= DETAIL =================
    @GetMapping("/{id}")
    public SellerOrder detail(@PathVariable Long id) {
        return sellerOrderService.getMySellerOrder(id);
    }

    // ================= UPDATE STATUS =================
    @PostMapping("/{id}/status")
    public SellerOrder updateStatus(
            @PathVariable Long id,
            @RequestParam SellerOrder.SellerOrderStatus status
    ) {
        return sellerOrderService.updateMySellerOrderStatus(id, status);
    }

    // ================= HISTORY =================
    @GetMapping("/{id}/history")
    public List<SellerOrderStatusHistory> history(@PathVariable Long id) {
        return historyService.getHistory(id);
    }
}