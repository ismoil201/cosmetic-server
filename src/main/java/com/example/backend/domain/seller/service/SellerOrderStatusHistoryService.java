package com.example.backend.domain.seller.service;

import com.example.backend.domain.order.entity.OrderStatus;
import com.example.backend.domain.seller.entity.SellerOrder;
import com.example.backend.domain.seller.entity.SellerOrderStatusHistory;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.seller.repository.SellerOrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SellerOrderStatusHistoryService {

    private final SellerOrderStatusHistoryRepository historyRepository;

    @Transactional(readOnly = true)
    public List<SellerOrderStatusHistory> getHistory(Long sellerOrderId) {
        return historyRepository.findBySellerOrderIdOrderByCreatedAtAsc(sellerOrderId);
    }

    public SellerOrderStatusHistory addHistory(
            SellerOrder sellerOrder,
            OrderStatus newStatus,
            User changedByOrNull
    ) {
        SellerOrderStatusHistory h = new SellerOrderStatusHistory();
        h.setSellerOrder(sellerOrder);
        h.setStatus(newStatus);
        h.setChangedBy(changedByOrNull);
        // createdAt PrePersist bilan set bo'ladi
        return historyRepository.save(h);
    }
}