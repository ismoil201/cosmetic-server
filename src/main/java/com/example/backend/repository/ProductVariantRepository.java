package com.example.backend.repository;

import com.example.backend.entity.ProductVariant;
import com.example.backend.entity.VariantTierPrice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
}