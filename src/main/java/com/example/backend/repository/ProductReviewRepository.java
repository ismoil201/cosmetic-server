package com.example.backend.repository;

import com.example.backend.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findByProductIdAndActiveTrue(Long productId);

    List<ProductReview> findByProductId(Long productId);

}
