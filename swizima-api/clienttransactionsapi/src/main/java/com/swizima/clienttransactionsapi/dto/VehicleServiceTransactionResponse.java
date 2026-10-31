package com.swizima.clienttransactionsapi.dto;

import com.swizima.clienttransactionsapi.entity.VehicleServiceTransaction;
import com.swizima.clienttransactionsapi.entity.enums.IdDocumentType;
import com.swizima.clienttransactionsapi.entity.enums.PaymentStatus;
import com.swizima.clienttransactionsapi.entity.enums.RecordStatus;
import com.swizima.clienttransactionsapi.entity.enums.VehicleClearanceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record VehicleServiceTransactionResponse(
        UUID id,
        UUID clientRef,
        String customerPhoneNumber,
        String customerAlternativePhone,
        String vehicleNumberPlate,
        String serviceType,
        BigDecimal amountCharged,
        BigDecimal amountPaid,
        BigDecimal balance,
        String assignedTo,
        Boolean vehicleKeyShared,
        LocalDateTime entryDateTime,
        LocalDate entryDate,
        LocalTime entryTime,
        LocalDateTime expectedExitTime,
        LocalDateTime actualExitDateTime,
        LocalDate actualExitDate,
        LocalTime actualExitTime,
        String releasedBy,
        LocalDateTime clearedAt,
        String clearedBy,
        String customerName,
        String customerNationality,
        IdDocumentType customerIdDocumentType,
        String customerIdNumber,
        String customerAddress,
        String generalComment,
        PaymentStatus paymentStatus,
        VehicleClearanceStatus vehicleClearanceStatus,
        RecordStatus recordStatus,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt
) {
    public static VehicleServiceTransactionResponse from(VehicleServiceTransaction t) {
        return new VehicleServiceTransactionResponse(
                t.getId(), t.getClientRef(), t.getCustomerPhoneNumber(), t.getCustomerAlternativePhone(),
                t.getVehicleNumberPlate(), t.getServiceType(), t.getAmountCharged(), t.getAmountPaid(),
                t.getAmountCharged().subtract(t.getAmountPaid()).max(java.math.BigDecimal.ZERO), t.getAssignedTo(),
                t.getVehicleKeyShared(), t.getEntryDateTime(), t.getEntryDate(), t.getEntryTime(),
                t.getExpectedExitTime(), t.getActualExitDateTime(), t.getActualExitDate(), t.getActualExitTime(),
                t.getReleasedBy(), t.getClearedAt(), t.getClearedBy(),
                t.getCustomerName(), t.getCustomerNationality(), t.getCustomerIdDocumentType(),
                t.getCustomerIdNumber(), t.getCustomerAddress(), t.getGeneralComment(),
                t.getPaymentStatus(), t.getVehicleClearanceStatus(), t.getRecordStatus(),
                t.getCreatedBy(), t.getCreatedAt(), t.getUpdatedBy(), t.getUpdatedAt());
    }
}
