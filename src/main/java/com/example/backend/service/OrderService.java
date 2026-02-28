package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.entity.*;
import com.example.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final CartItemRepository cartRepo;
    private final ProductRepository productRepo;
    private final ProductImageRepository productImageRepo;
    private final UserService userService;
    private final SellerOrderRepository sellerOrderRepo;
    private final SellerOrderStatusHistoryService sellerOrderHistoryService;

    // ✅ FINAL: receiver + address manbalari
    private final ReceiverService receiverService;
    private final AddressService addressService;
    private final UserRepository userRepo;
    private final NotificationService notificationService;

    private final OrderStatusHistoryService orderStatusHistoryService;
    private  final FcmService  fcmService;

    /* ================= CREATE ORDER (CART → ORDER) ================= */
    @Transactional
    public OrderResponse create(OrderCreateRequest req) {

        User user = userService.getCurrentUser();

        Receiver receiver = receiverService.getOwnedByCurrentUser(req.getReceiverId());
        Address address = addressService.getOwnedByCurrentUser(req.getAddressId());

        List<CartItem> cartItems = cartRepo.findByUserId(user.getId());
        if (cartItems.isEmpty()) throw new RuntimeException("Cart is empty");

        // ================= MASTER ORDER =================
        Order order = new Order();
        order.setUser(user);
        order.setReceiverId(receiver.getId());
        order.setAddressId(address.getId());

        order.setAddress(address.getAddress());
        order.setLatitude(address.getLatitude());
        order.setLongitude(address.getLongitude());
        order.setPhone(receiver.getPhone());

        order.setStatus(OrderStatus.NEW);
        order.setTotalAmount(BigDecimal.ZERO);

        order = orderRepo.save(order);
        orderStatusHistoryService.log(order, OrderStatus.NEW);

        // ================= GROUP BY SELLER =================
        Map<Seller, List<CartItem>> grouped =
                cartItems.stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                c -> c.getProduct().getSeller()
                        ));

        BigDecimal grandTotal = BigDecimal.ZERO;

        // ================= EACH SELLER =================
        for (Map.Entry<Seller, List<CartItem>> entry : grouped.entrySet()) {

            Seller seller = entry.getKey();
            List<CartItem> items = entry.getValue();

            SellerOrder sellerOrder = new SellerOrder();
            sellerOrder.setOrder(order);
            sellerOrder.setSeller(seller);
            sellerOrder.setStatus(OrderStatus.NEW);
            sellerOrder.setSubtotalAmount(BigDecimal.ZERO);
            sellerOrder.setShippingFee(BigDecimal.ZERO);

            sellerOrder = sellerOrderRepo.save(sellerOrder);

            sellerOrderHistoryService.addHistory(
                    sellerOrder,
                    OrderStatus.NEW,
                    user
            );

            BigDecimal sellerSubtotal = BigDecimal.ZERO;

            for (CartItem c : items) {

                Product product = c.getProduct();

                if (product.getStock() < c.getQuantity()) {
                    throw new RuntimeException("Not enough stock: " + product.getName());
                }

                product.setStock(product.getStock() - c.getQuantity());
                productRepo.save(product);

                BigDecimal price =
                        product.getDiscountPrice() != null &&
                                product.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0
                                ? product.getDiscountPrice()
                                : product.getPrice();

                BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(c.getQuantity()));
                sellerSubtotal = sellerSubtotal.add(lineTotal);

                OrderItem oi = new OrderItem();
                oi.setOrder(order);
                oi.setSellerOrder(sellerOrder);   // 🔥 MUHIM
                oi.setProduct(product);
                oi.setQuantity(c.getQuantity());
                oi.setPrice(price);

                orderItemRepo.save(oi);
            }

            sellerOrder.setSubtotalAmount(sellerSubtotal);
            sellerOrderRepo.save(sellerOrder);

            grandTotal = grandTotal.add(sellerSubtotal);
        }

        order.setTotalAmount(grandTotal);
        orderRepo.save(order);

        cartRepo.deleteByUserId(user.getId());

        BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500000");
        BigDecimal SHIPPING_FEE = new BigDecimal("30000");

// grandTotal -> productlar summasi (siz hisoblagansiz)
        BigDecimal finalTotal = grandTotal.compareTo(FREE_SHIPPING_THRESHOLD) < 0
                ? grandTotal.add(SHIPPING_FEE)
                : grandTotal;

        order.setTotalAmount(finalTotal);
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
                user.getFullName(),
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
        if (order.getStatus() != OrderStatus.CONFIRMED && newStatus == OrderStatus.CONFIRMED) {

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


        if (newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.DELIVERED) {

            fcmService.sendToUser(
                    order.getUser().getId(),
                    newStatus == OrderStatus.SHIPPED
                            ? "📦 Buyurtma jo‘natildi"
                            : "✅ Buyurtma yetib bordi",
                    newStatus == OrderStatus.SHIPPED
                            ? "Buyurtmangiz kuryerga topshirildi"
                            : "Xaridingiz yetkazildi",
                    Map.of(
                            "type", "ORDER",
                            "orderId", order.getId().toString()
                    )
            );

            notificationService.orderStatusChanged(
                    order.getUser(),
                    order,
                    newStatus
            );
        }


        // 🔔 NOTIFICATION
        notificationService.orderStatusChanged(
                order.getUser(),
                order,
                newStatus
        );
    }
}
