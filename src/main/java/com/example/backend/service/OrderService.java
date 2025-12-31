package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.entity.*;
import com.example.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    // ✅ FINAL: receiver + address manbalari
    private final ReceiverService receiverService;
    private final AddressService addressService;

    private final OrderStatusHistoryService orderStatusHistoryService;

    /* ================= CREATE ORDER (CART → ORDER) ================= */

    @Transactional
    public OrderResponse create(OrderCreateRequest req) {

        User user = userService.getCurrentUser();

        Receiver receiver = receiverService.getOwnedByCurrentUser(req.getReceiverId());
        Address address = addressService.getOwnedByCurrentUser(req.getAddressId());

        List<CartItem> cartItems = cartRepo.findByUserId(user.getId());
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Order order = new Order();
        order.setUser(user);

        // 🔗 FK
        order.setReceiverId(receiver.getId());
        order.setAddressId(address.getId());

        // 📸 SNAPSHOT
        order.setAddress(address.getAddress());
        order.setLatitude(address.getLatitude());
        order.setLongitude(address.getLongitude());
        order.setPhone(receiver.getPhone());

        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.ZERO);

        order = orderRepo.save(order);

        orderStatusHistoryService.log(order, OrderStatus.PENDING);


        BigDecimal total = BigDecimal.ZERO;

        for (CartItem c : cartItems) {

            Product product = c.getProduct();

            if (product.getStock() < c.getQuantity()) {
                throw new RuntimeException(
                        "Not enough stock for product: " + product.getName()
                );
            }

            product.setStock(product.getStock() - c.getQuantity());
            productRepo.save(product);

            BigDecimal price =
                    product.getDiscountPrice() != null
                            && product.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0
                            ? product.getDiscountPrice()
                            : product.getPrice();

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(c.getQuantity());
            item.setPrice(price);

            orderItemRepo.save(item);
        }

        order.setTotalAmount(total);
        orderRepo.save(order);

        cartRepo.deleteByUserId(user.getId());

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
                .filter(o -> o.getUser().getId().equals(user.getId()) || user.getRole() == Role.ADMIN)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        List<OrderItemResponse> items =
                orderItemRepo.findByOrderId(order.getId())
                        .stream()
                        .map(i -> {
                            String imageUrl = productImageRepo
                                    .findFirstByProductIdAndMainTrue(i.getProduct().getId())
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
                order.getAddress(),
                order.getLatitude(),
                order.getLongitude(),
                order.getPhone(),
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
        if (order.getStatus() != OrderStatus.PAID && newStatus == OrderStatus.PAID) {

            List<OrderItem> items = orderItemRepo.findByOrder(order);

            for (OrderItem item : items) {
                Product product = item.getProduct();
                product.setSoldCount(product.getSoldCount() + item.getQuantity());
                productRepo.save(product);
            }
        }

        // 🔄 AGAR CANCEL BO‘LSA → STOCK QAYTADI
        if (order.getStatus() != OrderStatus.CANCELED && newStatus == OrderStatus.CANCELED) {

            List<OrderItem> items = orderItemRepo.findByOrder(order);

            for (OrderItem item : items) {
                Product product = item.getProduct();
                product.setStock(product.getStock() + item.getQuantity());
                productRepo.save(product);
            }
        }

        order.setStatus(newStatus);
        orderRepo.save(order);

        orderStatusHistoryService.log(order, newStatus);

    }
}
