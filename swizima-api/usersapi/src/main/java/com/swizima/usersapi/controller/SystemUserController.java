package com.swizima.usersapi.controller;

import com.swizima.usersapi.dto.ActivationStatusRequest;
import com.swizima.usersapi.dto.CreateUserRequest;
import com.swizima.usersapi.dto.PageResponse;
import com.swizima.usersapi.dto.SystemUserResponse;
import com.swizima.usersapi.dto.UpdateUserRequest;
import com.swizima.usersapi.dto.UserCredentialsResponse;
import com.swizima.usersapi.entity.enums.ActivationStatus;
import com.swizima.usersapi.service.SystemUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "System user management")
public class SystemUserController {

    private final SystemUserService userService;

    @PostMapping
    @Operation(summary = "Create a user",
            description = "Username may be an email or phone number. If no password is supplied, a temporary "
                    + "password is generated, returned once, and must be changed on first login.")
    public ResponseEntity<UserCredentialsResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserCredentialsResponse created = userService.createUser(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.user().id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "List users (paginated, filterable)")
    public ResponseEntity<PageResponse<SystemUserResponse>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ActivationStatus activationStatus,
            @RequestParam(required = false) Boolean enabled,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(userService.getUsers(search, activationStatus, enabled, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by ID")
    public ResponseEntity<SystemUserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/by-username")
    @Operation(summary = "Get a user by username (email or phone, any format)")
    public ResponseEntity<SystemUserResponse> getUserByUsername(@RequestParam String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user's username and enabled flag")
    public ResponseEntity<SystemUserResponse> updateUser(@PathVariable UUID id,
                                                         @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PatchMapping("/{id}/activation-status")
    @Operation(summary = "Change a user's activation status")
    public ResponseEntity<SystemUserResponse> updateActivationStatus(@PathVariable UUID id,
                                                                     @Valid @RequestBody ActivationStatusRequest request) {
        return ResponseEntity.ok(userService.updateActivationStatus(id, request.activationStatus()));
    }

    @PatchMapping("/{id}/enable")
    @Operation(summary = "Enable a user")
    public ResponseEntity<SystemUserResponse> enableUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.setEnabled(id, true));
    }

    @PatchMapping("/{id}/disable")
    @Operation(summary = "Disable a user")
    public ResponseEntity<SystemUserResponse> disableUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.setEnabled(id, false));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Reset a user's password",
            description = "Generates a new temporary password (returned once) and forces a change on next login.")
    public ResponseEntity<UserCredentialsResponse> resetPassword(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.resetPassword(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a user")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
