package com.swizima.clientapi.service;

import com.swizima.clientapi.entity.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

/**
 * Calls usersapi to automatically create and link a login account (ClientUser + SystemUser)
 * whenever a new client is registered.
 */
@Slf4j
@Component
public class UsersApiClient {

    private final RestClient restClient;

    public UsersApiClient(@Value("${app.users-api.url:${USERS_API_URL:http://localhost:8085}}") String usersApiUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(usersApiUrl)
                .build();
    }

    public void ensureClientAccount(Client client) {
        if (client.getId() == null) {
            return;
        }
        String phone = compactPhone(client.getPhoneNumber());
        String email = client.getEmail() != null ? client.getEmail().trim() : null;
        String username = (phone != null && !phone.isBlank()) ? phone : email;
        if (username == null || username.isBlank()) {
            return;
        }

        try {
            Map<String, Object> payload = Map.of(
                    "clientRef", client.getId(),
                    "username", username,
                    "phoneNumber", phone != null ? phone : "",
                    "email", email != null ? email : "",
                    "preferredOtpChannel", "EMAIL",
                    "accountCategory", "CLIENT_ADMIN",
                    "isDefaultClient", true,
                    "sendCredentials", true,
                    "organisationName", client.getClientName() != null ? client.getClientName() : ""
            );
            restClient.post()
                    .uri("/api/v1/users/client-users/assign-account")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Provisioned client user account for client {} ({})", client.getClientCode(), username);
        } catch (Exception ex) {
            log.warn("Could not auto-provision client user account in usersapi for client {}: {}",
                    client.getClientCode(), ex.getMessage());
        }
    }

    private static String compactPhone(String phone) {
        return phone == null ? null : phone.replaceAll("[\\s()-]", "").trim();
    }
}

