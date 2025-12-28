package com.example.backend.service;

import com.example.backend.dto.CartAddRequest;
import com.example.backend.dto.CartItemResponse;
import com.example.backend.dto.ProductImageResponse;
import com.example.backend.dto.ProductResponse;
import com.example.backend.entity.CartItem;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductImage;
import com.example.backend.entity.User;
import com.example.backend.repository.CartItemRepository;
import com.example.backend.repository.ProductImageRepository;
import com.example.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartRepo;
    private final ProductRepository productRepo;
    private final ProductImageRepository productImageRepo;
    private final UserService userService;

    /* ================= ADD TO CART ================= */

    @Transactional
    public void add(CartAddRequest req) {

        User user = userService.getCurrentUser();

        if (req.getQuantity() <= 0) {
            throw new RuntimeException("Quantity must be greater than zero");
        }

        Product product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.isActive()) {
            throw new RuntimeException("Product is not active");
        }

        cartRepo.findByUserIdAndProductId(user.getId(), product.getId())
                .ifPresentOrElse(
                        item -> {
                            int newQty = item.getQuantity() + req.getQuantity();

                            if (newQty > product.getStock()) {
                                throw new RuntimeException("Not enough stock");
                            }

                            item.setQuantity(newQty);
                            cartRepo.save(item);
                        },
                        () -> {
                            if (req.getQuantity() > product.getStock()) {
                                throw new RuntimeException("Not enough stock");
                            }

                            CartItem item = new CartItem();
                            item.setUser(user);
                            item.setProduct(product);
                            item.setQuantity(req.getQuantity());
                            cartRepo.save(item);
                        }
                );
    }

    /* ================= GET MY CART ================= */

    @Transactional(readOnly = true)
    public List<CartItemResponse> getMyCart() {

        User user = userService.getCurrentUser();

        return cartRepo.findByUserId(user.getId())
                .stream()
                .map(c -> {

                    Product p = c.getProduct();

                    // 🔥 MAIN IMAGE
                    String imageUrl = productImageRepo
                            .findByProductIdAndMainTrue(p.getId())
                            .map(ProductImage::getImageUrl)
                            .orElse(null);

                    double finalPrice = p.getDiscountPrice() > 0
                            ? p.getDiscountPrice()
                            : p.getPrice();

                    return new CartItemResponse(
                            c.getId(),
                            new ProductResponse(
                                    p.getId(),
                                    p.getName(),
                                    p.getBrand(),
                                    p.getPrice(),
                                    finalPrice,
                                    p.getCategory(),
                                    p.getRatingAvg(),
                                    p.getReviewCount(),
                                    p.getSoldCount(),
                                    p.isTodayDeal(),
                                    false, // favorite (cartda shart emas)
                                    List.of(new ProductImageResponse(imageUrl, true))
                            ),
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

        if (quantity > item.getProduct().getStock()) {
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

    /* ================= CLEAR CART (ORDER SUCCESS) ================= */

    @Transactional
    public void clearMyCart() {
        User user = userService.getCurrentUser();
        cartRepo.deleteByUserId(user.getId());
    }
}
