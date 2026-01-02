package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.entity.*;
import com.example.backend.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepo;
    private final UserService userService;

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

    public List<AddressResponse> myAddresses() {
        User user = userService.getCurrentUser();

        return addressRepo.findByUserIdAndActiveTrue(user.getId())
                .stream()
                .map(this::map)
                .toList();
    }

    public Address getById(Long id) {
        return addressRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));
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

//    public Address getOwnedByCurrentUser(Long addressId) {
//        User user = userService.getCurrentUser();
//        Address a = addressRepo.findById(addressId)
//                .orElseThrow(() -> new RuntimeException("Address not found"));
//
//        if (!a.getUser().isActive() || !a.getUser().getId().equals(user.getId())) {
//            throw new RuntimeException("Address not allowed");
//        }
//        return a;
//    }



    public void delete(Long addressId) {
        User user = userService.getCurrentUser();

        Address a = addressRepo.findById(addressId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));

        // ⚠️ active null bo‘lishi mumkin bo‘lsa, NPE bo‘ladi. Shuning uchun Boolean.TRUE bilan tekshiramiz.
        boolean active = Boolean.TRUE.equals(a.getActive()); // getActive() bo‘lsa
        // agar entity'da primitive boolean bo‘lsa: boolean active = a.isActive();

        if (!active || !a.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Address not allowed");
        }

        a.setActive(false);
        addressRepo.save(a);
    }

    public Address getOwnedByCurrentUser(Long addressId) {
        User user = userService.getCurrentUser();
        Address a = addressRepo.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        // ❗ Agar active Boolean bo‘lsa, bu yerda NPE bo‘lishi mumkin:
        // if (!a.isActive() || ...)  <-- NPE
        // Shuning uchun ham bu funksiyani ham shunday yozing:
        if (!Boolean.TRUE.equals(a.getActive()) || !a.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Address not allowed");
        }
        return a;
    }

}
