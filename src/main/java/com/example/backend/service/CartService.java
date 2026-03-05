package com.example.backend.service;

import com.example.backend.dto.CartAddRequest;
import com.example.backend.dto.CartItemResponse;
import com.example.backend.dto.ProductImageResponse;
import com.example.backend.dto.ProductResponse;
import com.example.backend.entity.*;
import com.example.backend.repository.CartItemRepository;
import com.example.backend.repository.ProductImageRepository;
import com.example.backend.repository.ProductRepository;
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

        if (req.getQuantity() <= 0) throw new RuntimeException("Quantity must be greater than zero");

        ProductVariant v = variantRepo.findById(req.getVariantId())
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        Product product = v.getProduct();

        if (!v.isActive() || !product.isActive()) throw new RuntimeException("Product is not active");
        if (req.getQuantity() > v.getStock()) throw new RuntimeException("Not enough stock");

        cartRepo.findByUserIdAndVariantId(user.getId(), v.getId())
                .ifPresentOrElse(
                        item -> {
                            int newQty = item.getQuantity() + req.getQuantity();
                            if (newQty > v.getStock()) throw new RuntimeException("Not enough stock");
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

        // 1) cart itemlarni olamiz
        List<CartItem> list = cartRepo.findByUserId(user.getId());

        // 2) invalid/buzilgan variantli itemlarni topamiz (variant_id=0, null yoki missing proxy)
        List<CartItem> bad = list.stream()
                .filter(ci -> {
                    try {
                        ProductVariant v = ci.getVariant();
                        // variant null yoki id null/0 bo‘lsa invalid
                        return (v == null || v.getId() == null || v.getId() <= 0);
                    } catch (Exception e) {
                        // Hibernate proxy missing (Unable to find ProductVariant...) shu yerga tushadi
                        return true;
                    }
                })
                .toList();

        // 3) bularni o‘chirib tashlaymiz (shunda endpoint yiqilmaydi)
        if (!bad.isEmpty()) {
            cartRepo.deleteAll(bad);
            // qaytadan toza listni olamiz
            list = cartRepo.findByUserId(user.getId());
        }

        // 4) normal map
        return list.stream()
                .map(c -> {

                    ProductVariant v = c.getVariant(); // endi xavfsiz
                    Product p = v.getProduct();

                    // main image
                    String imageUrl = productImageRepo
                            .findFirstByProductIdAndMainTrue(p.getId())
                            .map(ProductImage::getImageUrl)
                            .orElse(null);

                    BigDecimal unitPrice = pricingService.unitPrice(v);
                    BigDecimal lineTotal = pricingService.lineTotal(v, c.getQuantity());

                    ProductResponse pr = new ProductResponse(
                            p.getId(),
                            p.getName(),
                            p.getBrand(),
                            v.getPrice(),
                            unitPrice, // ✅ variant final unit
                            p.getCategory(),
                            p.getRatingAvg(),
                            p.getReviewCount(),
                            p.getSoldCount(),
                            p.isTodayDeal(),
                            false,
                            v.getStock(), // ✅ variant stock
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
        if (quantity > v.getStock()) throw new RuntimeException("Not enough stock");

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

    /* ================= CLEAR CART (ORDER SUCCESS) ================= */

    @Transactional
    public void clearMyCart() {
        User user = userService.getCurrentUser();
        cartRepo.deleteByUserId(user.getId());
    }
}
