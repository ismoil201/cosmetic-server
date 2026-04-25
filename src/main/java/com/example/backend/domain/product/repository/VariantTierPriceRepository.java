package com.example.backend.domain.product.repository;

import com.example.backend.domain.product.entity.VariantTierPrice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VariantTierPriceRepository extends JpaRepository<VariantTierPrice, Long> {

    Optional<VariantTierPrice> findByVariantIdAndMinQty(Long variantId, int minQty);

    @Query("""
        select t
        from VariantTierPrice t
        where t.variant.id = :variantId
          and t.minQty < :qty
        order by t.minQty desc
    """)
    List<VariantTierPrice> findBestTier(@Param("variantId") Long variantId,
                                        @Param("qty") int qty,
                                        Pageable pageable);

    @Query("""
        select t
        from VariantTierPrice t
        where t.variant.id = :variantId
        order by t.minQty asc
    """)
    List<VariantTierPrice> findAllByVariantIdOrderByMinQtyAsc(@Param("variantId") Long variantId);

    @Query("""
        select t
        from VariantTierPrice t
        where t.variant.id in :variantIds
        order by t.variant.id asc, t.minQty asc
    """)
    List<VariantTierPrice> findAllByVariantIdInOrderByVariantIdAscMinQtyAsc(
            @Param("variantIds") Collection<Long> variantIds
    );

    @Transactional
    void deleteByVariantIdIn(List<Long> variantIds);
}