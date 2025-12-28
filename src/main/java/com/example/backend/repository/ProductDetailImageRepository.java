package com.example.backend.repository;

import com.example.backend.entity.ProductDetailImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductDetailImageRepository
        extends JpaRepository<ProductDetailImage, Long> {

    void deleteByProductId(Long productId);


    List<ProductDetailImage> findByProductIdOrderBySortOrderAsc(Long productId);
}
