package com.swizima.stafftrackapi.service;

import com.swizima.stafftrackapi.dto.PageResponse;
import com.swizima.stafftrackapi.dto.StaffMemberRequest;
import com.swizima.stafftrackapi.dto.StaffMemberResponse;
import com.swizima.stafftrackapi.entity.enums.StaffStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface StaffMemberService {

    StaffMemberResponse registerStaff(StaffMemberRequest request);

    StaffMemberResponse updateStaff(UUID id, StaffMemberRequest request);

    StaffMemberResponse getStaff(UUID id);

    List<StaffMemberResponse> getAllStaff();

    PageResponse<StaffMemberResponse> searchStaff(String query,
                                                  String department,
                                                  String dutyStation,
                                                  StaffStatus status,
                                                  Pageable pageable);

    void deleteStaff(UUID id);
}

