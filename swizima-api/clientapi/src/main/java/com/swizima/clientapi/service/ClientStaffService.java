package com.swizima.clientapi.service;

import com.swizima.clientapi.dto.ClientStaffRequest;
import com.swizima.clientapi.dto.ClientStaffResponse;
import com.swizima.clientapi.dto.PageResponse;
import com.swizima.clientapi.entity.enums.Gender;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ClientStaffService {

    ClientStaffResponse createStaff(UUID clientId, ClientStaffRequest request);

    ClientStaffResponse updateStaff(UUID clientId, UUID staffId, ClientStaffRequest request);

    ClientStaffResponse getStaffById(UUID clientId, UUID staffId);

    PageResponse<ClientStaffResponse> getStaff(UUID clientId, Gender gender, String search, Pageable pageable);

    void deleteStaff(UUID clientId, UUID staffId);
}
