package com.example.backend.controller;

import com.example.backend.dto.CartAddRequest;
import com.example.backend.dto.CartItemResponse;
import com.example.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // ADD
    @PostMapping
    public ResponseEntity<?> add(@RequestBody CartAddRequest req) {
        cartService.add(req);
        return ResponseEntity.ok("Added");
    }

    // LIST
    @GetMapping
    public List<CartItemResponse> list() {
        return cartService.getMyCart();
    }

    // UPDATE QTY
    @PutMapping("/{cartItemId}")
    public ResponseEntity<?> updateQty(
            @PathVariable Long cartItemId,
            @RequestParam int quantity
    ) {
        cartService.updateQuantity(cartItemId, quantity);
        return ResponseEntity.ok("Updated");
    }

    // DELETE
    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<?> delete(@PathVariable Long cartItemId) {
        cartService.delete(cartItemId);
        return ResponseEntity.ok("Deleted");
    }
}

