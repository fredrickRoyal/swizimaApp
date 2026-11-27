package com.swizima.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Configures Spring Cloud Gateway WebMVC's outbound HTTP client to use JDK {@link HttpClient}
 * pinned to {@link HttpClient.Version#HTTP_1_1}. This supports {@code PATCH} (unlike
 * {@code SimpleClientHttpRequestFactory}/{@code HttpURLConnection}) while avoiding cleartext
 * HTTP/2 (h2c) upgrade rejections against downstream Tomcat services.
 */
@Configuration
public class GatewayHttpClientConfig {

    @Bean
    @Primary
    public ClientHttpRequestFactory gatewayClientHttpRequestFactory(
            @Value("${spring.http.clients.connect-timeout:5s}") Duration connectTimeout,
            @Value("${spring.http.clients.read-timeout:30s}") Duration readTimeout) {
        HttpClient jdkClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdkClient);
        factory.setReadTimeout(readTimeout);
        return factory;
    }

    @Bean
    @Scope("prototype")
    public RestClient.Builder gatewayRestClientBuilder(ClientHttpRequestFactory factory) {
        return RestClient.builder().requestFactory(factory);
    }
}
