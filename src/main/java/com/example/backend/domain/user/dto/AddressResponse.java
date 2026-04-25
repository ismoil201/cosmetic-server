package com.example.backend.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddressResponse {
    private Long id;
    private String title;
    private String address;
    private Double latitude;
    private Double longitude;
}
