package com.example.backend.controller;

import com.example.backend.dto.EventRequest;
import com.example.backend.entity.EventType;
import com.example.backend.service.EventService;
import com.example.backend.service.EventTrackingService;
import com.example.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;
    private final EventTrackingService trackingService;

    private final UserService userService;


    @PostMapping("/impression")
    public void impression(@RequestBody EventRequest req) {
        eventService.log(EventType.IMPRESSION, req.getProductId(), req.getScreen(), req.getPosition(), req.getQueryText(), req.getSessionId());
    }

    @PostMapping("/click")
    public Map<String, Object> click(@RequestParam Long productId) {
        var user = userService.getCurrentUserOrNull();
        if (user != null) {
            trackingService.logClick(user, productId);
        }
        return Map.of("ok", true);
    }


    @PostMapping("/view")
    public void view(@RequestBody EventRequest req) {
        eventService.log(EventType.VIEW, req.getProductId(), req.getScreen(), req.getPosition(), req.getQueryText(), req.getSessionId());
    }
}