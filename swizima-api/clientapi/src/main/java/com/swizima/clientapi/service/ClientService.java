package com.swizima.clientapi.service;

import com.swizima.clientapi.dto.ClientRequest;
import com.swizima.clientapi.dto.ClientResponse;
import com.swizima.clientapi.dto.PageResponse;
import com.swizima.clientapi.entity.enums.ClientStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ClientService {

    ClientResponse createClient(ClientRequest request);

    ClientResponse updateClient(UUID id, ClientRequest request);

    ClientResponse getClientById(UUID id);

    ClientResponse getClientByCode(String clientCode);

    PageResponse<ClientResponse> getClients(String search, ClientStatus status, Pageable pageable);

    ClientResponse updateClientStatus(UUID id, ClientStatus status);

    void deleteClient(UUID id);
}
