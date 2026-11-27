package com.swizima.apigateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * CORS is handled once, at the gateway, so downstream services don't need their own CORS config.
 * Registered as the first servlet filter so pre-flight (OPTIONS) requests are answered before routing.
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter(CorsProperties props) {
        CorsConfiguration config = new CorsConfiguration();
        Set<String> patterns = new LinkedHashSet<>();
        if (props.allowedOrigins() != null) {
            patterns.addAll(props.allowedOrigins());
        }
        // Always permit standard Swizima / StaffTrack web portal ports across local & deployed hosts
        patterns.add("http://*:4200");
        patterns.add("http://*:4300");
        patterns.add("http://*:5000");
        patterns.add("https://*");
        config.setAllowedOriginPatterns(new ArrayList<>(patterns));
        config.setAllowedMethods(props.allowedMethods());
        config.setAllowedHeaders(props.allowedHeaders());
        config.setExposedHeaders(props.exposedHeaders());
        config.setAllowCredentials(props.allowCredentials());
        config.setMaxAge(props.maxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
