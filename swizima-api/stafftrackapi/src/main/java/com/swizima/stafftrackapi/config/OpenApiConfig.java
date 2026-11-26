package com.swizima.stafftrackapi.config;

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
    public OpenAPI staffTrackApiOpenAPI(@Value("${app.openapi.server-url:/}") String serverUrl) {
        return new OpenAPI()
                .info(new Info()
                        .title("SOOTR — Staff Out-of-Office Tracking & Reporting API")
                        .description("Staff registration, departments, officer designations, duty stations, out-of-office notifications, and report-back management actions.")
                        .version("v1")
                        .contact(new Contact().name("Office of the Prime Minister (OPM) · Department of Refugees")))
                .servers(List.of(new Server().url(serverUrl)));
    }
}

