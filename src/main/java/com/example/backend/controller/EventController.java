package com.example.backend.controller;

import com.example.backend.dto.EventRequest;
import com.example.backend.entity.EventType;
import com.example.backend.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    @PostMapping("/impression")
    public void impression(@RequestBody EventRequest req) {
        eventService.log(EventType.IMPRESSION, req.getProductId(), req.getScreen(), req.getPosition(), req.getQueryText(), req.getSessionId());
    }

    @PostMapping("/click")
    public void click(@RequestBody EventRequest req) {
        eventService.log(EventType.CLICK, req.getProductId(), req.getScreen(), req.getPosition(), req.getQueryText(), req.getSessionId());
    }

    @PostMapping("/view")
    public void view(@RequestBody EventRequest req) {
        eventService.log(EventType.VIEW, req.getProductId(), req.getScreen(), req.getPosition(), req.getQueryText(), req.getSessionId());
    }
}