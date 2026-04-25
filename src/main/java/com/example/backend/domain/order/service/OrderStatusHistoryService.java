package com.example.backend.domain.order.service;

import com.example.backend.domain.order.dto.OrderStatusHistoryResponse;
import com.example.backend.domain.order.entity.Order;
import com.example.backend.domain.order.entity.OrderStatus;
import com.example.backend.domain.order.entity.OrderStatusHistory;
import com.example.backend.domain.user.entity.Role;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.order.repository.OrderRepository;
import com.example.backend.domain.order.repository.OrderStatusHistoryRepository;
import com.example.backend.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderStatusHistoryService {

    private final OrderStatusHistoryRepository historyRepo;
    private final UserService userService;

    private final OrderRepository orderRepo;

    public void log(Order order, OrderStatus status) {

        User currentUser = null;
        try {
            currentUser = userService.getCurrentUser();
        } catch (Exception ignored) {
            // system / scheduler holatlari uchun
        }

        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrder(order);
        h.setStatus(status);
        h.setChangedBy(currentUser);

        historyRepo.save(h);
    }

    public List<OrderStatusHistoryResponse> history(Long orderId) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        User user = userService.getCurrentUser();

        if (!order.getUser().getId().equals(user.getId())
                && user.getRole() != Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }

        return historyRepo.findByOrderIdOrderByCreatedAtAsc(orderId)
                .stream()
                .map(h -> new OrderStatusHistoryResponse(
                        h.getStatus(),
                        h.getCreatedAt()
                ))
                .toList();
    }


}
