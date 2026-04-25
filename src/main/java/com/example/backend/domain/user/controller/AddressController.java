package com.example.backend.domain.user.controller;

import com.example.backend.global.response.SimpleResponse;
import com.example.backend.domain.user.dto.AddressCreateRequest;
import com.example.backend.domain.user.dto.AddressResponse;
import com.example.backend.domain.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public AddressResponse create(@RequestBody AddressCreateRequest req) {
        return addressService.create(req);
    }

    @GetMapping
    public List<AddressResponse> myAddresses() {
        return addressService.myAddresses();
    }

    @DeleteMapping("/{id}")
    public SimpleResponse delete(@PathVariable Long id) {
        addressService.delete(id);
        return new SimpleResponse(true, "Deleted", null);
    }
}

