package com.example.backend.domain.product.repository;

import com.example.backend.domain.product.entity.ProductVariant;
import com.example.backend.domain.product.entity.VariantTierPrice;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductIdAndActiveTrueOrderBySortOrderAscIdAsc(Long productId);

    @Query("""
      select t from VariantTierPrice t
      where t.variant.id = :variantId
        and t.minQty <= :qty
      order by t.minQty desc
    """)
    List<VariantTierPrice> findBestTier(@Param("variantId") Long variantId,
                                        @Param("qty") int qty,
                                        Pageable pageable);


    List<ProductVariant> findByProductId(Long productId);

    void deleteByProductId(Long productId);

    /**
     * 🔒 CRITICAL: Pessimistic write lock for stock updates
     * Prevents race conditions when multiple users order the same variant
     *
     * Usage: Order creation must use this method to safely decrement stock
     * Lock is held until transaction commits
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ProductVariant v where v.id = :id")
    Optional<ProductVariant> findByIdForUpdate(@Param("id") Long id);
}