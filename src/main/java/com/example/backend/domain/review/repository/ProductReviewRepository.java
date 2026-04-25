package com.example.backend.domain.review.repository;

import com.example.backend.domain.order.entity.Order;
import com.example.backend.domain.product.entity.Product;
import com.example.backend.domain.review.entity.ProductReview;
import com.example.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findByProductIdAndActiveTrue(Long productId);

    boolean existsByUserAndProduct(User user, Product product);

    List<ProductReview> findByProductId(Long productId);

    boolean existsByUserAndProductAndOrder(User user, Product product, Order order);
    List<ProductReview> findByUserId(Long userId);


}
