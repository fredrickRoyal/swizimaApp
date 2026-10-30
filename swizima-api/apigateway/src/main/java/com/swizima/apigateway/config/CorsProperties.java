package com.swizima.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * CORS settings bound from {@code app.cors.*}.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        @DefaultValue("http://localhost:3000") List<String> allowedOrigins,
        @DefaultValue({"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"}) List<String> allowedMethods,
        @DefaultValue("*") List<String> allowedHeaders,
        @DefaultValue("Location") List<String> exposedHeaders,
        @DefaultValue("true") boolean allowCredentials,
        @DefaultValue("3600") long maxAge
) {
}
