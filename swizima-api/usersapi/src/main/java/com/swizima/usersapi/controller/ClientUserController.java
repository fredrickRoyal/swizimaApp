package com.swizima.usersapi.controller;

import com.swizima.usersapi.dto.ActivationStatusRequest;
import com.swizima.usersapi.dto.AssignAccountRequest;
import com.swizima.usersapi.dto.AssignAccountResponse;
import com.swizima.usersapi.dto.ClientUserRequest;
import com.swizima.usersapi.dto.ClientUserResponse;
import com.swizima.usersapi.dto.PageResponse;
import com.swizima.usersapi.entity.enums.ActivationStatus;
import com.swizima.usersapi.service.ClientUserService;
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

import java.util.List;
import java.util.UUID;

/**
 * Under /api/v1/users so the existing gateway route covers it
 * (the literal "client-users" segment takes precedence over /users/{id}).
 */
@RestController
@RequestMapping("/api/v1/users/client-users")
@RequiredArgsConstructor
@Tag(name = "Client Users", description = "People who can access a client, by phone and/or email")
public class ClientUserController {

    private final ClientUserService service;

    @PostMapping
    @Operation(summary = "Link a user to a client", description = "At least one of userPhoneNumber or userEmail is required.")
    public ResponseEntity<ClientUserResponse> create(@Valid @RequestBody ClientUserRequest request) {
        ClientUserResponse created = service.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PostMapping("/assign-account")
    @Operation(summary = "Create (or reuse) a system user and link it to a client",
            description = "If the username is new, a system user is created with a temporary password "
                    + "(returned once). The account is then linked to the client as a client user.")
    public ResponseEntity<AssignAccountResponse> assignAccount(@Valid @RequestBody AssignAccountRequest request) {
        AssignAccountResponse result = service.assignAccount(request);
        return ResponseEntity.status(result.userCreated() ? 201 : 200).body(result);
    }

    @GetMapping
    @Operation(summary = "List client users (paginated, filterable)")
    public ResponseEntity<PageResponse<ClientUserResponse>> list(
            @RequestParam(required = false) UUID clientRef,
            @RequestParam(required = false) UUID staffRef,
            @RequestParam(required = false) ActivationStatus activationStatus,
            @RequestParam(required = false) Boolean isDefaultClient,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.list(clientRef, staffRef, activationStatus, isDefaultClient, search, pageable));
    }

    @GetMapping("/by-identity")
    @Operation(summary = "All clients a person is linked to (by phone and/or email), default first")
    public ResponseEntity<List<ClientUserResponse>> byIdentity(@RequestParam(required = false) String phoneNumber,
                                                               @RequestParam(required = false) String email) {
        return ResponseEntity.ok(service.findByIdentity(phoneNumber, email));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a client user by ID")
    public ResponseEntity<ClientUserResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a client user")
    public ResponseEntity<ClientUserResponse> update(@PathVariable UUID id, @Valid @RequestBody ClientUserRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/activation-status")
    @Operation(summary = "Change a client user's activation status")
    public ResponseEntity<ClientUserResponse> updateActivationStatus(@PathVariable UUID id,
                                                                     @Valid @RequestBody ActivationStatusRequest request) {
        return ResponseEntity.ok(service.updateActivationStatus(id, request.activationStatus()));
    }

    @PatchMapping("/{id}/default")
    @Operation(summary = "Make this the user's default client")
    public ResponseEntity<ClientUserResponse> makeDefault(@PathVariable UUID id) {
        return ResponseEntity.ok(service.makeDefault(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a user from a client (soft delete)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
