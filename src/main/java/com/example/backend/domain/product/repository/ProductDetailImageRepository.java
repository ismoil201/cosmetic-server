package com.example.backend.domain.product.repository;

import com.example.backend.domain.product.entity.ProductDetailImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ProductDetailImageRepository
        extends JpaRepository<ProductDetailImage, Long> {

    @Modifying
    @Transactional
    void deleteByProductId(Long productId);


    List<ProductDetailImage> findByProductIdOrderBySortOrderAsc(Long productId);
}
