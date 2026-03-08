package com.example.backend.service;

import com.example.backend.dto.CartAddRequest;
import com.example.backend.dto.CartItemResponse;
import com.example.backend.dto.ProductImageResponse;
import com.example.backend.dto.ProductResponse;
import com.example.backend.entity.CartItem;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductImage;
import com.example.backend.entity.ProductVariant;
import com.example.backend.entity.User;
import com.example.backend.repository.CartItemRepository;
import com.example.backend.repository.ProductImageRepository;
import com.example.backend.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartRepo;
    private final ProductVariantRepository variantRepo;
    private final ProductImageRepository productImageRepo;
    private final UserService userService;
    private final PricingService pricingService;

    /* ================= ADD TO CART ================= */

    @Transactional
    public void add(CartAddRequest req) {

        User user = userService.getCurrentUser();

        if (req.getQuantity() <= 0) {
            throw new RuntimeException("Quantity must be greater than zero");
        }

        ProductVariant v = variantRepo.findById(req.getVariantId())
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        Product product = v.getProduct();

        if (!v.isActive() || !product.isActive()) {
            throw new RuntimeException("Product is not active");
        }

        if (req.getQuantity() > v.getStock()) {
            throw new RuntimeException("Not enough stock");
        }

        cartRepo.findByUserIdAndVariantId(user.getId(), v.getId())
                .ifPresentOrElse(
                        item -> {
                            int newQty = item.getQuantity() + req.getQuantity();

                            if (newQty > v.getStock()) {
                                throw new RuntimeException("Not enough stock");
                            }

                            item.setQuantity(newQty);
                            cartRepo.save(item);
                        },
                        () -> {
                            CartItem item = new CartItem();
                            item.setUser(user);
                            item.setVariant(v);
                            item.setQuantity(req.getQuantity());
                            cartRepo.save(item);
                        }
                );
    }

    /* ================= GET MY CART ================= */

    @Transactional
    public List<CartItemResponse> getMyCart() {

        User user = userService.getCurrentUser();

        // 1) user cart itemlari
        List<CartItem> list = cartRepo.findByUserId(user.getId());

        // 2) invalid / buzilgan itemlarni topamiz
        List<CartItem> bad = list.stream()
                .filter(ci -> {
                    try {
                        ProductVariant v = ci.getVariant();
                        return (v == null || v.getId() == null || v.getId() <= 0);
                    } catch (Exception e) {
                        return true;
                    }
                })
                .toList();

        // 3) invalid itemlarni o‘chiramiz
        if (!bad.isEmpty()) {
            cartRepo.deleteAll(bad);
            list = cartRepo.findByUserId(user.getId());
        }

        // 4) response map
        return list.stream()
                .map(c -> {
                    ProductVariant v = c.getVariant();
                    Product p = v.getProduct();

                    String imageUrl = productImageRepo
                            .findFirstByProductIdAndMainTrue(p.getId())
                            .map(ProductImage::getImageUrl)
                            .orElse(null);

                    // ProductResponse uchun bazaviy discount price
                    BigDecimal baseDiscountPrice = pricingService.baseUnitPrice(v);

                    // Cart uchun effective unit price
                    BigDecimal unitPrice = pricingService.unitPrice(v, c.getQuantity());

                    // Cart uchun final total
                    BigDecimal lineTotal = pricingService.lineTotal(v, c.getQuantity());

                    ProductResponse pr = new ProductResponse(
                            p.getId(),
                            p.getName(),
                            p.getBrand(),
                            v.getPrice(),
                            baseDiscountPrice,
                            p.getCategory(),
                            p.getRatingAvg(),
                            p.getReviewCount(),
                            p.getSoldCount(),
                            p.isTodayDeal(),
                            false,
                            v.getStock(),
                            List.of(new ProductImageResponse(imageUrl, true))
                    );

                    return new CartItemResponse(
                            c.getId(),
                            pr,
                            v.getId(),
                            v.getLabel(),
                            unitPrice,
                            lineTotal,
                            c.getQuantity()
                    );
                })
                .toList();
    }

    /* ================= UPDATE QUANTITY ================= */

    @Transactional
    public void updateQuantity(Long cartItemId, int quantity) {

        User user = userService.getCurrentUser();

        CartItem item = cartRepo.findById(cartItemId)
                .filter(c -> c.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Access denied"));

        if (quantity <= 0) {
            cartRepo.delete(item);
            return;
        }

        ProductVariant v = item.getVariant();

        if (quantity > v.getStock()) {
            throw new RuntimeException("Not enough stock");
        }

        item.setQuantity(quantity);
        cartRepo.save(item);
    }

    /* ================= DELETE CART ITEM ================= */

    @Transactional
    public void delete(Long cartItemId) {

        User user = userService.getCurrentUser();

        CartItem item = cartRepo.findById(cartItemId)
                .filter(c -> c.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Access denied"));

        cartRepo.delete(item);
    }

    /* ================= CLEAR CART ================= */

    @Transactional
    public void clearMyCart() {
        User user = userService.getCurrentUser();
        cartRepo.deleteByUserId(user.getId());
    }
}