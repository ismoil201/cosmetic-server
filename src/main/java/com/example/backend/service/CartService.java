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

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartRepo;
    private final ProductRepository productRepo;
    private final UserService userService;
    private final ProductImageRepository productImageRepo;


    public void add(CartAddRequest req) {
        User user = userService.getCurrentUser();

        Product product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        cartRepo.findByUserIdAndProductId(user.getId(), product.getId())
                .ifPresentOrElse(
                        item -> {
                            item.setQuantity(item.getQuantity() + req.getQuantity());
                            cartRepo.save(item);
                        },
                        () -> {
                            CartItem item = new CartItem();
                            item.setUser(user);
                            item.setProduct(product);
                            item.setQuantity(req.getQuantity());
                            cartRepo.save(item);
                        }
                );


    }

    public List<CartItemResponse> getMyCart() {
        User user = userService.getCurrentUser();

        return cartRepo.findByUserId(user.getId())
                .stream()
                .map(c -> {

                    String imageUrl = productImageRepo
                            .findByProductIdAndMainTrue(c.getProduct().getId())
                            .map(ProductImage::getImageUrl)
                            .orElse(null);

                    return new CartItemResponse(
                            c.getId(),
                            new ProductResponse(
                                    c.getProduct().getId(),
                                    c.getProduct().getName(),
                                    c.getProduct().getBrand(),
                                    c.getProduct().getPrice(),
                                    c.getProduct().getDiscountPrice(),
                                    c.getProduct().getCategory(),
                                    c.getProduct().getRatingAvg(),     // 🔥 QO‘SHILDI
                                    c.getProduct().getReviewCount(),   // 🔥 QO‘SHILDI
                                    c.getProduct().getSoldCount(),      // 🔥
                                    c.getProduct().isTodayDeal(),       // 🔥
                                    false,
                                    List.of(
                                            new ProductImageResponse(imageUrl, true)
                                    )
                            )
                            ,
                            c.getQuantity()
                    );
                })
                .toList();
    }

    public void updateQuantity(Long cartItemId, int quantity) {
        User user = userService.getCurrentUser();

        CartItem item = cartRepo.findById(cartItemId)
                .filter(c -> c.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Access denied"));

        item.setQuantity(quantity);
        cartRepo.save(item);
    }

    public void delete(Long cartItemId) {
        cartRepo.deleteById(cartItemId);
    }
}

