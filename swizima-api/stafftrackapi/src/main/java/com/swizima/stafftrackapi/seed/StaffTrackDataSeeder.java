package com.swizima.stafftrackapi.seed;

import com.swizima.stafftrackapi.entity.ClientUserAssignment;
import com.swizima.stafftrackapi.entity.Department;
import com.swizima.stafftrackapi.entity.DutyStation;
import com.swizima.stafftrackapi.entity.OfficerDesignation;
import com.swizima.stafftrackapi.entity.Organisation;
import com.swizima.stafftrackapi.entity.StaffMember;
import com.swizima.stafftrackapi.entity.Supervisor;
import com.swizima.stafftrackapi.entity.UserAccount;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import com.swizima.stafftrackapi.entity.enums.StaffStatus;
import com.swizima.stafftrackapi.repository.ClientUserAssignmentRepository;
import com.swizima.stafftrackapi.repository.DepartmentRepository;
import com.swizima.stafftrackapi.repository.DutyStationRepository;
import com.swizima.stafftrackapi.repository.OfficerDesignationRepository;
import com.swizima.stafftrackapi.repository.OrganisationRepository;
import com.swizima.stafftrackapi.repository.StaffMemberRepository;
import com.swizima.stafftrackapi.repository.SupervisorRepository;
import com.swizima.stafftrackapi.repository.UserAccountRepository;
import com.swizima.stafftrackapi.service.NotificationChannelConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Seeds the default OPM Organisation, Departments, Officer Designations, Duty Stations,
 * initial staff directory, and default user account (Kasoma Fredrick) on startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StaffTrackDataSeeder implements CommandLineRunner {

    private final OrganisationRepository organisationRepository;
    private final DepartmentRepository departmentRepository;
    private final OfficerDesignationRepository designationRepository;
    private final DutyStationRepository dutyStationRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final UserAccountRepository userAccountRepository;
    private final ClientUserAssignmentRepository clientUserAssignmentRepository;
    private final SupervisorRepository supervisorRepository;
    private final NotificationChannelConfigService notificationChannelConfigService;

    @Override
    @Transactional
    public void run(String... args) {
        Organisation opm = seedDefaultOrganisation();
        seedDepartments();
        seedDesignations();
        seedDutyStations();
        StaffMember fredrickStaff = seedStaffMembers();
        seedDefaultUserAndAssignment(opm, fredrickStaff);
        seedDefaultSupervisor(opm);
        notificationChannelConfigService.seedMissingDefaults();
    }

    private void seedDefaultSupervisor(Organisation opm) {
        if (supervisorRepository.count() > 0) {
            return;
        }
        Supervisor sup = Supervisor.builder()
                .clientRef(opm.getId())
                .entityName(opm.getName())
                .entityCode(opm.getCode())
                .name("Kasoma Fredrick")
                .email("kfredrick35@gmail.com")
                .phone("+256 700 408 387")
                .status("ACTIVE")
                .recordStatus(RecordStatus.ACTIVE)
                .build();
        supervisorRepository.save(sup);
        log.info("Seeded default Entity Supervisor for {}: kfredrick35@gmail.com", opm.getCode());
    }

    private Organisation seedDefaultOrganisation() {
        Optional<Organisation> existing = organisationRepository
                .findByCodeIgnoreCaseAndRecordStatusNot("OPM", RecordStatus.DELETED);
        if (existing.isPresent()) {
            return existing.get();
        }
        Organisation opm = Organisation.builder()
                .code("OPM")
                .name("Office of the Prime Minister (OPM)")
                .organisationType("Government Ministry / Agency")
                .contactPerson("Kasoma Fredrick")
                .email("kfredrick35@gmail.com")
                .phone("+256 700 408 387")
                .city("Kampala")
                .country("Uganda")
                .address("Plot 9-11 Apollo Kaggwa Road, Kampala")
                .status("ACTIVE")
                .recordStatus(RecordStatus.ACTIVE)
                .build();
        Organisation saved = organisationRepository.save(opm);
        log.info("Seeded default Organisation: {} ({})", saved.getName(), saved.getCode());
        return saved;
    }

    private void seedDepartments() {
        if (departmentRepository.count() > 0) {
            return;
        }
        List<Department> departments = List.of(
                dept("DOR", "Department of Refugees"),
                dept("DPMR", "Disaster Preparedness, Management and Refugees"),
                dept("PCME", "Policy Coordination, Monitoring and Evaluation"),
                dept("SP-REG", "Special Programs (Northern Uganda, Karamoja, Luwero–Rwenzori, and Teso)"),
                dept("ADMIN-FIN", "Administration and Finance"),
                dept("PMDU", "Prime Minister's Delivery Unit (PMDU)")
        );
        departmentRepository.saveAll(departments);
        log.info("Seeded {} OPM Departments / Units", departments.size());
    }

    private void seedDesignations() {
        if (designationRepository.count() > 0) {
            return;
        }
        List<OfficerDesignation> designations = List.of(
                des("PO", "Principal Officer"),
                des("SO", "Senior Officer"),
                des("RSIO", "Refugee Status Interviewing Officer"),
                des("APO", "Assistant Protection Officer"),
                des("CSO", "Community Services Officer"),
                des("SC", "Settlement Commandant"),
                des("RDO", "Refugee Desk Officer"),
                des("PRO", "Protection Officer"),
                des("ITO", "IT Officer"),
                des("LIO", "Legal Integration Officer"),
                des("MEO", "M&E Officer"),
                des("AMEO", "Ass. M&E Officer"),
                des("DRDO", "Deputy Refugee Desk Officer")
        );
        designationRepository.saveAll(designations);
        log.info("Seeded {} OPM Officer Designations", designations.size());
    }

    private void seedDutyStations() {
        if (dutyStationRepository.count() > 0) {
            return;
        }
        List<DutyStation> stations = List.of(
                stn("OPM-HQ", "OPM Main"),
                stn("DOR-KLA", "DOR-Kampala"),
                stn("MBR-RD", "Mbarara Refugee Desk"),
                stn("HMA-RD", "Hoima Refugee Desk"),
                stn("ARU-RD", "Arua Refugee Desk"),
                stn("ADJ-RD", "Adjumani Refugee Desk"),
                stn("LMW-RD", "Lamwo Refugee Desk"),
                stn("BDB-RS", "Bidibidi RS"),
                stn("LBL-RS", "Lobule RS"),
                stn("IMV-RS", "Imvepi RS"),
                stn("RHC-RS", "Rhino Camp RS"),
                stn("ADJ-RS", "Adjumani RS"),
                stn("PLR-RS", "Palorinya RS"),
                stn("PLB-RS", "Palabek RS"),
                stn("KRY-RS", "Kiryandongo RS"),
                stn("KYG-RS", "Kyangwali RS"),
                stn("NKV-RS", "Nakivale RS"),
                stn("ORC-RS", "Oruchinga RS"),
                stn("KYK-RS", "Kyaka II RS"),
                stn("RWM-RS", "Rwamwanja RS")
        );
        dutyStationRepository.saveAll(stations);
        log.info("Seeded {} OPM Duty Stations / Settlements", stations.size());
    }

    private StaffMember seedStaffMembers() {
        Optional<StaffMember> existing = staffMemberRepository
                .findByEmailIgnoreCaseAndRecordStatusNot("kfredrick35@gmail.com", RecordStatus.DELETED);
        if (existing.isPresent()) {
            return existing.get();
        }
        if (staffMemberRepository.count() > 0) {
            return staffMemberRepository.findAll().getFirst();
        }
        StaffMember fredrick = staff("STF-101", "Kasoma", "Fredrick", "Principal Officer", "kfredrick35@gmail.com",
                "fredrick.kasoma@opm.go.ug", "+256 700 408 387", "+256 772 408 387",
                "Department of Refugees", "DOR-Kampala");
        StaffMember saved = staffMemberRepository.save(fredrick);
        log.info("Seeded default OPM staff member: {} {}", saved.getSurname(), saved.getLastName());
        return saved;
    }

    private void seedDefaultUserAndAssignment(Organisation opm, StaffMember fredrickStaff) {
        UserAccount fredrickUser = userAccountRepository
                .findByEmailIgnoreCaseAndRecordStatusNot("kfredrick35@gmail.com", RecordStatus.DELETED)
                .orElseGet(() -> {
                    UserAccount u = UserAccount.builder()
                            .clientRef(opm.getId())
                            .staffId(fredrickStaff != null ? fredrickStaff.getId() : null)
                            .fullName("Kasoma Fredrick")
                            .username("kfredrick35@gmail.com")
                            .email("kfredrick35@gmail.com")
                            .phone("+256 700 408 387")
                            .designation("Principal Officer")
                            .department("Department of Refugees")
                            .dutyStation("DOR-Kampala")
                            .role("SUPER_ADMIN")
                            .preferredOtpChannel("EMAIL")
                            .status("ACTIVE")
                            .recordStatus(RecordStatus.ACTIVE)
                            .build();
                    UserAccount savedUser = userAccountRepository.save(u);
                    log.info("Seeded default System User: {} ({})", savedUser.getFullName(), savedUser.getEmail());
                    return savedUser;
                });

        boolean hasOpmAssignment = clientUserAssignmentRepository
                .findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus.DELETED)
                .stream()
                .anyMatch(cu -> "kfredrick35@gmail.com".equalsIgnoreCase(cu.getUserEmail()));

        if (!hasOpmAssignment) {
            ClientUserAssignment link = ClientUserAssignment.builder()
                    .clientRef(opm.getId())
                    .systemUserId(fredrickUser.getId())
                    .userPhoneNumber("+256 700 408 387")
                    .userEmail("kfredrick35@gmail.com")
                    .accountCategory("SUPER_ADMIN")
                    .clientName(opm.getName())
                    .clientCode(opm.getCode())
                    .defaultClient(true)
                    .activationStatus("ACTIVE")
                    .recordStatus(RecordStatus.ACTIVE)
                    .build();
            clientUserAssignmentRepository.save(link);
            log.info("Seeded default ClientUserAssignment linking kfredrick35@gmail.com to OPM");
        }
    }

    private Department dept(String code, String name) {
        return Department.builder()
                .code(code)
                .name(name)
                .recordStatus(RecordStatus.ACTIVE)
                .build();
    }

    private OfficerDesignation des(String code, String name) {
        return OfficerDesignation.builder()
                .code(code)
                .name(name)
                .recordStatus(RecordStatus.ACTIVE)
                .build();
    }

    private DutyStation stn(String code, String name) {
        return DutyStation.builder()
                .code(code)
                .name(name)
                .recordStatus(RecordStatus.ACTIVE)
                .build();
    }

    private StaffMember staff(String code, String surname, String lastName, String designation,
                              String email, String altEmail, String phone, String altPhone,
                              String department, String dutyStation) {
        return StaffMember.builder()
                .staffCode(code)
                .surname(surname)
                .lastName(lastName)
                .designation(designation)
                .email(email)
                .alternativeEmail(altEmail)
                .phone(phone)
                .alternativePhone(altPhone)
                .department(department)
                .dutyStation(dutyStation)
                .status(StaffStatus.ACTIVE)
                .recordStatus(RecordStatus.ACTIVE)
                .build();
    }
}
