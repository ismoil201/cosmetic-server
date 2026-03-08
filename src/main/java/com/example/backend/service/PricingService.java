package com.example.backend.service;

import com.example.backend.entity.ProductVariant;
import com.example.backend.entity.VariantTierPrice;
import com.example.backend.repository.VariantTierPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final VariantTierPriceRepository tierRepo;

    /**
     * Variantning oddiy bazaviy narxi:
     * discountPrice bo‘lsa o‘sha, bo‘lmasa price
     */
    public BigDecimal baseUnitPrice(ProductVariant v) {
        if (v.getDiscountPrice() != null && v.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0) {
            return v.getDiscountPrice();
        }
        return v.getPrice();
    }

    /**
     * Final line total:
     * 1) exact tier bo‘lsa exact totalPrice
     * 2) exact tier bo‘lmasa, qty dan kichik eng katta tier topiladi
     *    va qolgan dona effective unit bo‘yicha qo‘shiladi
     * 3) umuman tier bo‘lmasa baseUnit * qty
     */
    public BigDecimal lineTotal(ProductVariant v, int qty) {
        if (qty <= 0) return BigDecimal.ZERO;

        BigDecimal baseUnit = baseUnitPrice(v);

        // 1) exact tier qidiramiz
        Optional<VariantTierPrice> exactTier = tierRepo.findByVariantIdAndMinQty(v.getId(), qty);
        if (exactTier.isPresent()) {
            return exactTier.get().getTotalPrice();
        }

        // 2) qty dan kichik eng katta tierni topamiz
        var tiers = tierRepo.findBestTier(v.getId(), qty, PageRequest.of(0, 1));

        if (!tiers.isEmpty()) {
            VariantTierPrice bestTier = tiers.get(0);

            int tierQty = bestTier.getMinQty();
            BigDecimal tierTotal = bestTier.getTotalPrice();

            BigDecimal effectiveTierUnit = tierTotal.divide(
                    BigDecimal.valueOf(tierQty),
                    2,
                    RoundingMode.HALF_UP
            );

            int remainQty = qty - tierQty;

            return tierTotal.add(
                    effectiveTierUnit.multiply(BigDecimal.valueOf(remainQty))
            );
        }

        // 3) umuman tier bo‘lmasa oddiy pricing
        return baseUnit.multiply(BigDecimal.valueOf(qty));
    }

    /**
     * Savatdagi current quantity uchun effective unit price
     * Misol:
     * qty=2, total=248000 => unit=124000
     */
    public BigDecimal unitPrice(ProductVariant v, int qty) {
        if (qty <= 0) return BigDecimal.ZERO;

        return lineTotal(v, qty).divide(
                BigDecimal.valueOf(qty),
                2,
                RoundingMode.HALF_UP
        );
    }
}