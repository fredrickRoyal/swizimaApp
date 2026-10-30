package com.swizima.clientapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    /**
     * The server URL defaults to the relative path "/". Swagger UI resolves it against the location
     * the spec was loaded from, so "Try it out" calls go through the API gateway when the docs are
     * viewed on the gateway, and directly to this service when viewed on the service itself.
     */
    @Bean
    public OpenAPI clientApiOpenAPI(@Value("${app.openapi.server-url:/}") String serverUrl) {
        return new OpenAPI()
                .info(new Info()
                        .title("Swizima Client API")
                        .description("Manages clients and their service catalogs.")
                        .version("v1")
                        .contact(new Contact().name("Swizima")))
                .servers(List.of(new Server().url(serverUrl)));
    }
}
