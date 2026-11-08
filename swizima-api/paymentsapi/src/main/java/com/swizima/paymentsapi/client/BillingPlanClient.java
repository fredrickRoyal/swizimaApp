package com.swizima.paymentsapi.client;

import com.swizima.paymentsapi.dto.BillingPlan;
import com.swizima.paymentsapi.exception.BillingNotConfiguredException;
import com.swizima.paymentsapi.exception.DependencyUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;

/**
 * Reads client billing plans from the Configurations API (which caches them,
 * so repeated lookups don't hit its database).
 */
@Slf4j
@Component
public class BillingPlanClient {

    private final RestClient restClient;

    public BillingPlanClient(@Value("${app.configurations.base-url:http://localhost:8082}") String baseUrl,
                             @Value("${app.configurations.timeout:3s}") Duration timeout) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(timeout);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl + "/api/v1/configurations/billing/plans")
                .requestFactory(factory)
                .build();
    }

    public BillingPlan getPlan(UUID clientRef) {
        try {
            BillingPlan plan = restClient.get().uri("/client/{clientRef}", clientRef)
                    .retrieve()
                    .body(BillingPlan.class);
            if (plan == null) throw new IllegalStateException("Empty billing plan response");
            return plan;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new BillingNotConfiguredException(
                    "Billing is not set up for this client. Please contact support to activate your account.");
        } catch (RestClientException | IllegalStateException ex) {
            log.error("Could not load billing plan for client {} from Configurations API", clientRef, ex);
            throw new DependencyUnavailableException(
                    "Billing configuration is temporarily unavailable. Please try again.", ex);
        }
    }
}
