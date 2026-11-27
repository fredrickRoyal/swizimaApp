package com.swizima.stafftrackapi.service.impl;

import com.swizima.stafftrackapi.dto.MasterLookupRequest;
import com.swizima.stafftrackapi.dto.MasterLookupResponse;
import com.swizima.stafftrackapi.dto.PageResponse;
import com.swizima.stafftrackapi.entity.DutyStation;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import com.swizima.stafftrackapi.exception.DuplicateResourceException;
import com.swizima.stafftrackapi.exception.ResourceNotFoundException;
import com.swizima.stafftrackapi.repository.DutyStationRepository;
import com.swizima.stafftrackapi.service.DutyStationService;
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
public class DutyStationServiceImpl implements DutyStationService {

    private final DutyStationRepository dutyStationRepository;

    @Override
    @Transactional
    public MasterLookupResponse createDutyStation(MasterLookupRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        String name = request.name().trim();

        if (dutyStationRepository.existsByCodeIgnoreCaseAndRecordStatusNot(code, RecordStatus.DELETED)) {
            throw new DuplicateResourceException("Duty station with code '" + code + "' already exists");
        }
        if (dutyStationRepository.existsByNameIgnoreCaseAndRecordStatusNot(name, RecordStatus.DELETED)) {
            throw new DuplicateResourceException("Duty station with name '" + name + "' already exists");
        }

        DutyStation station = DutyStation.builder()
                .clientRef(request.clientRef())
                .code(code)
                .name(name)
                .description(request.description())
                .recordStatus(RecordStatus.ACTIVE)
                .build();
        return MasterLookupResponse.fromDutyStation(dutyStationRepository.save(station));
    }

    @Override
    @Transactional
    public MasterLookupResponse updateDutyStation(UUID id, MasterLookupRequest request) {
        DutyStation station = findDutyStation(id);
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        String name = request.name().trim();

        dutyStationRepository.findByCodeIgnoreCaseAndRecordStatusNot(code, RecordStatus.DELETED)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Duty station with code '" + code + "' already exists");
                });
        dutyStationRepository.findByNameIgnoreCaseAndRecordStatusNot(name, RecordStatus.DELETED)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Duty station with name '" + name + "' already exists");
                });

        if (request.clientRef() != null) {
            station.setClientRef(request.clientRef());
        }
        station.setCode(code);
        station.setName(name);
        station.setDescription(request.description());
        return MasterLookupResponse.fromDutyStation(dutyStationRepository.save(station));
    }

    @Override
    public MasterLookupResponse getDutyStation(UUID id) {
        return MasterLookupResponse.fromDutyStation(findDutyStation(id));
    }

    @Override
    public List<MasterLookupResponse> getAllDutyStations() {
        return dutyStationRepository.findAllByRecordStatusNotOrderByNameAsc(RecordStatus.DELETED)
                .stream()
                .map(MasterLookupResponse::fromDutyStation)
                .toList();
    }

    @Override
    public PageResponse<MasterLookupResponse> searchDutyStations(String query, Pageable pageable) {
        return PageResponse.from(
                dutyStationRepository.search(query, RecordStatus.DELETED, pageable)
                        .map(MasterLookupResponse::fromDutyStation)
        );
    }

    @Override
    @Transactional
    public void deleteDutyStation(UUID id) {
        DutyStation station = findDutyStation(id);
        station.setRecordStatus(RecordStatus.DELETED);
        dutyStationRepository.save(station);
    }

    private DutyStation findDutyStation(UUID id) {
        return dutyStationRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Duty station with id '" + id + "' not found"));
    }
}

