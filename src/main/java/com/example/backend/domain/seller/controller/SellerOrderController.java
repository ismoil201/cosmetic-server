package com.example.backend.domain.seller.controller;

import com.example.backend.domain.seller.dto.SellerOrderDetailResponse;
import com.example.backend.domain.seller.dto.SellerOrderListResponse;
import com.example.backend.domain.order.entity.OrderStatus;
import com.example.backend.domain.seller.entity.SellerOrderStatusHistory;
import com.example.backend.domain.seller.service.SellerOrderService;
import com.example.backend.domain.seller.service.SellerOrderStatusHistoryService;
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
    public Page<SellerOrderListResponse> myOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return sellerOrderService.mySellerOrders(PageRequest.of(page, size))
                .map(SellerOrderListResponse::from);
    }

    // ================= BY STATUS =================
    @GetMapping("/status/{status}")
    public Page<SellerOrderListResponse> myOrdersByStatus(
            @PathVariable OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return sellerOrderService.mySellerOrdersByStatus(status, PageRequest.of(page, size))
                .map(SellerOrderListResponse::from);
    }

    // ================= DETAIL =================
    @GetMapping("/{id}")
    public SellerOrderDetailResponse detail(@PathVariable Long id) {
        return SellerOrderDetailResponse.from(
                sellerOrderService.getMySellerOrder(id)
        );
    }

    // ================= UPDATE STATUS =================
    @PostMapping("/{id}/status")
    public SellerOrderDetailResponse updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status
    ) {
        return SellerOrderDetailResponse.from(
                sellerOrderService.updateMySellerOrderStatus(id, status)
        );
    }

    // ================= HISTORY =================
    @GetMapping("/{id}/history")
    public List<SellerOrderStatusHistory> history(@PathVariable Long id) {
        return historyService.getHistory(id);
    }
}