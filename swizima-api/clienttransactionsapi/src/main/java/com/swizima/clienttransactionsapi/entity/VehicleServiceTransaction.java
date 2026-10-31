package com.swizima.clienttransactionsapi.entity;

import com.swizima.clienttransactionsapi.entity.enums.IdDocumentType;
import com.swizima.clienttransactionsapi.entity.enums.PaymentStatus;
import com.swizima.clienttransactionsapi.entity.enums.VehicleClearanceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * A vehicle brought to a client (e.g. car wash / garage) for a service.
 * {@code clientRef} is the id of the client in clientapi (separate database, so no FK).
 * Entry/exit date and time columns are derived from their datetime counterparts.
 */
@Entity
@Table(name = "vehicle_service_transactions", indexes = {
        @Index(name = "idx_vst_client_ref", columnList = "client_ref"),
        @Index(name = "idx_vst_number_plate", columnList = "vehicle_number_plate"),
        @Index(name = "idx_vst_entry_date", columnList = "entry_date")
})
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class VehicleServiceTransaction extends ParentEntity {

    @Column(name = "client_ref", nullable = false)
    private UUID clientRef;

    // ---- Customer ----
    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "customer_phone_number", nullable = false, length = 30)
    private String customerPhoneNumber;

    @Column(name = "customer_alternative_phone", length = 30)
    private String customerAlternativePhone;

    @Column(name = "customer_nationality", length = 100)
    private String customerNationality;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_id_document_type", length = 30)
    private IdDocumentType customerIdDocumentType;

    @Column(name = "customer_id_number", length = 50)
    private String customerIdNumber;

    @Column(name = "customer_address", length = 255)
    private String customerAddress;

    // ---- Vehicle & service ----
    @Column(name = "vehicle_number_plate", nullable = false, length = 20)
    private String vehicleNumberPlate;

    @Column(name = "service_type", nullable = false, length = 150)
    private String serviceType;

    @Column(name = "amount_charged", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountCharged;

    /** Sum of non-deleted payments; kept in sync by the service. */
    @Builder.Default
    @Column(name = "amount_paid", nullable = false, precision = 15, scale = 2,
            columnDefinition = "NUMERIC(15,2) NOT NULL DEFAULT 0")
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "assigned_to", length = 150)
    private String assignedTo;

    @Builder.Default
    @Column(name = "vehicle_key_shared", nullable = false)
    private Boolean vehicleKeyShared = Boolean.FALSE;

    // ---- Entry ----
    @Column(name = "entry_date_time", nullable = false)
    private LocalDateTime entryDateTime;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "entry_time", nullable = false)
    private LocalTime entryTime;

    @Column(name = "expected_exit_time")
    private LocalDateTime expectedExitTime;

    // ---- Exit ----
    @Column(name = "actual_exit_date_time")
    private LocalDateTime actualExitDateTime;

    @Column(name = "actual_exit_date")
    private LocalDate actualExitDate;

    @Column(name = "actual_exit_time")
    private LocalTime actualExitTime;

    @Column(name = "released_by", length = 150)
    private String releasedBy;

    // ---- Clearance ----
    @Column(name = "cleared_at")
    private LocalDateTime clearedAt;

    @Column(name = "cleared_by", length = 150)
    private String clearedBy;

    // ---- Status ----
    @Column(name = "general_comment", columnDefinition = "TEXT")
    private String generalComment;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_clearance_status", nullable = false, length = 20)
    private VehicleClearanceStatus vehicleClearanceStatus = VehicleClearanceStatus.IN_SERVICE;

    /** Keep split date/time columns in sync with the datetime columns. */
    @PrePersist
    @PreUpdate
    void syncDateParts() {
        if (entryDateTime == null) {
            entryDateTime = LocalDateTime.now();
        }
        entryDate = entryDateTime.toLocalDate();
        entryTime = entryDateTime.toLocalTime();
        actualExitDate = actualExitDateTime == null ? null : actualExitDateTime.toLocalDate();
        actualExitTime = actualExitDateTime == null ? null : actualExitDateTime.toLocalTime();
        if (paymentStatus == null) paymentStatus = PaymentStatus.PENDING;
        if (vehicleClearanceStatus == null) vehicleClearanceStatus = VehicleClearanceStatus.IN_SERVICE;
        if (vehicleKeyShared == null) vehicleKeyShared = Boolean.FALSE;
        if (amountPaid == null) amountPaid = BigDecimal.ZERO;
    }
}
