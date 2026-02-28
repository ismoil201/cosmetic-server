package com.example.backend.service;

import com.example.backend.entity.OrderStatus;
import com.example.backend.entity.SellerOrder;
import com.example.backend.entity.User;
import com.example.backend.repository.SellerOrderRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional
public class SellerOrderService {

    private final SellerOrderRepository sellerOrderRepository;
    private final SellerService sellerService;
    private final SellerOrderStatusHistoryService historyService;

    // Agar changed_by ni aniq yozmoqchi bo'lsangiz:
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository; // sizda bo'lsa

    @Transactional(readOnly = true)
    public Page<SellerOrder> mySellerOrders(Pageable pageable) {
        Long sellerId = sellerService.requireCurrentSellerId();
        return sellerOrderRepository.findBySellerIdOrderByCreatedAtDesc(sellerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<SellerOrder> mySellerOrdersByStatus(OrderStatus status, Pageable pageable) {
        Long sellerId = sellerService.requireCurrentSellerId();
        return sellerOrderRepository.findBySellerIdAndStatusOrderByCreatedAtDesc(sellerId, status, pageable);
    }

    @Transactional(readOnly = true)
    public SellerOrder getMySellerOrder(Long sellerOrderId) {
        Long sellerId = sellerService.requireCurrentSellerId();

        SellerOrder so = sellerOrderRepository.findById(sellerOrderId)
                .orElseThrow(() -> new IllegalArgumentException("SellerOrder topilmadi: " + sellerOrderId));

        if (!so.getSeller().getId().equals(sellerId)) {
            throw new AccessDeniedException("Bu buyurtma sizga tegishli emas");
        }
        return so;
    }

    /**
     * Seller/Admin status update qiladi:
     * 1) seller_orders.status update
     * 2) seller_order_status_history insert
     * 3) (ixtiyoriy) master orders.status recalc
     */
    public SellerOrder updateMySellerOrderStatus(Long sellerOrderId, OrderStatus  newStatus) {
        SellerOrder so = getMySellerOrder(sellerOrderId);

        // status o'zgarish qoidasi (xohlasangiz kuchaytiramiz)
        validateTransition(so.getStatus(), newStatus);

        so.setStatus(newStatus);
        SellerOrder saved = sellerOrderRepository.save(so);

        // changed_by ni yozamiz (user topilmasa null qoldirsa ham bo'ladi)
        User changedBy = null;
        try {
            Long userId = currentUserService.requireUserId();
            changedBy = userRepository.findById(userId).orElse(null);
        } catch (Exception ignored) {
            // current user implement qilinmagan bo'lsa ham yiqilmasin
        }

        historyService.addHistory(saved, newStatus, changedBy);

        // optional: master order statusni qayta hisoblash
        // recalcMasterOrderStatus(saved.getOrder().getId());

        return saved;
    }

    private void validateTransition(OrderStatus from, OrderStatus to) {
        if (from == to) return;

        // Minimal qoidalar:
        // CANCELED yoki DELIVERED bo'lsa, endi o'zgarmasin
        if (from == OrderStatus.CANCELED || from == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Bu statusdan keyin o'zgartirib bo'lmaydi: " + from);
        }

        // NEW -> CONFIRMED/CANCELED
        // CONFIRMED -> PACKED/CANCELED
        // PACKED -> SHIPPED/CANCELED
        // SHIPPED -> DELIVERED
        switch (from) {
            case NEW -> {
                if (!(to == OrderStatus.CONFIRMED || to == OrderStatus.CANCELED))
                    throw new IllegalStateException("NEW dan faqat CONFIRMED yoki CANCELED o'tadi");
            }
            case CONFIRMED -> {
                if (!(to == OrderStatus.PACKED || to == OrderStatus.CANCELED))
                    throw new IllegalStateException("CONFIRMED dan faqat PACKED yoki CANCELED o'tadi");
            }
            case PACKED -> {
                if (!(to == OrderStatus.SHIPPED || to == OrderStatus.CANCELED))
                    throw new IllegalStateException("PACKED dan faqat SHIPPED yoki CANCELED o'tadi");
            }
            case SHIPPED -> {
                if (to != OrderStatus.DELIVERED)
                    throw new IllegalStateException("SHIPPED dan faqat DELIVERED o'tadi");
            }
            default -> {
                // NEW, CONFIRMED, PACKED, SHIPPED dan tashqari holatlar
            }
        }
    }

    /**
     * Agar master order statusni seller_orders dan avtomatik hisoblamoqchi bo'lsangiz:
     * - hamma DELIVERED => COMPLETED
     * - bitta SHIPPED/PACKED/... => IN_PROGRESS
     * - hammasi CANCELED => CANCELED
     * - aralash cancel + delivered => PARTIALLY_CANCELED
     *
     * Bu funksiyani keyin OrderService bilan chiroyli qilib ulab beraman.
     */
    // private void recalcMasterOrderStatus(Long orderId) { ... }
}