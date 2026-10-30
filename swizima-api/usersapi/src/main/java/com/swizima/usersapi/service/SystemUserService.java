package com.swizima.usersapi.service;

import com.swizima.usersapi.dto.CreateUserRequest;
import com.swizima.usersapi.dto.PageResponse;
import com.swizima.usersapi.dto.SystemUserResponse;
import com.swizima.usersapi.dto.UpdateUserRequest;
import com.swizima.usersapi.dto.UserCredentialsResponse;
import com.swizima.usersapi.entity.enums.ActivationStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SystemUserService {

    UserCredentialsResponse createUser(CreateUserRequest request);

    SystemUserResponse updateUser(UUID id, UpdateUserRequest request);

    SystemUserResponse getUserById(UUID id);

    SystemUserResponse getUserByUsername(String username);

    PageResponse<SystemUserResponse> getUsers(String search, ActivationStatus activationStatus,
                                              Boolean enabled, Pageable pageable);

    SystemUserResponse updateActivationStatus(UUID id, ActivationStatus activationStatus);

    SystemUserResponse setEnabled(UUID id, boolean enabled);

    /** Admin reset: generates a new temporary password and forces a change on next login. */
    UserCredentialsResponse resetPassword(UUID id);

    void deleteUser(UUID id);
}
