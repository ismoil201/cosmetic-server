package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.service.ReceiverService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/receivers")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ReceiverController {

    private final ReceiverService receiverService;

    @PostMapping
    public ReceiverResponse create(@RequestBody ReceiverCreateRequest req) {
        return receiverService.create(req);
    }

    @GetMapping
    public List<ReceiverResponse> myReceivers() {
        return receiverService.myReceivers();
    }
}
