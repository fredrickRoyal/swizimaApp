package com.swizima.usersapi.service.impl;

import com.swizima.usersapi.dto.AssignAccountRequest;
import com.swizima.usersapi.dto.AssignAccountResponse;
import com.swizima.usersapi.dto.ClientUserRequest;
import com.swizima.usersapi.dto.CreateUserRequest;
import com.swizima.usersapi.dto.SystemUserResponse;
import com.swizima.usersapi.dto.UserCredentialsResponse;
import com.swizima.usersapi.entity.SystemUser;
import com.swizima.usersapi.entity.enums.OtpChannel;
import com.swizima.usersapi.entity.enums.UsernameType;
import com.swizima.usersapi.repository.SystemUserRepository;
import com.swizima.usersapi.service.SystemUserService;
import com.swizima.usersapi.service.notify.AccountCredentialsNotifier;
import com.swizima.usersapi.util.UsernameNormalizer.NormalizedUsername;
import java.util.Optional;
import com.swizima.usersapi.dto.ClientUserResponse;
import com.swizima.usersapi.dto.PageResponse;
import com.swizima.usersapi.entity.ClientUser;
import com.swizima.usersapi.entity.enums.ActivationStatus;
import com.swizima.usersapi.entity.enums.RecordStatus;
import com.swizima.usersapi.exception.BadRequestException;
import com.swizima.usersapi.exception.DuplicateResourceException;
import com.swizima.usersapi.exception.ResourceNotFoundException;
import com.swizima.usersapi.repository.ClientUserRepository;
import com.swizima.usersapi.service.ClientUserService;
import com.swizima.usersapi.util.UsernameNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientUserServiceImpl implements ClientUserService {

    private final ClientUserRepository repository;
    private final UsernameNormalizer normalizer;
    private final SystemUserRepository systemUserRepository;
    private final SystemUserService systemUserService;
    private final AccountCredentialsNotifier credentialsNotifier;

    @Override
    public ClientUserResponse create(ClientUserRequest request) {
        Contact c = contact(request.userPhoneNumber(), request.userEmail());
        ensureNotLinked(request.clientRef(), c, null);

        ClientUser u = new ClientUser();
        u.setClientRef(request.clientRef());
        u.setStaffRef(request.staffRef());
        u.setUserPhoneNumber(c.phone());
        u.setUserEmail(c.email());
        u.setActivationStatus(request.activationStatus() != null
                ? request.activationStatus() : ActivationStatus.PENDING_ACTIVATION);
        // A person's first client becomes their default automatically.
        boolean first = repository.findByIdentity(c.phone(), c.email(), RecordStatus.DELETED).isEmpty();
        u.setDefaultClient(Boolean.TRUE.equals(request.isDefaultClient()) || first);

        ClientUser saved = repository.saveAndFlush(u);
        if (saved.isDefaultClient()) {
            repository.clearOtherDefaults(saved.getId(), c.phone(), c.email());
        }
        return ClientUserResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClientUserResponse> list(UUID clientRef, UUID staffRef, ActivationStatus activationStatus, Boolean defaultClient,
                                                 String search, Pageable pageable) {
        String term = (search == null || search.isBlank()) ? null : search.trim();
        return PageResponse.from(repository
                .search(clientRef, staffRef, activationStatus, defaultClient, term, RecordStatus.DELETED, pageable)
                .map(ClientUserResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientUserResponse get(UUID id) {
        return ClientUserResponse.from(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientUserResponse> findByIdentity(String phoneNumber, String email) {
        Contact c = contact(phoneNumber, email);
        return repository.findByIdentity(c.phone(), c.email(), RecordStatus.DELETED)
                .stream().map(ClientUserResponse::from).toList();
    }

    @Override
    public ClientUserResponse update(UUID id, ClientUserRequest request) {
        ClientUser u = find(id);
        Contact c = contact(request.userPhoneNumber(), request.userEmail());
        ensureNotLinked(request.clientRef(), c, id);

        u.setClientRef(request.clientRef());
        if (request.staffRef() != null) u.setStaffRef(request.staffRef());
        u.setUserPhoneNumber(c.phone());
        u.setUserEmail(c.email());
        if (request.activationStatus() != null) {
            u.setActivationStatus(request.activationStatus());
        }
        if (request.isDefaultClient() != null) {
            u.setDefaultClient(request.isDefaultClient());
        }
        ClientUser saved = repository.saveAndFlush(u);
        if (saved.isDefaultClient()) {
            repository.clearOtherDefaults(saved.getId(), c.phone(), c.email());
        }
        return ClientUserResponse.from(saved);
    }

    @Override
    public ClientUserResponse updateActivationStatus(UUID id, ActivationStatus status) {
        ClientUser u = find(id);
        u.setActivationStatus(status);
        if (status == ActivationStatus.ACTIVE && u.getSystemUserId() != null) {
            systemUserRepository.findByIdAndRecordStatusNot(u.getSystemUserId(), RecordStatus.DELETED)
                    .filter(su -> su.getActivationStatus() != ActivationStatus.ACTIVE)
                    .ifPresent(su -> {
                        su.setActivationStatus(ActivationStatus.ACTIVE);
                        su.setEnabled(true);
                        systemUserRepository.save(su);
                    });
        }
        return ClientUserResponse.from(repository.saveAndFlush(u));
    }

    @Override
    public ClientUserResponse makeDefault(UUID id) {
        ClientUser u = find(id);
        u.setDefaultClient(true);
        ClientUser saved = repository.saveAndFlush(u);
        repository.clearOtherDefaults(saved.getId(), saved.getUserPhoneNumber(), saved.getUserEmail());
        return ClientUserResponse.from(saved);
    }

    /** Soft delete. */
    @Override
    public void delete(UUID id) {
        ClientUser u = find(id);
        u.setRecordStatus(RecordStatus.DELETED);
        u.setDefaultClient(false);
    }

    @Override
    public AssignAccountResponse assignAccount(AssignAccountRequest r) {
        NormalizedUsername username = normalizer.normalize(r.username());
        boolean isPhone = username.type() == UsernameType.PHONE;
        String phone = isPhone ? username.value() : normalizer.normalizePhoneNumber(r.phoneNumber());
        String email = isPhone ? normalizer.normalizeEmail(r.email()) : username.value();

        // 1. System user: reuse when the username already exists, otherwise create one.
        Optional<SystemUser> existing = systemUserRepository.findByUsernameAndRecordStatusNot(username.value(), RecordStatus.DELETED);
        SystemUserResponse user;
        String temporaryPassword = null;
        boolean created = existing.isEmpty();
        if (created) {
            OtpChannel channel = r.preferredOtpChannel() != null ? r.preferredOtpChannel()
                    : (email != null ? OtpChannel.EMAIL : OtpChannel.SMS);
            UserCredentialsResponse credentials = systemUserService.createUser(
                    new CreateUserRequest(username.value(), null, true, email, phone, channel));
            user = credentials.user();
            temporaryPassword = credentials.temporaryPassword();
        } else {
            UUID existingId = existing.get().getId();
            if (email != null && systemUserRepository.contactInUse(email, existingId, RecordStatus.DELETED)) {
                throw new DuplicateResourceException("Email '" + email + "' is already used by another user");
            }
            if (phone != null && systemUserRepository.contactInUse(phone, existingId, RecordStatus.DELETED)) {
                throw new DuplicateResourceException("Phone number '" + phone + "' is already used by another user");
            }
            user = SystemUserResponse.from(existing.get());
        }

        // 2. Client user link: update an existing link for this person, or create one.
        List<ClientUser> links = repository.findLinks(r.clientRef(), phone, email, RecordStatus.DELETED);
        if (links.size() > 1) {
            throw new DuplicateResourceException("This phone number and email are linked to different users of the client");
        }
        ClientUser link = links.isEmpty() ? new ClientUser() : links.get(0);
        if (!links.isEmpty() && link.getSystemUserId() != null && !link.getSystemUserId().equals(user.id())) {
            throw new DuplicateResourceException("This contact is already linked to another account for the client");
        }
        boolean first = repository.findByIdentity(phone, email, RecordStatus.DELETED).isEmpty();
        link.setClientRef(r.clientRef());
        if (r.staffRef() != null) link.setStaffRef(r.staffRef());
        link.setSystemUserId(user.id());
        if (phone != null) link.setUserPhoneNumber(phone);
        if (email != null) link.setUserEmail(email);
        link.setActivationStatus(user.activationStatus());
        if (Boolean.TRUE.equals(r.isDefaultClient()) || first) link.setDefaultClient(true);

        ClientUser saved = repository.saveAndFlush(link);
        if (saved.isDefaultClient()) {
            repository.clearOtherDefaults(saved.getId(), saved.getUserPhoneNumber(), saved.getUserEmail());
        }
        String sentTo = null;
        String note = null;
        if (temporaryPassword != null && !Boolean.FALSE.equals(r.sendCredentials())) {
            AccountCredentialsNotifier.Delivery d = credentialsNotifier.send(
                    user.email() != null ? user.email() : email, user.username(), temporaryPassword, r.organisationName());
            sentTo = d.sentTo();
            note = d.note();
        }
        return new AssignAccountResponse(ClientUserResponse.from(saved), user, created, temporaryPassword, sentTo, note);
    }

    // ------------------------------------------------------------------ helpers

    private record Contact(String phone, String email) {
    }

    /** Normalises phone (E.164) and email (lower case); at least one must be present. */
    private Contact contact(String rawPhone, String rawEmail) {
        String phone = normalizer.normalizePhoneNumber(rawPhone);
        String email = normalizer.normalizeEmail(rawEmail);
        if (phone == null && email == null) {
            throw new BadRequestException("Provide a phone number or an email address");
        }
        return new Contact(phone, email);
    }

    private void ensureNotLinked(UUID clientRef, Contact c, UUID selfId) {
        if (repository.existsLink(clientRef, c.phone(), c.email(), selfId, RecordStatus.DELETED)) {
            throw new DuplicateResourceException("A user with this phone number or email is already linked to the client");
        }
    }

    private ClientUser find(UUID id) {
        return repository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Client user not found: " + id));
    }
}
