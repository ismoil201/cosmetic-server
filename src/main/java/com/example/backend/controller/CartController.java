package com.example.backend.controller;

import com.example.backend.dto.CartRequest;
import com.example.backend.entity.CartItem;
import com.example.backend.entity.Product;
import com.example.backend.entity.User;
import com.example.backend.repository.CartRepository;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartRepository cartRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;

    // ➤ 1) CARTGA QO‘SHISH
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody CartRequest req) {

        User user = userRepo.findById(req.getUserId()).orElseThrow();
        Product product = productRepo.findById(req.getProductId()).orElseThrow();

        CartItem item = new CartItem();
        item.setUser(user);
        item.setProduct(product);
        item.setQuantity(req.getQuantity());

        return ResponseEntity.ok(cartRepo.save(item));
    }

    // ➤ 2) USER CART LIST
    @GetMapping("/{userId}")
    public List<CartItem> getUserCart(@PathVariable Long userId) {
        User user = userRepo.findById(userId).orElseThrow();
        return cartRepo.findByUser(user);
    }

    // ➤ 3) QUANTITY UPDATE
    @PutMapping("/{cartId}")
    public ResponseEntity<?> updateQuantity(@PathVariable Long cartId,
                                            @RequestParam int quantity) {

        CartItem item = cartRepo.findById(cartId).orElseThrow();
        item.setQuantity(quantity);

        return ResponseEntity.ok(cartRepo.save(item));
    }

    // ➤ 4) CARTDAN O‘CHIRISH
    @DeleteMapping("/{cartId}")
    public ResponseEntity<?> deleteCartItem(@PathVariable Long cartId) {
        cartRepo.deleteById(cartId);
        return ResponseEntity.ok("Deleted");
    }
}
