package com.swizima.clientapi.service.impl;

import com.swizima.clientapi.dto.ClientStaffRequest;
import com.swizima.clientapi.dto.ClientStaffResponse;
import com.swizima.clientapi.dto.PageResponse;
import com.swizima.clientapi.entity.Client;
import com.swizima.clientapi.entity.ClientStaff;
import com.swizima.clientapi.entity.enums.Gender;
import com.swizima.clientapi.entity.enums.RecordStatus;
import com.swizima.clientapi.exception.DuplicateResourceException;
import com.swizima.clientapi.exception.ResourceNotFoundException;
import com.swizima.clientapi.repository.ClientRepository;
import com.swizima.clientapi.repository.ClientStaffRepository;
import com.swizima.clientapi.service.ClientStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientStaffServiceImpl implements ClientStaffService {

    private final ClientStaffRepository staffRepository;
    private final ClientRepository clientRepository;

    @Override
    @Transactional
    public ClientStaffResponse createStaff(UUID clientId, ClientStaffRequest request) {
        Client client = findActiveClient(clientId);
        String nin = normalizeNin(request.nin());
        if (nin != null && staffRepository.existsByClientIdAndNinIgnoreCaseAndRecordStatusNot(
                client.getId(), nin, RecordStatus.DELETED)) {
            throw duplicateNin(nin, client);
        }

        ClientStaff staff = new ClientStaff();
        staff.setClient(client);
        applyRequest(staff, request, nin);
        staff.setRecordStatus(RecordStatus.ACTIVE);

        return ClientStaffResponse.from(staffRepository.save(staff));
    }

    @Override
    @Transactional
    public ClientStaffResponse updateStaff(UUID clientId, UUID staffId, ClientStaffRequest request) {
        ClientStaff staff = findActiveStaff(clientId, staffId);

        String nin = normalizeNin(request.nin());
        if (nin != null && staffRepository.existsByClientIdAndNinIgnoreCaseAndRecordStatusNotAndIdNot(
                clientId, nin, RecordStatus.DELETED, staffId)) {
            throw duplicateNin(nin, staff.getClient());
        }

        applyRequest(staff, request, nin);
        return ClientStaffResponse.from(staffRepository.save(staff));
    }

    @Override
    public ClientStaffResponse getStaffById(UUID clientId, UUID staffId) {
        return ClientStaffResponse.from(findActiveStaff(clientId, staffId));
    }

    @Override
    public PageResponse<ClientStaffResponse> getStaff(UUID clientId, Gender gender, String search, Pageable pageable) {
        findActiveClient(clientId);
        String term = (search == null || search.isBlank()) ? null : search.trim();
        return PageResponse.from(
                staffRepository.search(clientId, gender, term, RecordStatus.DELETED, pageable)
                        .map(ClientStaffResponse::from));
    }

    @Override
    @Transactional
    public void deleteStaff(UUID clientId, UUID staffId) {
        ClientStaff staff = findActiveStaff(clientId, staffId);
        // Soft delete: keep the row for audit purposes
        staff.setRecordStatus(RecordStatus.DELETED);
        staffRepository.save(staff);
    }

    private ClientStaff findActiveStaff(UUID clientId, UUID staffId) {
        return staffRepository.findByIdAndRecordStatusNot(staffId, RecordStatus.DELETED)
                .filter(s -> s.getClient().getId().equals(clientId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Staff member with id '" + staffId + "' not found for client '" + clientId + "'"));
    }

    private Client findActiveClient(UUID clientId) {
        return clientRepository.findByIdAndRecordStatusNot(clientId, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Client with id '" + clientId + "' not found"));
    }

    private static DuplicateResourceException duplicateNin(String nin, Client client) {
        return new DuplicateResourceException(
                "A staff member with NIN '" + nin + "' already exists for client '" + client.getClientCode() + "'");
    }

    private static String normalizeNin(String nin) {
        return (nin == null || nin.isBlank()) ? null : nin.trim().toUpperCase(Locale.ROOT);
    }

    private static final java.util.regex.Pattern NAME_PART_START =
            java.util.regex.Pattern.compile("(^|[\\s'-])(\\p{L})");

    /** "mary-jane o'neil" -> "Mary-Jane O'Neil" (rest of each word left as typed). */
    static String capitalizeName(String value) {
        return NAME_PART_START.matcher(value)
                .replaceAll(m -> java.util.regex.Matcher.quoteReplacement(m.group(1) + m.group(2).toUpperCase(Locale.ROOT)));
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private static void applyRequest(ClientStaff staff, ClientStaffRequest r, String nin) {
        staff.setSurname(capitalizeName(r.surname().trim()));
        staff.setGivenName(capitalizeName(r.givenName().trim()));
        staff.setGender(r.gender());
        staff.setPhoneNumber(blankToNull(r.phoneNumber()));
        staff.setAlternativePhone(blankToNull(r.alternativePhone()));
        staff.setEmail(r.email() == null || r.email().isBlank() ? null : r.email().trim().toLowerCase(Locale.ROOT));
        staff.setPosition(blankToNull(r.position()));
        staff.setNin(nin);
        staff.setSummary(blankToNull(r.summary()));
    }
}
