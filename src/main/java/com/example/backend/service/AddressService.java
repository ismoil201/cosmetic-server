package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.entity.*;
import com.example.backend.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public Address getOwnedByCurrentUser(Long addressId) {
        User user = userService.getCurrentUser();
        Address a = addressRepo.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!a.isActive() || !a.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Address not allowed");
        }
        return a;
    }


}
