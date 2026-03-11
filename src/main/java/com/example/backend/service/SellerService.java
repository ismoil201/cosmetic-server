package com.example.backend.service;

import com.example.backend.entity.Role;
import com.example.backend.entity.Seller;
import com.example.backend.entity.User;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.SellerRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerService {

    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public Seller requireCurrentSeller() {
        Long userId = currentUserService.requireUserId();
        return sellerRepository.findByOwnerUserId(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Sizda seller profili mavjud emas (owner_user_id=" + userId + ")"
                ));
    }

    public Long requireCurrentSellerId() {
        return requireCurrentSeller().getId();
    }

    public Seller requireSeller(Long sellerId) {
        return sellerRepository.findById(sellerId)
                .orElseThrow(() -> new NotFoundException("Seller topilmadi: " + sellerId));
    }

    @Transactional
    public Seller createSellerForUser(User user, String shopName) {

        // 1 user = 1 seller bo'lsa, oldin tekshir
        sellerRepository.findByOwnerUserId(user.getId()).ifPresent(s -> {
            throw new IllegalStateException("Bu user allaqachon seller: sellerId=" + s.getId());
        });

        Seller seller = new Seller();
        seller.setOwnerUser(user);
        seller.setName(shopName);
        seller.setStatus(Seller.SellerStatus.ACTIVE);

        seller = sellerRepository.save(seller);

        user.setRole(Role.SELLER);
        userRepository.save(user);

        return seller;
    }

    @Transactional(readOnly = true)
    public Seller getOfficialSeller() {

        // Platformaning default do‘koni (masalan: owner_user_id = 1)
        return sellerRepository.findByOwnerUserId(1L)
                .orElseThrow(() -> new IllegalStateException("Official seller not found"));
    }
}