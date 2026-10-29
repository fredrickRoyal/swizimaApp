package com.swizima.clientapi.service.impl;

import com.swizima.clientapi.dto.PageResponse;
import com.swizima.clientapi.dto.ServiceCatalogRequest;
import com.swizima.clientapi.dto.ServiceCatalogResponse;
import com.swizima.clientapi.entity.Client;
import com.swizima.clientapi.entity.ServiceCatalog;
import com.swizima.clientapi.entity.enums.RecordStatus;
import com.swizima.clientapi.entity.enums.ServiceCatalogStatus;
import com.swizima.clientapi.entity.enums.ServiceCategory;
import com.swizima.clientapi.exception.DuplicateResourceException;
import com.swizima.clientapi.exception.ResourceNotFoundException;
import com.swizima.clientapi.repository.ClientRepository;
import com.swizima.clientapi.repository.ServiceCatalogRepository;
import com.swizima.clientapi.service.ServiceCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceCatalogServiceImpl implements ServiceCatalogService {

    private static final String DEFAULT_CURRENCY = "UGX";

    private final ServiceCatalogRepository serviceCatalogRepository;
    private final ClientRepository clientRepository;

    @Override
    @Transactional
    public ServiceCatalogResponse createService(ServiceCatalogRequest request) {
        Client client = findActiveClient(request.clientId());
        String code = request.serviceCode().trim();

        if (serviceCatalogRepository.existsByClientIdAndServiceCodeIgnoreCase(client.getId(), code)) {
            throw new DuplicateResourceException(
                    "Service with code '" + code + "' already exists for client '" + client.getClientCode() + "'");
        }

        ServiceCatalog service = new ServiceCatalog();
        service.setClient(client);
        service.setServiceCode(code);
        applyRequest(service, request);
        if (request.status() == null) {
            service.setStatus(ServiceCatalogStatus.ACTIVE);
        }
        service.setRecordStatus(RecordStatus.ACTIVE);

        return ServiceCatalogResponse.from(serviceCatalogRepository.save(service));
    }

    @Override
    @Transactional
    public ServiceCatalogResponse updateService(UUID id, ServiceCatalogRequest request) {
        ServiceCatalog service = findActiveService(id);

        Client client = service.getClient().getId().equals(request.clientId())
                ? service.getClient()
                : findActiveClient(request.clientId());

        String code = request.serviceCode().trim();
        if (serviceCatalogRepository.existsByClientIdAndServiceCodeIgnoreCaseAndIdNot(client.getId(), code, id)) {
            throw new DuplicateResourceException(
                    "Service with code '" + code + "' already exists for client '" + client.getClientCode() + "'");
        }

        service.setClient(client);
        service.setServiceCode(code);
        applyRequest(service, request);

        return ServiceCatalogResponse.from(serviceCatalogRepository.save(service));
    }

    @Override
    public ServiceCatalogResponse getServiceById(UUID id) {
        return ServiceCatalogResponse.from(findActiveService(id));
    }

    @Override
    public PageResponse<ServiceCatalogResponse> getServices(UUID clientId,
                                                            ServiceCategory category,
                                                            ServiceCatalogStatus status,
                                                            String search,
                                                            Pageable pageable) {
        String term = (search == null || search.isBlank()) ? null : search.trim();
        return PageResponse.from(
                serviceCatalogRepository
                        .search(clientId, category, status, term, RecordStatus.DELETED, pageable)
                        .map(ServiceCatalogResponse::from));
    }

    @Override
    @Transactional
    public ServiceCatalogResponse updateServiceStatus(UUID id, ServiceCatalogStatus status) {
        ServiceCatalog service = findActiveService(id);
        service.setStatus(status);
        return ServiceCatalogResponse.from(serviceCatalogRepository.save(service));
    }

    @Override
    @Transactional
    public void deleteService(UUID id) {
        ServiceCatalog service = findActiveService(id);
        // Soft delete: keep the row for audit purposes
        service.setRecordStatus(RecordStatus.DELETED);
        serviceCatalogRepository.save(service);
    }

    private ServiceCatalog findActiveService(UUID id) {
        return serviceCatalogRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Service catalog item with id '" + id + "' not found"));
    }

    private Client findActiveClient(UUID clientId) {
        return clientRepository.findByIdAndRecordStatusNot(clientId, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Client with id '" + clientId + "' not found"));
    }

    private void applyRequest(ServiceCatalog service, ServiceCatalogRequest r) {
        service.setServiceName(r.serviceName());
        service.setCategory(r.category());
        service.setDescription(r.description());
        service.setPrice(r.price());
        service.setCurrency(r.currency() == null || r.currency().isBlank()
                ? DEFAULT_CURRENCY
                : r.currency().trim().toUpperCase(Locale.ROOT));
        service.setDurationMinutes(r.durationMinutes());
        if (r.status() != null) {
            service.setStatus(r.status());
        }
    }
}
