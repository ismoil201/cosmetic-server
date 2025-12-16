package com.example.backend.service;

import com.example.backend.dto.CartAddRequest;
import com.example.backend.dto.CartItemResponse;
import com.example.backend.dto.ProductResponse;
import com.example.backend.entity.CartItem;
import com.example.backend.entity.Product;
import com.example.backend.entity.User;
import com.example.backend.repository.CartRepository;
import com.example.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepo;
    private final ProductRepository productRepo;
    private final UserService userService;

    public void add(CartAddRequest req) {
        User user = userService.getCurrentUser();
        Product product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        cartRepo.findByUserAndProduct(user, product)
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

        return cartRepo.findByUser(user)
                .stream()
                .map(c -> new CartItemResponse(
                        c.getId(),
                        new ProductResponse(
                                c.getProduct().getId(),
                                c.getProduct().getName(),
                                c.getProduct().getBrand(),
                                c.getProduct().getPrice(),
                                c.getProduct().getDiscountPrice(),
                                c.getProduct().getImageUrl(),
                                c.getProduct().getCategory(),
                                false
                        ),
                        c.getQuantity()
                ))
                .toList();
    }

    // ✅ YO‘Q EDI
    public void updateQuantity(Long cartItemId, int quantity) {
        User user = userService.getCurrentUser();
        CartItem item = cartRepo.findById(cartItemId)
                .filter(c -> c.getUser().equals(user))
                .orElseThrow(() -> new RuntimeException("Access denied"));

        cartRepo.save(item);
    }

    // ✅ YO‘Q EDI
    public void delete(Long cartItemId) {
        cartRepo.deleteById(cartItemId);
    }
}

