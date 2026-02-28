package com.example.backend.service;

import com.example.backend.entity.OrderStatus;
import com.example.backend.entity.SellerOrder;
import com.example.backend.entity.SellerOrderStatusHistory;
import com.example.backend.entity.User;
import com.example.backend.repository.SellerOrderStatusHistoryRepository;
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