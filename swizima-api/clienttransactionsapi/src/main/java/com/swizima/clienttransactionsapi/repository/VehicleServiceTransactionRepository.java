package com.swizima.clienttransactionsapi.repository;

import com.swizima.clienttransactionsapi.entity.VehicleServiceTransaction;
import com.swizima.clienttransactionsapi.entity.enums.PaymentStatus;
import com.swizima.clienttransactionsapi.entity.enums.RecordStatus;
import com.swizima.clienttransactionsapi.entity.enums.VehicleClearanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleServiceTransactionRepository extends JpaRepository<VehicleServiceTransaction, UUID> {

    Optional<VehicleServiceTransaction> findByIdAndRecordStatusNot(UUID id, RecordStatus recordStatus);

    /** Flexible listing: any filter passed as null is ignored. */
    @Query("""
            SELECT t FROM VehicleServiceTransaction t
            WHERE t.recordStatus <> :excluded
              AND (:clientRef       IS NULL OR t.clientRef = :clientRef)
              AND (:paymentStatus   IS NULL OR t.paymentStatus = :paymentStatus)
              AND (:clearanceStatus IS NULL OR t.vehicleClearanceStatus = :clearanceStatus)
              AND (CAST(:fromDate AS date) IS NULL OR t.entryDate >= :fromDate)
              AND (CAST(:toDate   AS date) IS NULL OR t.entryDate <= :toDate)
              AND (:search IS NULL
                   OR LOWER(t.vehicleNumberPlate)  LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(t.customerName)        LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(t.customerPhoneNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(t.serviceType)         LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(t.assignedTo)          LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<VehicleServiceTransaction> search(@Param("clientRef") UUID clientRef,
                                           @Param("paymentStatus") PaymentStatus paymentStatus,
                                           @Param("clearanceStatus") VehicleClearanceStatus clearanceStatus,
                                           @Param("fromDate") LocalDate fromDate,
                                           @Param("toDate") LocalDate toDate,
                                           @Param("search") String search,
                                           @Param("excluded") RecordStatus excluded,
                                           Pageable pageable);
}
