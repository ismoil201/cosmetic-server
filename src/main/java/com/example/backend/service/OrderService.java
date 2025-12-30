package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.entity.*;
import com.example.backend.repository.*;
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
    private final ProductRepository productRepo;
    private final ProductImageRepository productImageRepo;
    private final UserService userService;

    /* ================= CREATE ORDER (CART → ORDER) ================= */

    @Transactional
    public OrderResponse create(OrderCreateRequest req) {

        User user = userService.getCurrentUser();
        List<CartItem> cartItems = cartRepo.findByUserId(user.getId());

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // 🔒 1. STOCKNI AVVAL TEKSHIRIB OLAMIZ
        for (CartItem c : cartItems) {
            Product p = c.getProduct();

            if (!p.isActive()) {
                throw new RuntimeException("Product is not active: " + p.getName());
            }

            if (c.getQuantity() > p.getStock()) {
                throw new RuntimeException(
                        "Not enough stock for product: " + p.getName()
                );
            }
        }

        // 🔒 2. ORDER CREATE
        Order order = new Order();
        order.setUser(user);
        order.setAddress(req.getAddress());
        order.setLatitude(req.getLatitude());
        order.setLongitude(req.getLongitude());
        order.setPhone(req.getPhone());

        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(0);

        order = orderRepo.save(order);

        double total = 0;

        // 🔥 3. ORDER ITEMS + STOCK KAMAYTIRISH
        for (CartItem c : cartItems) {

            Product product = c.getProduct();

            // 🔥 STOCK KAMAYADI
            product.setStock(product.getStock() - c.getQuantity());
            productRepo.save(product);

            double price = product.getDiscountPrice() > 0
                    ? product.getDiscountPrice()
                    : product.getPrice();

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(c.getQuantity());
            item.setPrice(price); // snapshot

            orderItemRepo.save(item);

            total += price * c.getQuantity();
        }

        order.setTotalAmount(total);
        orderRepo.save(order);

        // 🧹 4. CART TOZALANADI
        cartRepo.deleteByUserId(user.getId());

        // ✅ Hammasi muvaffaqiyatli → commit
        return detail(order.getId());
    }

    /* ================= MY ORDERS ================= */

    public List<OrderResponse> myOrders() {

        User user = userService.getCurrentUser();

        return orderRepo.findByUserId(user.getId())
                .stream()
                .map(o -> detail(o.getId()))
                .toList();
    }

    /* ================= ORDER DETAIL ================= */

    public OrderResponse detail(Long orderId) {

        User user = userService.getCurrentUser();

        Order order = orderRepo.findById(orderId)
                .filter(o ->
                        o.getUser().getId().equals(user.getId())
                                || user.getRole() == Role.ADMIN
                )
                .orElseThrow(() -> new RuntimeException("Order not found"));

        List<OrderItemResponse> items =
                orderItemRepo.findByOrderId(order.getId())
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
                        .toList();

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                items
        );
    }

    /* ================= ADMIN: ALL ORDERS ================= */

    public List<OrderResponse> allOrders() {
        return orderRepo.findAll()
                .stream()
                .map(o -> detail(o.getId()))
                .toList();
    }

    /* ================= ADMIN: UPDATE STATUS ================= */

    @Transactional
    public void updateStatus(Long orderId, String status) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());

        // 🔥 SOLD COUNT FAQAT PAID GA O‘TGANDA
        if (order.getStatus() != OrderStatus.PAID
                && newStatus == OrderStatus.PAID) {

            List<OrderItem> items =
                    orderItemRepo.findByOrder(order);

            for (OrderItem item : items) {
                Product product = item.getProduct();
                product.setSoldCount(
                        product.getSoldCount() + item.getQuantity()
                );
                productRepo.save(product);
            }
        }

        // 🔄 AGAR CANCEL BO‘LSA → STOCK QAYTADI
        if (order.getStatus() != OrderStatus.CANCELED
                && newStatus == OrderStatus.CANCELED) {

            List<OrderItem> items =
                    orderItemRepo.findByOrder(order);

            for (OrderItem item : items) {
                Product product = item.getProduct();
                product.setStock(
                        product.getStock() + item.getQuantity()
                );
                productRepo.save(product);
            }
        }

        order.setStatus(newStatus);
        orderRepo.save(order);
    }
}
