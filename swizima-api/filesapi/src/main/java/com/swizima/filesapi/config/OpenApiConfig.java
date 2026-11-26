package com.swizima.filesapi.config;

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

    @Bean
    public OpenAPI filesApiOpenAPI(@Value("${app.openapi.server-url:/}") String serverUrl) {
        return new OpenAPI()
                .info(new Info()
                        .title("Swizima — Files & Attachments API")
                        .description("General file attachments, organisation logos, uploads, downloads, streaming, thumbnails, and previews.")
                        .version("v1")
                        .contact(new Contact().name("Swizima Platform")))
                .servers(List.of(new Server().url(serverUrl)));
    }
}
