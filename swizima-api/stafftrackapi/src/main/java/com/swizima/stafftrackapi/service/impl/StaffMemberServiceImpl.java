package com.swizima.stafftrackapi.service.impl;

import com.swizima.stafftrackapi.dto.PageResponse;
import com.swizima.stafftrackapi.dto.StaffMemberRequest;
import com.swizima.stafftrackapi.dto.StaffMemberResponse;
import com.swizima.stafftrackapi.entity.StaffMember;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import com.swizima.stafftrackapi.entity.enums.StaffStatus;
import com.swizima.stafftrackapi.exception.DuplicateResourceException;
import com.swizima.stafftrackapi.exception.ResourceNotFoundException;
import com.swizima.stafftrackapi.repository.StaffMemberRepository;
import com.swizima.stafftrackapi.service.StaffMemberService;
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
public class StaffMemberServiceImpl implements StaffMemberService {

    private final StaffMemberRepository staffMemberRepository;

    @Override
    @Transactional
    public StaffMemberResponse registerStaff(StaffMemberRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (staffMemberRepository.existsByEmailIgnoreCaseAndRecordStatusNot(normalizedEmail, RecordStatus.DELETED)) {
            throw new DuplicateResourceException("Staff member with email '" + normalizedEmail + "' is already registered");
        }

        StaffMember staff = new StaffMember();
        staff.setStaffCode(generateStaffCode());
        staff.setRecordStatus(RecordStatus.ACTIVE);
        applyRequest(staff, request, normalizedEmail);

        return StaffMemberResponse.from(staffMemberRepository.save(staff));
    }

    @Override
    @Transactional
    public StaffMemberResponse updateStaff(UUID id, StaffMemberRequest request) {
        StaffMember staff = findStaff(id);
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        staffMemberRepository.findByEmailIgnoreCaseAndRecordStatusNot(normalizedEmail, RecordStatus.DELETED)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Staff member with email '" + normalizedEmail + "' is already registered");
                });

        applyRequest(staff, request, normalizedEmail);
        return StaffMemberResponse.from(staffMemberRepository.save(staff));
    }

    @Override
    public StaffMemberResponse getStaff(UUID id) {
        return StaffMemberResponse.from(findStaff(id));
    }

    @Override
    public List<StaffMemberResponse> getAllStaff() {
        return staffMemberRepository.findAllByRecordStatusNotOrderBySurnameAscLastNameAsc(RecordStatus.DELETED)
                .stream()
                .map(StaffMemberResponse::from)
                .toList();
    }

    @Override
    public PageResponse<StaffMemberResponse> searchStaff(String query,
                                                         String department,
                                                         String dutyStation,
                                                         StaffStatus status,
                                                         Pageable pageable) {
        return PageResponse.from(
                staffMemberRepository.search(query, department, dutyStation, status, RecordStatus.DELETED, pageable)
                        .map(StaffMemberResponse::from)
        );
    }

    @Override
    @Transactional
    public void deleteStaff(UUID id) {
        StaffMember staff = findStaff(id);
        staff.setRecordStatus(RecordStatus.DELETED);
        staffMemberRepository.save(staff);
    }

    private void applyRequest(StaffMember staff, StaffMemberRequest r, String normalizedEmail) {
        if (r.clientRef() != null) {
            staff.setClientRef(r.clientRef());
        }
        staff.setSurname(r.surname().trim());
        staff.setLastName(r.lastName().trim());
        staff.setDesignation(r.designation().trim());
        staff.setDesignationId(r.designationId());
        staff.setEmail(normalizedEmail);
        staff.setAlternativeEmail(r.alternativeEmail());
        staff.setPhone(r.phone().trim());
        staff.setAlternativePhone(r.alternativePhone());
        staff.setDepartment(r.department().trim());
        staff.setDepartmentId(r.departmentId());
        staff.setDutyStation(r.dutyStation().trim());
        staff.setDutyStationId(r.dutyStationId());
        staff.setStatus(r.status() != null ? r.status() : StaffStatus.ACTIVE);
    }

    private StaffMember findStaff(UUID id) {
        return staffMemberRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member with id '" + id + "' not found"));
    }

    private String generateStaffCode() {
        long nextNum = staffMemberRepository.count() + 101;
        return "STF-" + nextNum;
    }
}

