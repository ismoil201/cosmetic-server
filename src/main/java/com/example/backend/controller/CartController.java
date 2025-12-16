package com.example.backend.controller;

import com.example.backend.dto.CartAddRequest;
import com.example.backend.dto.CartItemResponse;
import com.example.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CartController {

    private final CartService cartService;

    @PostMapping
    public String add(@RequestBody CartAddRequest req) {
        cartService.add(req);
        return "Added";
    }

    @GetMapping
    public List<CartItemResponse> list() {
        return cartService.getMyCart();
    }

    @PutMapping("/{cartItemId}")
    public String updateQty(
            @PathVariable Long cartItemId,
            @RequestParam int quantity
    ) {
        cartService.updateQuantity(cartItemId, quantity);
        return "Updated";
    }

    @DeleteMapping("/{cartItemId}")
    public String delete(@PathVariable Long cartItemId) {
        cartService.delete(cartItemId);
        return "Deleted";
    }
}
