package com.swizima.clienttransactionsapi.service;

import com.swizima.clienttransactionsapi.dto.TransactionSummaryResponse;
import com.swizima.clienttransactionsapi.dto.TransactionSummaryResponse.DailySummary;
import com.swizima.clienttransactionsapi.dto.TransactionSummaryResponse.StaffSummary;
import com.swizima.clienttransactionsapi.dto.TransactionSummaryResponse.Totals;
import com.swizima.clienttransactionsapi.entity.enums.PaymentStatus;
import com.swizima.clienttransactionsapi.entity.enums.RecordStatus;
import com.swizima.clienttransactionsapi.entity.enums.VehicleClearanceStatus;
import com.swizima.clienttransactionsapi.exception.BusinessRuleException;
import com.swizima.clienttransactionsapi.repository.VehicleServiceTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/** Builds the transaction summary report from a single grouped aggregate query. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionReportService {

    /** Upper bound on the report window to keep the aggregation cheap. */
    private static final long MAX_DAYS = 366;

    private final VehicleServiceTransactionRepository repository;

    public TransactionSummaryResponse summary(UUID clientRef, LocalDate fromDate, LocalDate toDate) {
        LocalDate from = fromDate != null ? fromDate : (toDate != null ? toDate : LocalDate.now());
        LocalDate to = toDate != null ? toDate : from;
        if (to.isBefore(from)) {
            throw new BusinessRuleException("toDate must be on or after fromDate");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_DAYS) {
            throw new BusinessRuleException("Report period cannot exceed " + MAX_DAYS + " days");
        }

        Acc total = new Acc();
        Map<String, Acc> byPayment = new LinkedHashMap<>();
        for (PaymentStatus s : PaymentStatus.values()) byPayment.put(s.name(), new Acc());
        Map<String, Acc> byClearance = new LinkedHashMap<>();
        for (VehicleClearanceStatus s : VehicleClearanceStatus.values()) byClearance.put(s.name(), new Acc());
        Map<String, StaffAcc> byStaff = new HashMap<>();
        Map<LocalDate, Acc> daily = new TreeMap<>();

        for (Object[] row : repository.summarize(clientRef, from, to, RecordStatus.DELETED)) {
            LocalDate date = (LocalDate) row[0];
            String staff = normalize((String) row[1]);
            PaymentStatus payment = (PaymentStatus) row[2];
            VehicleClearanceStatus clearance = (VehicleClearanceStatus) row[3];
            long count = ((Number) row[4]).longValue();
            BigDecimal charged = toDecimal(row[5]);
            BigDecimal paid = toDecimal(row[6]);
            boolean cancelled = payment == PaymentStatus.CANCELLED;

            total.add(count, charged, paid, cancelled);
            byPayment.get(payment.name()).add(count, charged, paid, cancelled);
            byClearance.get(clearance.name()).add(count, charged, paid, cancelled);
            daily.computeIfAbsent(date, d -> new Acc()).add(count, charged, paid, cancelled);

            // Group staff case-insensitively, keep the first spelling seen as the display name.
            String key = staff == null ? "" : staff.toLowerCase();
            StaffAcc s = byStaff.computeIfAbsent(key, k -> new StaffAcc(staff));
            s.totals.add(count, charged, paid, cancelled);
            s.payment.merge(payment.name(), count, Long::sum);
            s.clearance.merge(clearance.name(), count, Long::sum);
        }

        List<StaffSummary> staff = new ArrayList<>();
        byStaff.values().stream()
                .sorted(Comparator.comparingLong((StaffAcc s) -> s.totals.count).reversed()
                        .thenComparing(s -> s.name == null ? "\uffff" : s.name.toLowerCase()))
                .forEach(s -> staff.add(new StaffSummary(s.name, s.totals.toTotals(),
                        withAll(s.payment, PaymentStatus.values()), withAll(s.clearance, VehicleClearanceStatus.values()))));

        List<DailySummary> days = new ArrayList<>();
        daily.forEach((d, acc) -> days.add(new DailySummary(d, acc.toTotals())));

        return new TransactionSummaryResponse(clientRef, from, to, total.toTotals(),
                toTotals(byPayment), toTotals(byClearance), staff, days);
    }

    private static String normalize(String name) {
        return name == null || name.isBlank() ? null : name.trim();
    }

    private static BigDecimal toDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        return value instanceof BigDecimal bd ? bd : new BigDecimal(value.toString());
    }

    private static Map<String, Totals> toTotals(Map<String, Acc> in) {
        Map<String, Totals> out = new LinkedHashMap<>();
        in.forEach((k, v) -> out.put(k, v.toTotals()));
        return out;
    }

    private static Map<String, Long> withAll(Map<String, Long> counts, Enum<?>[] values) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (Enum<?> v : values) out.put(v.name(), counts.getOrDefault(v.name(), 0L));
        return out;
    }

    private static final class Acc {
        long count;
        BigDecimal charged = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;

        void add(long n, BigDecimal c, BigDecimal p, boolean cancelled) {
            count += n;
            if (!cancelled) {
                charged = charged.add(c);
                paid = paid.add(p);
            }
        }

        Totals toTotals() {
            return new Totals(count, charged, paid, charged.subtract(paid).max(BigDecimal.ZERO));
        }
    }

    private static final class StaffAcc {
        final String name;
        final Acc totals = new Acc();
        final Map<String, Long> payment = new HashMap<>();
        final Map<String, Long> clearance = new HashMap<>();

        StaffAcc(String name) {
            this.name = name;
        }
    }
}
