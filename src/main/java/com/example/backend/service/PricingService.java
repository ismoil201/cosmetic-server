package com.example.backend.service;

import com.example.backend.entity.ProductVariant;
import com.example.backend.repository.VariantTierPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final VariantTierPriceRepository tierRepo;


    public BigDecimal calcLineTotal(ProductVariant v, int qty) {
        if (qty <= 0) return BigDecimal.ZERO;

        // 1) tier bormi?
        var tiers = tierRepo.findBestTier(v.getId(), qty, PageRequest.of(0,1));
        if (!tiers.isEmpty()) {
            return tiers.get(0).getTotalPrice();
        }

        // 2) tier bo‘lmasa default price*qty
        BigDecimal unit = finalUnitPrice(v);
        return unit.multiply(BigDecimal.valueOf(qty));
    }

    public BigDecimal finalUnitPrice(ProductVariant v) {
        if (v.getDiscountPrice() != null && v.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0) {
            return v.getDiscountPrice();
        }
        return v.getPrice();
    }


    public BigDecimal unitPrice(ProductVariant v) {
        if (v.getDiscountPrice() != null && v.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0) {
            return v.getDiscountPrice();
        }
        return v.getPrice();
    }

    /** qty bo‘yicha line total (tier bo‘lsa totalPrice, bo‘lmasa unit*qty) */
    public BigDecimal lineTotal(ProductVariant v, int qty) {
        if (qty <= 0) return BigDecimal.ZERO;

        var tiers = tierRepo.findBestTier(v.getId(), qty, PageRequest.of(0, 1));
        if (!tiers.isEmpty()) return tiers.get(0).getTotalPrice();

        return unitPrice(v).multiply(BigDecimal.valueOf(qty));
    }
}