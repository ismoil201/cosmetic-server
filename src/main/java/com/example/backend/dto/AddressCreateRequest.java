package com.example.backend.dto;

import lombok.Data;

@Data
public class AddressCreateRequest {
    private String title;
    private String address;
    private Double latitude;
    private Double longitude;
}
