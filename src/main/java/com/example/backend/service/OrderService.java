package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.entity.*;
import com.example.backend.repository.CartItemRepository;
import com.example.backend.repository.OrderItemRepository;
import com.example.backend.repository.OrderRepository;
import com.example.backend.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final CartItemRepository cartRepo;
    private final UserService userService;
    private final ProductImageRepository productImageRepo;


    @Transactional
    public OrderResponse create(OrderCreateRequest req) {
        User user = userService.getCurrentUser();
        List<CartItem> cartItems = cartRepo.findByUserId(user.getId());

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

            double price = c.getProduct().getDiscountPrice() > 0
                    ? c.getProduct().getDiscountPrice()
                    : c.getProduct().getPrice();

            item.setPrice(price);
            total += price * item.getQuantity();

            orderItemRepo.save(item);
        }

        order.setTotalAmount(total);
        orderRepo.save(order);

        cartRepo.deleteByUserId(user.getId());

        return detail(order.getId());
    }

    public List<OrderResponse> myOrders() {
        User user = userService.getCurrentUser();

        return orderRepo.findByUserId(user.getId())
                .stream()
                .map(o -> detail(o.getId()))
                .toList();
    }

    public OrderResponse detail(Long orderId) {

        User user = userService.getCurrentUser();

        Order order = orderRepo.findById(orderId)
                .filter(o ->
                        o.getUser().getId().equals(user.getId())
                                || user.getRole() == Role.ADMIN
                )
                .orElseThrow(() -> new RuntimeException("Order not found"));

        List<OrderItemResponse> items = orderItemRepo.findByOrderId(order.getId())
                .stream()
                .map(i -> {

                    String imageUrl = productImageRepo
                            .findByProductIdAndMainTrue(i.getProduct().getId())
                            .map(ProductImage::getImageUrl)
                            .orElse(null);

                    return new OrderItemResponse(
                            i.getProduct().getId(),
                            i.getProduct().getName(),
                            imageUrl,
                            i.getPrice(),
                            i.getQuantity()
                    );
                })
                .toList(); // 🔥 MUHIM

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                items
        );
    }


    // ADMIN
    public List<OrderResponse> allOrders() {
        return orderRepo.findAll()
                .stream()
                .map(o -> detail(o.getId()))
                .toList();
    }

    // ADMIN
    public void updateStatus(Long orderId, String status) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(OrderStatus.valueOf(status.toUpperCase()));
        orderRepo.save(order);
    }
}
