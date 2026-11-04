package com.swizima.clienttransactionsapi.billing;

import com.swizima.clienttransactionsapi.exception.BillingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Calls the Payments API billing endpoints. Every failure is converted into a
 * {@link BillingException} so the caller's transaction is rolled back (blocked).
 */
@Slf4j
@Component
public class BillingClient {

    private final RestClient restClient;

    public BillingClient(@Value("${app.billing.base-url:http://localhost:8083}") String baseUrl,
                         @Value("${app.billing.timeout:5s}") Duration timeout) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(timeout);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl + "/api/v1/payments/billing")
                .requestFactory(factory)
                .build();
    }

    /** Deducts the client's percentage for this transaction. Idempotent on transactionRef. */
    public void charge(UUID clientRef, String transactionRef, BigDecimal transactionAmount, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("clientRef", clientRef);
        body.put("transactionRef", transactionRef);
        body.put("transactionAmount", transactionAmount);
        body.put("description", description);
        try {
            restClient.post().uri("/charges")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException ex) {
            throw translate(ex);
        } catch (ResourceAccessException ex) {
            log.error("Billing service unreachable while charging transaction {}", transactionRef, ex);
            throw unavailable();
        } catch (RestClientException ex) {
            log.error("Billing call failed for transaction {}", transactionRef, ex);
            throw unavailable();
        }
    }

    /** Refunds the charge for a transaction. Best effort: failures are logged, never thrown. */
    public void reverseQuietly(String transactionRef, String reason) {
        try {
            restClient.post().uri("/charges/{ref}/reversal", transactionRef)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("reason", reason))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 404) return; // nothing was charged
            log.error("MANUAL ACTION: could not reverse billing charge for transaction {} ({}): {}",
                    transactionRef, ex.getStatusCode(), ex.getResponseBodyAsString());
        } catch (RuntimeException ex) {
            log.error("MANUAL ACTION: could not reverse billing charge for transaction {}", transactionRef, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private BillingException translate(HttpStatusCodeException ex) {
        Map<String, Object> problem;
        try {
            problem = ex.getResponseBodyAs(Map.class);
        } catch (RuntimeException parse) {
            problem = null;
        }
        if (problem == null) problem = Map.of();
        String detail = problem.get("detail") instanceof String s ? s : null;
        int status = ex.getStatusCode().value();

        Object code = problem.get("code");

        if (BillingException.BILLING_NOT_CONFIGURED.equals(code) || status == 404) {
            return new BillingException(HttpStatus.PAYMENT_REQUIRED, BillingException.BILLING_NOT_CONFIGURED,
                    detail != null ? detail
                            : "Billing is not set up for this client. Please contact support to activate your account.",
                    null);
        }
        if (BillingException.BILLING_UNAVAILABLE.equals(code)) {
            return unavailable();
        }
        if (BillingException.INSUFFICIENT_CREDIT.equals(code) || status == 402) {
            Map<String, Object> details = new LinkedHashMap<>();
            for (String k : new String[]{"requiredAmount", "balance", "currency"}) {
                if (problem.get(k) != null) details.put(k, problem.get(k));
            }
            return new BillingException(HttpStatus.PAYMENT_REQUIRED, BillingException.INSUFFICIENT_CREDIT,
                    detail != null ? detail
                            : "Insufficient billing credit. Please top up your credit to continue.",
                    details);
        }
        if (status == 409 || status == 400) {
            return new BillingException(HttpStatus.CONFLICT, BillingException.BILLING_REJECTED,
                    detail != null ? detail : "Billing rejected this transaction.", null);
        }
        log.error("Billing service error {}: {}", status, ex.getResponseBodyAsString());
        return unavailable();
    }

    private static BillingException unavailable() {
        return new BillingException(HttpStatus.SERVICE_UNAVAILABLE, BillingException.BILLING_UNAVAILABLE,
                "Billing is temporarily unavailable, so the transaction could not be recorded. Please try again.",
                null);
    }
}
