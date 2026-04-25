package com.example.backend.global.exception;

/**
 * Thrown when product variant stock is insufficient for order creation
 *
 * Use cases:
 * - User tries to order quantity > available stock
 * - Concurrent orders cause race condition (mitigated by PESSIMISTIC_WRITE lock)
 *
 * HTTP Response: 409 CONFLICT
 * Error Code: OUT_OF_STOCK
 *
 * Example:
 * throw new InsufficientStockException("Not enough stock: Moisturizer 50ml");
 */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
