package com.swizima.clienttransactionsapi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Transactions report for a client over a period.
 * Money totals exclude CANCELLED transactions; counts include them.
 */
public record TransactionSummaryResponse(
        UUID clientRef,
        LocalDate fromDate,
        LocalDate toDate,
        Totals totals,
        Map<String, Totals> byPaymentStatus,
        Map<String, Totals> byClearanceStatus,
        List<StaffSummary> byStaff,
        List<DailySummary> daily
) {

    public record Totals(long count, BigDecimal amountCharged, BigDecimal amountPaid, BigDecimal outstanding) {
    }

    /** Work done by one assigned staff member; {@code assignedTo} is null for unassigned transactions. */
    public record StaffSummary(
            String assignedTo,
            Totals totals,
            Map<String, Long> paymentStatusCounts,
            Map<String, Long> clearanceStatusCounts
    ) {
    }

    public record DailySummary(LocalDate date, Totals totals) {
    }
}
