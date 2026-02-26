package com.example.backend.service;

import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    /**
     * TODO: bu joyni sizning Security'ingizga moslab yozing.
     * Masalan: SecurityContextHolder.getContext().getAuthentication().getPrincipal()
     */
    public Long requireUserId() {
        throw new IllegalStateException("CurrentUserService.requireUserId() ni Security'ga moslab implement qiling");
    }
}