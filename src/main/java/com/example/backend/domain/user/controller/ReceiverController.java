package com.example.backend.domain.user.controller;

import com.example.backend.domain.user.dto.ReceiverCreateRequest;
import com.example.backend.domain.user.dto.ReceiverResponse;
import com.example.backend.global.response.SimpleResponse;
import com.example.backend.domain.user.service.ReceiverService;
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

    @DeleteMapping("/{id}")
    public SimpleResponse delete(@PathVariable Long id) {
        receiverService.delete(id);
        return new SimpleResponse(true, "Receiver deleted", null);
    }
}
