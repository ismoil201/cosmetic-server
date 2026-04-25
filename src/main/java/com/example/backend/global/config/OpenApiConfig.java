package com.example.backend.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()

                // ✅ SERVER URL NI HTTPS QILDIK
                .servers(List.of(
                        new Server()
                                .url("https://cosmetic-server-production.up.railway.app")
                                .description("Production Server")
                ))

                // 🔐 Global security
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(
                        SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ))
                .info(new Info()
                        .title("Zaven API")
                        .description("Zaven Marketplace Backend API (Spring Boot + JWT + OpenAPI)")
                        .version("v1")
                        .contact(new Contact().name("Zaven Team").email("ismoiljoraxonov1@gmail.com"))
                        .license(new License().name("Proprietary")));
    }
}