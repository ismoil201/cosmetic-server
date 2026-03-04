package com.example.backend.service;

import com.example.backend.dto.AddressCreateRequest;
import com.example.backend.dto.AddressResponse;
import com.example.backend.entity.Address;
import com.example.backend.entity.User;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepo;
    private final UserService userService;

    @Transactional
    public AddressResponse create(AddressCreateRequest req) {

        User user = userService.getCurrentUser();

        Address address = new Address();
        address.setUser(user);
        address.setTitle(req.getTitle());
        address.setAddress(req.getAddress());
        address.setLatitude(req.getLatitude());
        address.setLongitude(req.getLongitude());

        address = addressRepo.save(address);

        return map(address);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> myAddresses() {
        User user = userService.getCurrentUser();

        return addressRepo.findByUserIdAndActiveTrue(user.getId())
                .stream()
                .map(this::map)
                .toList();
    }

    /**
     * General getById (ADMIN yoki internal ishlatish uchun).
     * Public API bo‘lsa ownership tekshiruvini getOwnedByCurrentUser() bilan qiling.
     */
    @Transactional(readOnly = true)
    public Address getById(Long id) {
        return addressRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Address not found"));
    }

    /**
     * ✅ IDOR-safe: faqat current userga tegishli va active bo‘lgan addressni qaytaradi.
     * NotFoundException ishlatamiz: address bor/yo‘qligini oshkor qilmaydi.
     */
    @Transactional(readOnly = true)
    public Address getOwnedByCurrentUser(Long addressId) {
        User user = userService.getCurrentUser();

        Address a = addressRepo.findById(addressId)
                .orElseThrow(() -> new NotFoundException("Address not found"));

        boolean active = isActiveTrue(a);

        // address boshqa userniki yoki inactive bo‘lsa -> "topilmadi" (IDOR best practice)
        if (!active || a.getUser() == null || !a.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Address not found");
        }

        return a;
    }

    /**
     * Soft delete (faqat owner)
     */
    @Transactional
    public void delete(Long addressId) {
        User user = userService.getCurrentUser();

        Address a = addressRepo.findById(addressId)
                .orElseThrow(() -> new NotFoundException("Address not found"));

        boolean active = isActiveTrue(a);

        // Agar boshqa userniki bo‘lsa -> 404 (IDOR)
        if (!active || a.getUser() == null || !a.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Address not found");
        }

        a.setActive(false);
        addressRepo.save(a);
    }

    private AddressResponse map(Address a) {
        return new AddressResponse(
                a.getId(),
                a.getTitle(),
                a.getAddress(),
                a.getLatitude(),
                a.getLongitude()
        );
    }

    /**
     * Address.active primitive boolean bo‘lsa ham, Boolean bo‘lsa ham NPE bo‘lmasin.
     * - Agar entity’da boolean active bo‘lsa: a.isActive()
     * - Agar entity’da Boolean active bo‘lsa: Boolean.TRUE.equals(a.getActive())
     *
     * Sizda qaysi biri ekanini bilmaganim uchun universal qilib yozdim.
     */
    private boolean isActiveTrue(Address a) {
        try {
            // Boolean getter bo‘lsa
            //noinspection RedundantCast
            Boolean v = (Boolean) Address.class.getMethod("getActive").invoke(a);
            return Boolean.TRUE.equals(v);
        } catch (Exception ignored) {
            // primitive boolean bo‘lsa
            try {
                //noinspection RedundantCast
                boolean v = (boolean) Address.class.getMethod("isActive").invoke(a);
                return v;
            } catch (Exception e) {
                // Agar umuman active field bo‘lmasa — xavfsiz tomonga: false
                return false;
            }
        }
    }
}