package com.swizima.stafftrackapi.service;

import com.swizima.stafftrackapi.dto.MasterLookupRequest;
import com.swizima.stafftrackapi.dto.MasterLookupResponse;
import com.swizima.stafftrackapi.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface OfficerDesignationService {

    MasterLookupResponse createDesignation(MasterLookupRequest request);

    MasterLookupResponse updateDesignation(UUID id, MasterLookupRequest request);

    MasterLookupResponse getDesignation(UUID id);

    List<MasterLookupResponse> getAllDesignations();

    PageResponse<MasterLookupResponse> searchDesignations(String query, Pageable pageable);

    void deleteDesignation(UUID id);
}

