package com.swizima.stafftrackapi.service;

import com.swizima.stafftrackapi.dto.MasterLookupRequest;
import com.swizima.stafftrackapi.dto.MasterLookupResponse;
import com.swizima.stafftrackapi.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface DepartmentService {

    MasterLookupResponse createDepartment(MasterLookupRequest request);

    MasterLookupResponse updateDepartment(UUID id, MasterLookupRequest request);

    MasterLookupResponse getDepartment(UUID id);

    List<MasterLookupResponse> getAllDepartments();

    PageResponse<MasterLookupResponse> searchDepartments(String query, Pageable pageable);

    void deleteDepartment(UUID id);
}

