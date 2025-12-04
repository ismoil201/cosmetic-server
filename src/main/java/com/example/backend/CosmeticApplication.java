package com.example.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.backend")
public class CosmeticApplication {

	public static void main(String[] args) {
		SpringApplication.run(CosmeticApplication.class, args);
	}

}
