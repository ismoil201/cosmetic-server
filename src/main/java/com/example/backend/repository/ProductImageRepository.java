package com.example.backend.repository;

import com.example.backend.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductId(Long productId);

    void deleteByProductId(Long productId);

    List<ProductImage> findByProductIdOrderByMainDescIdAsc(Long productId);

    Optional<ProductImage> findFirstByProductIdAndMainTrue(Long productId);
    Optional<ProductImage> findFirstByProductIdOrderByIdAsc(Long productId);

    List<ProductImage> findByProductIdInOrderByMainDescIdAsc(List<Long> productIds);


    @Query("""
select pi from ProductImage pi
where pi.main = true and pi.product.id in :ids
""")
    List<ProductImage> findMainImagesByProductIds(@Param("ids") List<Long> ids);
}

