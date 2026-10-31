package com.swizima.clienttransactionsapi.dto;

import com.swizima.clienttransactionsapi.entity.enums.IdDocumentType;
import com.swizima.clienttransactionsapi.entity.enums.PaymentStatus;
import com.swizima.clienttransactionsapi.entity.enums.VehicleClearanceStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Create/update payload. Entry/exit date and time parts are derived from the datetimes.
 * If {@code entryDateTime} is omitted on create, the current time is used.
 */
public record VehicleServiceTransactionRequest(
        @NotNull UUID clientRef,
        @NotBlank @Size(max = 30) String customerPhoneNumber,
        @Size(max = 30) String customerAlternativePhone,
        @NotBlank @Size(max = 20) String vehicleNumberPlate,
        @NotBlank @Size(max = 150) String serviceType,
        @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal amountCharged,
        @Size(max = 150) String assignedTo,
        Boolean vehicleKeyShared,
        LocalDateTime entryDateTime,
        LocalDateTime expectedExitTime,
        LocalDateTime actualExitDateTime,
        @Size(max = 150) String customerName,
        @Size(max = 100) String customerNationality,
        IdDocumentType customerIdDocumentType,
        @Size(max = 50) String customerIdNumber,
        @Size(max = 255) String customerAddress,
        String generalComment,
        PaymentStatus paymentStatus,
        VehicleClearanceStatus vehicleClearanceStatus
) {
}
