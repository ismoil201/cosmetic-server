package com.example.backend.domain.cart.controller;

import com.example.backend.domain.cart.dto.CartAddRequest;
import com.example.backend.domain.cart.dto.CartItemResponse;
import com.example.backend.domain.cart.dto.CartUpdateRequest;
import com.example.backend.domain.cart.service.CartService;
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
            @RequestBody CartUpdateRequest req
    ) {
        cartService.updateQuantity(cartItemId, req.getQuantity());
        return "Updated";
    }


    @DeleteMapping("/{cartItemId}")
    public String delete(@PathVariable Long cartItemId) {
        cartService.delete(cartItemId);
        return "Deleted";
    }
}
