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

    // inject qo‘shing:
    private final ProductVariantRepository variantRepo;
    private final PricingService pricingService;
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

        List<Long> selectedCartItemIds = req.getCartItemIds();

        if (selectedCartItemIds == null || selectedCartItemIds.isEmpty()) {
            throw new RuntimeException("No cart items selected");
        }

        List<CartItem> cartItems = cartRepo.findByUserIdAndIdIn(user.getId(), selectedCartItemIds);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Selected cart items not found");
        }

        if (cartItems.size() != selectedCartItemIds.size()) {
            throw new RuntimeException("Some selected cart items are invalid");
        }

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

        Map<Seller, List<CartItem>> grouped =
                cartItems.stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                c -> c.getVariant().getProduct().getSeller()
                        ));

        BigDecimal grandTotal = BigDecimal.ZERO;

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

                ProductVariant v = c.getVariant();
                Product product = v.getProduct();

                if (v.getStock() < c.getQuantity()) {
                    throw new RuntimeException("Not enough stock: " + product.getName() + " " + v.getLabel());
                }

                v.setStock(v.getStock() - c.getQuantity());
                variantRepo.save(v);

                BigDecimal lineTotal = pricingService.lineTotal(v, c.getQuantity());
                sellerSubtotal = sellerSubtotal.add(lineTotal);

                OrderItem oi = new OrderItem();
                oi.setOrder(order);
                oi.setSellerOrder(sellerOrder);
                oi.setProduct(product);
                oi.setVariant(v);
                oi.setQuantity(c.getQuantity());
                oi.setPrice(lineTotal);

                orderItemRepo.save(oi);
            }

            sellerOrder.setSubtotalAmount(sellerSubtotal);
            sellerOrderRepo.save(sellerOrder);

            grandTotal = grandTotal.add(sellerSubtotal);
        }

        BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500000");
        BigDecimal SHIPPING_FEE = new BigDecimal("30000");

        BigDecimal finalTotal = grandTotal.compareTo(FREE_SHIPPING_THRESHOLD) < 0
                ? grandTotal.add(SHIPPING_FEE)
                : grandTotal;

        order.setTotalAmount(finalTotal);
        orderRepo.save(order);

        cartRepo.deleteByUserIdAndIdIn(user.getId(), selectedCartItemIds);

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

                            ProductVariant v = i.getVariant(); // null bo‘lmasligi kerak

                            return new OrderItemResponse(
                                    i.getProduct().getId(),
                                    i.getProduct().getName(),

                                    v != null ? v.getId() : null,
                                    v != null ? v.getLabel() : null,

                                    imageUrl,

                                    i.getPrice(),      // ✅ line total (siz shu qiymatni saqlagansiz)
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
        User admin = userService.getCurrentUser(); // ✅ qo'shildi (ADMIN bo'lishi kerak)

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

        // 🔄 AGAR CANCEL BO‘LSA → STOCK QAYTADI (VARIANT STOCK)
        if (order.getStatus() != OrderStatus.CANCELED && newStatus == OrderStatus.CANCELED) {

            List<OrderItem> items = orderItemRepo.findByOrder(order);

            for (OrderItem item : items) {
                ProductVariant v = item.getVariant();
                if (v != null) {
                    v.setStock(v.getStock() + item.getQuantity());
                    variantRepo.save(v);
                }
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

        syncSellerOrdersFromMaster(orderId, newStatus, admin); // user - admin bo'ladi

        // 🔔 NOTIFICATION
        notificationService.orderStatusChanged(
                order.getUser(),
                order,
                newStatus
        );
    }


    private int rank(OrderStatus s) {
        return switch (s) {
            case NEW -> 1;
            case CONFIRMED -> 2;
            case PACKED -> 3;
            case SHIPPED -> 4;
            case DELIVERED -> 5;
            case CANCELED -> 0;
        };
    }
    @Transactional
    public void syncSellerOrdersFromMaster(Long orderId, OrderStatus newStatus, User changedBy) {
        List<SellerOrder> subs = sellerOrderRepo.findByOrderId(orderId);
        if (subs.isEmpty()) return;

        for (SellerOrder so : subs) {

            // ✅ Admin CANCEL qilsa hammasini CANCEL qilamiz
            if (newStatus == OrderStatus.CANCELED) {
                if (so.getStatus() != OrderStatus.CANCELED) {
                    so.setStatus(OrderStatus.CANCELED);
                    sellerOrderRepo.save(so);
                    sellerOrderHistoryService.addHistory(so, OrderStatus.CANCELED, changedBy);
                }
                continue;
            }

            // ✅ DELIVERED ham majburiy bo'lsin (xohlasangiz)
            if (newStatus == OrderStatus.DELIVERED) {
                if (so.getStatus() != OrderStatus.DELIVERED) {
                    so.setStatus(OrderStatus.DELIVERED);
                    sellerOrderRepo.save(so);
                    sellerOrderHistoryService.addHistory(so, OrderStatus.DELIVERED, changedBy);
                }
                continue;
            }

            // ✅ qolganlari uchun faqat oldinga
            if (rank(newStatus) < rank(so.getStatus())) continue;
            if (so.getStatus() == newStatus) continue;

            so.setStatus(newStatus);
            sellerOrderRepo.save(so);
            sellerOrderHistoryService.addHistory(so, newStatus, changedBy);
        }
    }

    @Transactional
    public void recalcMasterStatusFromSellerOrders(Long orderId) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        List<SellerOrder> subs = sellerOrderRepo.findByOrderId(orderId);
        if (subs.isEmpty()) return;

        boolean allCanceled = subs.stream().allMatch(s -> s.getStatus() == OrderStatus.CANCELED);
        boolean allDelivered = subs.stream().allMatch(s -> s.getStatus() == OrderStatus.DELIVERED);

        OrderStatus newMaster;

        if (allCanceled) {
            newMaster = OrderStatus.CANCELED;
        } else if (allDelivered) {
            newMaster = OrderStatus.DELIVERED;
        } else {
            // cancel bo'lmaganlar ichidan eng "kichik" progress master bo'lsin (ya'ni umumiy holat)
            // masalan biri PACKED biri SHIPPED => master PACKED
            OrderStatus min = subs.stream()
                    .filter(s -> s.getStatus() != OrderStatus.CANCELED)
                    .min((a, b) -> Integer.compare(rank(a.getStatus()), rank(b.getStatus())))
                    .map(SellerOrder::getStatus)
                    .orElse(OrderStatus.NEW);

            newMaster = min;
        }

        if (order.getStatus() != newMaster) {
            order.setStatus(newMaster);
            orderRepo.save(order);
            orderStatusHistoryService.log(order, newMaster);
        }
    }
}
