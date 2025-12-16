package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.entity.*;
import com.example.backend.repository.CartRepository;
import com.example.backend.repository.OrderItemRepository;
import com.example.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final CartRepository cartRepo;
    private final UserService userService;

    @Transactional
    public OrderResponse create(OrderCreateRequest req) {
        User user = userService.getCurrentUser();
        List<CartItem> cartItems = cartRepo.findByUser(user);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Order order = new Order();
        order.setUser(user);
        order.setAddress(req.getAddress());
        order.setPhone(req.getPhone());
        order.setStatus(OrderStatus.PENDING);

        order = orderRepo.save(order);

        double total = 0;

        for (CartItem c : cartItems) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(c.getProduct());
            item.setQuantity(c.getQuantity());
            item.setPrice(
                    c.getProduct().getDiscountPrice() > 0
                            ? c.getProduct().getDiscountPrice()
                            : c.getProduct().getPrice()
            );
            total += item.getPrice() * item.getQuantity();
            orderItemRepo.save(item);
        }

        order.setTotalAmount(total);
        orderRepo.save(order);
        cartRepo.deleteByUser(user);

        return detail(order.getId());
    }

    // ✅ YO‘Q EDI
    public List<OrderResponse> myOrders() {
        User user = userService.getCurrentUser();

        return orderRepo.findByUser(user)
                .stream()
                .map(o -> detail(o.getId()))
                .toList();
    }

    // ✅ YO‘Q EDI
    public OrderResponse detail(Long orderId) {
        User user = userService.getCurrentUser();
        Order order = orderRepo.findById(orderId)
                .filter(o -> o.getUser().equals(user) || user.getRole() == Role.ADMIN)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        List<OrderItemResponse> items = orderItemRepo.findByOrder(order)
                .stream()
                .map(i -> new OrderItemResponse(
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getProduct().getImageUrl(),
                        i.getPrice(),
                        i.getQuantity()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                items
        );
    }

    // ✅ ADMIN
    public List<OrderResponse> allOrders() {
        return orderRepo.findAll()
                .stream()
                .map(o -> detail(o.getId()))
                .toList();
    }

    // ✅ ADMIN
    public void updateStatus(Long orderId, String status) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(OrderStatus.valueOf(status.toUpperCase()));
        orderRepo.save(order);
    }
}

