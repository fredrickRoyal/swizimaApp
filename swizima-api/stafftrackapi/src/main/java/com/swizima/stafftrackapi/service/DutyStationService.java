package com.swizima.stafftrackapi.service;

import com.swizima.stafftrackapi.dto.MasterLookupRequest;
import com.swizima.stafftrackapi.dto.MasterLookupResponse;
import com.swizima.stafftrackapi.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface DutyStationService {

    MasterLookupResponse createDutyStation(MasterLookupRequest request);

    MasterLookupResponse updateDutyStation(UUID id, MasterLookupRequest request);

    MasterLookupResponse getDutyStation(UUID id);

    List<MasterLookupResponse> getAllDutyStations();

    PageResponse<MasterLookupResponse> searchDutyStations(String query, Pageable pageable);

    void deleteDutyStation(UUID id);
}

