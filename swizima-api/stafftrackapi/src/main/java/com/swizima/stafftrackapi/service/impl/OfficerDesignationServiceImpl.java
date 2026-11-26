package com.swizima.stafftrackapi.service.impl;

import com.swizima.stafftrackapi.dto.MasterLookupRequest;
import com.swizima.stafftrackapi.dto.MasterLookupResponse;
import com.swizima.stafftrackapi.dto.PageResponse;
import com.swizima.stafftrackapi.entity.OfficerDesignation;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import com.swizima.stafftrackapi.exception.DuplicateResourceException;
import com.swizima.stafftrackapi.exception.ResourceNotFoundException;
import com.swizima.stafftrackapi.repository.OfficerDesignationRepository;
import com.swizima.stafftrackapi.service.OfficerDesignationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfficerDesignationServiceImpl implements OfficerDesignationService {

    private final OfficerDesignationRepository designationRepository;

    @Override
    @Transactional
    public MasterLookupResponse createDesignation(MasterLookupRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        String name = request.name().trim();

        if (designationRepository.existsByCodeIgnoreCaseAndRecordStatusNot(code, RecordStatus.DELETED)) {
            throw new DuplicateResourceException("Officer designation with code '" + code + "' already exists");
        }
        if (designationRepository.existsByNameIgnoreCaseAndRecordStatusNot(name, RecordStatus.DELETED)) {
            throw new DuplicateResourceException("Officer designation with name '" + name + "' already exists");
        }

        OfficerDesignation designation = OfficerDesignation.builder()
                .clientRef(request.clientRef())
                .code(code)
                .name(name)
                .description(request.description())
                .recordStatus(RecordStatus.ACTIVE)
                .build();
        return MasterLookupResponse.fromDesignation(designationRepository.save(designation));
    }

    @Override
    @Transactional
    public MasterLookupResponse updateDesignation(UUID id, MasterLookupRequest request) {
        OfficerDesignation designation = findDesignation(id);
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        String name = request.name().trim();

        designationRepository.findByCodeIgnoreCaseAndRecordStatusNot(code, RecordStatus.DELETED)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Officer designation with code '" + code + "' already exists");
                });
        designationRepository.findByNameIgnoreCaseAndRecordStatusNot(name, RecordStatus.DELETED)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Officer designation with name '" + name + "' already exists");
                });

        if (request.clientRef() != null) {
            designation.setClientRef(request.clientRef());
        }
        designation.setCode(code);
        designation.setName(name);
        designation.setDescription(request.description());
        return MasterLookupResponse.fromDesignation(designationRepository.save(designation));
    }

    @Override
    public MasterLookupResponse getDesignation(UUID id) {
        return MasterLookupResponse.fromDesignation(findDesignation(id));
    }

    @Override
    public List<MasterLookupResponse> getAllDesignations() {
        return designationRepository.findAllByRecordStatusNotOrderByNameAsc(RecordStatus.DELETED)
                .stream()
                .map(MasterLookupResponse::fromDesignation)
                .toList();
    }

    @Override
    public PageResponse<MasterLookupResponse> searchDesignations(String query, Pageable pageable) {
        return PageResponse.from(
                designationRepository.search(query, RecordStatus.DELETED, pageable)
                        .map(MasterLookupResponse::fromDesignation)
        );
    }

    @Override
    @Transactional
    public void deleteDesignation(UUID id) {
        OfficerDesignation designation = findDesignation(id);
        designation.setRecordStatus(RecordStatus.DELETED);
        designationRepository.save(designation);
    }

    private OfficerDesignation findDesignation(UUID id) {
        return designationRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Officer designation with id '" + id + "' not found"));
    }
}

