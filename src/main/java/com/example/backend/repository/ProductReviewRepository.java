package com.example.backend.repository;

import com.example.backend.entity.Product;
import com.example.backend.entity.ProductReview;
import com.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findByProductIdAndActiveTrue(Long productId);

    boolean existsByUserAndProduct(User user, Product product);

    List<ProductReview> findByProductId(Long productId);

}
