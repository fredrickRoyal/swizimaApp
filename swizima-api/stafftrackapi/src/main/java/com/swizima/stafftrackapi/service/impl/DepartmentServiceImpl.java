package com.swizima.stafftrackapi.service.impl;

import com.swizima.stafftrackapi.dto.MasterLookupRequest;
import com.swizima.stafftrackapi.dto.MasterLookupResponse;
import com.swizima.stafftrackapi.dto.PageResponse;
import com.swizima.stafftrackapi.entity.Department;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import com.swizima.stafftrackapi.exception.DuplicateResourceException;
import com.swizima.stafftrackapi.exception.ResourceNotFoundException;
import com.swizima.stafftrackapi.repository.DepartmentRepository;
import com.swizima.stafftrackapi.service.DepartmentService;
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
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Override
    @Transactional
    public MasterLookupResponse createDepartment(MasterLookupRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        String name = request.name().trim();

        if (departmentRepository.existsByCodeIgnoreCaseAndRecordStatusNot(code, RecordStatus.DELETED)) {
            throw new DuplicateResourceException("Department with code '" + code + "' already exists");
        }
        if (departmentRepository.existsByNameIgnoreCaseAndRecordStatusNot(name, RecordStatus.DELETED)) {
            throw new DuplicateResourceException("Department with name '" + name + "' already exists");
        }

        Department dept = Department.builder()
                .clientRef(request.clientRef())
                .code(code)
                .name(name)
                .description(request.description())
                .recordStatus(RecordStatus.ACTIVE)
                .build();
        return MasterLookupResponse.fromDepartment(departmentRepository.save(dept));
    }

    @Override
    @Transactional
    public MasterLookupResponse updateDepartment(UUID id, MasterLookupRequest request) {
        Department dept = findDepartment(id);
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        String name = request.name().trim();

        departmentRepository.findByCodeIgnoreCaseAndRecordStatusNot(code, RecordStatus.DELETED)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Department with code '" + code + "' already exists");
                });
        departmentRepository.findByNameIgnoreCaseAndRecordStatusNot(name, RecordStatus.DELETED)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Department with name '" + name + "' already exists");
                });

        if (request.clientRef() != null) {
            dept.setClientRef(request.clientRef());
        }
        dept.setCode(code);
        dept.setName(name);
        dept.setDescription(request.description());
        return MasterLookupResponse.fromDepartment(departmentRepository.save(dept));
    }

    @Override
    public MasterLookupResponse getDepartment(UUID id) {
        return MasterLookupResponse.fromDepartment(findDepartment(id));
    }

    @Override
    public List<MasterLookupResponse> getAllDepartments() {
        return departmentRepository.findAllByRecordStatusNotOrderByNameAsc(RecordStatus.DELETED)
                .stream()
                .map(MasterLookupResponse::fromDepartment)
                .toList();
    }

    @Override
    public PageResponse<MasterLookupResponse> searchDepartments(String query, Pageable pageable) {
        return PageResponse.from(
                departmentRepository.search(query, RecordStatus.DELETED, pageable)
                        .map(MasterLookupResponse::fromDepartment)
        );
    }

    @Override
    @Transactional
    public void deleteDepartment(UUID id) {
        Department dept = findDepartment(id);
        dept.setRecordStatus(RecordStatus.DELETED);
        departmentRepository.save(dept);
    }

    private Department findDepartment(UUID id) {
        return departmentRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Department with id '" + id + "' not found"));
    }
}

