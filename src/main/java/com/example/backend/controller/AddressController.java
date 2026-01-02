package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.service.AddressService;
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

