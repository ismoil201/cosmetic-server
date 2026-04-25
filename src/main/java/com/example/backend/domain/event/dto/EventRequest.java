package com.example.backend.domain.event.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventRequest {
    private Long productId;     // impression/click/view uchun
    private Integer position;   // home position
    private String screen;      // HOME/SEARCH/DETAIL
    private String queryText;   // search event uchun
    private String sessionId;   // android generatsiya qiladi
}