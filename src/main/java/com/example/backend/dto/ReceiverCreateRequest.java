package com.example.backend.dto;

import lombok.Data;

@Data
public class ReceiverCreateRequest {
    private String firstName;
    private String lastName;
    private String phone;
}
