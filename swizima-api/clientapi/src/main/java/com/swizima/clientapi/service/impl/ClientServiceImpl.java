package com.swizima.clientapi.service.impl;

import com.swizima.clientapi.dto.ClientRequest;
import com.swizima.clientapi.dto.ClientResponse;
import com.swizima.clientapi.dto.PageResponse;
import com.swizima.clientapi.entity.Client;
import com.swizima.clientapi.entity.enums.ClientStatus;
import com.swizima.clientapi.entity.enums.RecordStatus;
import com.swizima.clientapi.exception.DuplicateResourceException;
import com.swizima.clientapi.exception.ResourceNotFoundException;
import com.swizima.clientapi.repository.ClientRepository;
import com.swizima.clientapi.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;

    @Override
    @Transactional
    public ClientResponse createClient(ClientRequest request) {
        String code = request.clientCode().trim();
        if (clientRepository.existsByClientCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Client with code '" + code + "' already exists");
        }

        Client client = new Client();
        applyRequest(client, request);
        client.setClientCode(code);
        if (request.status() == null) {
            client.setStatus(ClientStatus.PENDING);
        }
        client.setRecordStatus(RecordStatus.ACTIVE);

        return ClientResponse.from(clientRepository.save(client));
    }

    @Override
    @Transactional
    public ClientResponse updateClient(UUID id, ClientRequest request) {
        Client client = findActiveClient(id);

        String code = request.clientCode().trim();
        if (clientRepository.existsByClientCodeIgnoreCaseAndIdNot(code, id)) {
            throw new DuplicateResourceException("Client with code '" + code + "' already exists");
        }

        applyRequest(client, request);
        client.setClientCode(code);

        return ClientResponse.from(clientRepository.save(client));
    }

    @Override
    public ClientResponse getClientById(UUID id) {
        return ClientResponse.from(findActiveClient(id));
    }

    @Override
    public ClientResponse getClientByCode(String clientCode) {
        return clientRepository
                .findByClientCodeIgnoreCaseAndRecordStatusNot(clientCode.trim(), RecordStatus.DELETED)
                .map(ClientResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Client with code '" + clientCode + "' not found"));
    }

    @Override
    public PageResponse<ClientResponse> getClients(String search, ClientStatus status, Pageable pageable) {
        Page<Client> page;
        if (search != null && !search.isBlank()) {
            page = clientRepository.search(search.trim(), RecordStatus.DELETED, pageable);
        } else if (status != null) {
            page = clientRepository.findAllByStatusAndRecordStatusNot(status, RecordStatus.DELETED, pageable);
        } else {
            page = clientRepository.findAllByRecordStatusNot(RecordStatus.DELETED, pageable);
        }
        return PageResponse.from(page.map(ClientResponse::from));
    }

    @Override
    @Transactional
    public ClientResponse updateClientStatus(UUID id, ClientStatus status) {
        Client client = findActiveClient(id);
        client.setStatus(status);
        return ClientResponse.from(clientRepository.save(client));
    }

    @Override
    @Transactional
    public void deleteClient(UUID id) {
        Client client = findActiveClient(id);
        // Soft delete: keep the row for audit purposes
        client.setRecordStatus(RecordStatus.DELETED);
        clientRepository.save(client);
    }

    private Client findActiveClient(UUID id) {
        return clientRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Client with id '" + id + "' not found"));
    }

    private void applyRequest(Client client, ClientRequest r) {
        client.setClientName(r.clientName());
        client.setTradingName(r.tradingName());
        client.setClientType(r.clientType());
        client.setIndustrySector(r.industrySector());
        client.setDescription(r.description());
        client.setEmail(r.email());
        client.setPhoneNumber(r.phoneNumber());
        client.setAlternativePhone(r.alternativePhone());
        client.setAddress(r.address());
        client.setCountry(r.country());
        client.setDistrict(r.district());
        client.setCity(r.city());
        client.setContactPerson(r.contactPerson());
        client.setContactPersonTitle(r.contactPersonTitle());
        client.setContactPersonEmail(r.contactPersonEmail());
        client.setContactPersonPhone(r.contactPersonPhone());
        if (r.status() != null) {
            client.setStatus(r.status());
        }
    }
}
