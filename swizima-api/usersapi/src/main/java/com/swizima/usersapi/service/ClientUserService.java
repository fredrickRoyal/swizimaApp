package com.swizima.usersapi.service;

import com.swizima.usersapi.dto.AssignAccountRequest;
import com.swizima.usersapi.dto.AssignAccountResponse;
import com.swizima.usersapi.dto.ClientUserRequest;
import com.swizima.usersapi.dto.ClientUserResponse;
import com.swizima.usersapi.dto.PageResponse;
import com.swizima.usersapi.entity.enums.ActivationStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ClientUserService {

    ClientUserResponse create(ClientUserRequest request);

    PageResponse<ClientUserResponse> list(UUID clientRef, UUID staffRef, ActivationStatus activationStatus, Boolean defaultClient,
                                          String search, Pageable pageable);

    ClientUserResponse get(UUID id);

    /** Every client a person (phone and/or email) is linked to, default first. */
    List<ClientUserResponse> findByIdentity(String phoneNumber, String email);

    ClientUserResponse update(UUID id, ClientUserRequest request);

    ClientUserResponse updateActivationStatus(UUID id, ActivationStatus status);

    /** Makes this the person's default client and unsets the flag on their other links. */
    ClientUserResponse makeDefault(UUID id);

    void delete(UUID id);

    /**
     * Gives a staff member (or anyone) a sign-in account for a client: creates the system user
     * if the username doesn't exist yet, then links it to the client.
     */
    AssignAccountResponse assignAccount(AssignAccountRequest request);
}
