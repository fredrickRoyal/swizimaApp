package com.swizima.clienttransactionsapi.service;

import com.swizima.clienttransactionsapi.dto.PageResponse;
import com.swizima.clienttransactionsapi.dto.TransactionPaymentRequest;
import com.swizima.clienttransactionsapi.dto.TransactionPaymentResponse;
import com.swizima.clienttransactionsapi.dto.VehicleActionRequest;
import com.swizima.clienttransactionsapi.dto.VehicleServiceTransactionRequest;
import com.swizima.clienttransactionsapi.dto.VehicleServiceTransactionResponse;
import com.swizima.clienttransactionsapi.entity.TransactionPayment;
import com.swizima.clienttransactionsapi.entity.VehicleServiceTransaction;
import com.swizima.clienttransactionsapi.entity.enums.PaymentStatus;
import com.swizima.clienttransactionsapi.entity.enums.RecordStatus;
import com.swizima.clienttransactionsapi.entity.enums.VehicleClearanceStatus;
import com.swizima.clienttransactionsapi.exception.BusinessRuleException;
import com.swizima.clienttransactionsapi.exception.ResourceNotFoundException;
import com.swizima.clienttransactionsapi.repository.TransactionPaymentRepository;
import com.swizima.clienttransactionsapi.repository.VehicleServiceTransactionRepository;
import com.swizima.clienttransactionsapi.billing.BillingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Vehicle service workflow:
 * IN_SERVICE / READY --(fully paid)--> CLEARED --(exit)--> RELEASED.
 * Payment status is derived from recorded payments (CANCELLED is the only manual value).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class VehicleServiceTransactionService {

    private final VehicleServiceTransactionRepository repository;
    private final TransactionPaymentRepository paymentRepository;
    private final BillingClient billingClient;

    /** Kill switch for per-transaction billing (app.billing.enabled). */
    @Value("${app.billing.enabled:true}")
    private boolean billingEnabled;

    // ------------------------------------------------------------------ CRUD

    /**
     * Saves the transaction, then deducts the client's billing percentage. If billing fails
     * (e.g. insufficient credit) a {@link com.swizima.clienttransactionsapi.exception.BillingException}
     * is thrown and the whole transaction is rolled back, i.e. blocked.
     */
    public VehicleServiceTransactionResponse create(VehicleServiceTransactionRequest request) {
        VehicleServiceTransaction t = new VehicleServiceTransaction();
        t.setAmountPaid(BigDecimal.ZERO);
        apply(t, request, true);
        derivePaymentStatus(t, request.paymentStatus());
        VehicleServiceTransaction saved = repository.saveAndFlush(t);
        chargeBilling(saved);
        return VehicleServiceTransactionResponse.from(saved);
    }

    private void chargeBilling(VehicleServiceTransaction t) {
        if (!billingEnabled) return;
        String ref = t.getId().toString();
        billingClient.charge(t.getClientRef(), ref, t.getAmountCharged(),
                t.getServiceType() + " - " + t.getVehicleNumberPlate());
        // If our DB commit fails after billing succeeded, refund the charge so the client isn't billed
        // for a transaction that doesn't exist.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        billingClient.reverseQuietly(ref, "Transaction was not saved");
                    }
                }
            });
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<VehicleServiceTransactionResponse> list(UUID clientRef, PaymentStatus paymentStatus,
                                                                VehicleClearanceStatus clearanceStatus,
                                                                LocalDate fromDate, LocalDate toDate,
                                                                String assignedTo, String search,
                                                                Pageable pageable) {
        String term = (search == null || search.isBlank()) ? null : search.trim();
        // "-" selects unassigned transactions (matched as an empty name).
        String staff = assignedTo == null || assignedTo.isBlank() ? null
                : "-".equals(assignedTo.trim()) ? "" : assignedTo.trim();
        return PageResponse.from(repository
                .search(clientRef, paymentStatus, clearanceStatus, fromDate, toDate, staff, term,
                        RecordStatus.DELETED, pageable)
                .map(VehicleServiceTransactionResponse::from));
    }

    @Transactional(readOnly = true)
    public VehicleServiceTransactionResponse get(UUID id) {
        return VehicleServiceTransactionResponse.from(find(id));
    }

    public VehicleServiceTransactionResponse update(UUID id, VehicleServiceTransactionRequest request) {
        VehicleServiceTransaction t = find(id);
        if (request.amountCharged().compareTo(t.getAmountPaid()) < 0) {
            throw new BusinessRuleException("Amount charged cannot be less than the " + t.getAmountPaid() + " already paid");
        }
        apply(t, request, false);
        derivePaymentStatus(t, request.paymentStatus());
        return VehicleServiceTransactionResponse.from(repository.saveAndFlush(t));
    }

    /** Soft delete. */
    public void delete(UUID id) {
        find(id).setRecordStatus(RecordStatus.DELETED);
    }

    // ------------------------------------------------------------------ Workflow

    /** Marks the vehicle as cleared to leave. Requires the full amount to be paid. */
    public VehicleServiceTransactionResponse clear(UUID id, VehicleActionRequest request) {
        VehicleServiceTransaction t = find(id);
        if (!isOpen(t)) {
            throw new BusinessRuleException("Vehicle " + t.getVehicleNumberPlate() + " is already " + label(t.getVehicleClearanceStatus()));
        }
        if (t.getPaymentStatus() == PaymentStatus.CANCELLED) {
            throw new BusinessRuleException("This transaction is cancelled and cannot be cleared");
        }
        BigDecimal balance = balance(t);
        if (balance.signum() > 0) {
            throw new BusinessRuleException("Outstanding balance of " + balance.toPlainString() + " must be paid before clearing");
        }
        LocalDateTime at = request != null && request.at() != null ? request.at() : LocalDateTime.now();
        if (at.isBefore(t.getEntryDateTime())) {
            throw new BusinessRuleException("Clearance time cannot be before the entry time");
        }
        t.setVehicleClearanceStatus(VehicleClearanceStatus.CLEARED);
        t.setClearedAt(at);
        t.setClearedBy(request == null ? null : blankToNull(request.by()));
        // Clearance also records the actual exit (date/time parts are derived on save);
        // the Exit action overwrites it if the vehicle actually leaves later.
        t.setActualExitDateTime(at);
        appendComment(t, request);
        return VehicleServiceTransactionResponse.from(repository.saveAndFlush(t));
    }

    /** Records the vehicle leaving the premises. Only allowed once cleared. */
    public VehicleServiceTransactionResponse exit(UUID id, VehicleActionRequest request) {
        VehicleServiceTransaction t = find(id);
        if (t.getVehicleClearanceStatus() != VehicleClearanceStatus.CLEARED) {
            throw new BusinessRuleException(t.getVehicleClearanceStatus() == VehicleClearanceStatus.RELEASED
                    ? "Vehicle " + t.getVehicleNumberPlate() + " has already exited"
                    : "Vehicle " + t.getVehicleNumberPlate() + " must be cleared before it can exit");
        }
        LocalDateTime at = request != null && request.at() != null ? request.at() : LocalDateTime.now();
        if (at.isBefore(t.getEntryDateTime())) {
            throw new BusinessRuleException("Exit time cannot be before the entry time");
        }
        t.setVehicleClearanceStatus(VehicleClearanceStatus.RELEASED);
        t.setActualExitDateTime(at);
        t.setReleasedBy(request == null ? null : blankToNull(request.by()));
        appendComment(t, request);
        return VehicleServiceTransactionResponse.from(repository.saveAndFlush(t));
    }

    // ------------------------------------------------------------------ Payments

    @Transactional(readOnly = true)
    public List<TransactionPaymentResponse> payments(UUID transactionId) {
        find(transactionId);
        return paymentRepository
                .findByTransactionIdAndRecordStatusNotOrderByPaymentDateTimeDesc(transactionId, RecordStatus.DELETED)
                .stream().map(TransactionPaymentResponse::from).toList();
    }

    public TransactionPaymentResponse addPayment(UUID transactionId, TransactionPaymentRequest r) {
        VehicleServiceTransaction t = find(transactionId);
        if (t.getPaymentStatus() == PaymentStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot record a payment on a cancelled transaction");
        }
        BigDecimal balance = balance(t);
        if (balance.signum() <= 0) {
            throw new BusinessRuleException("This transaction is already fully paid");
        }
        if (r.amount().compareTo(balance) > 0) {
            throw new BusinessRuleException("Payment of " + r.amount().toPlainString()
                    + " exceeds the outstanding balance of " + balance.toPlainString());
        }
        TransactionPayment p = new TransactionPayment();
        p.setTransaction(t);
        p.setAmount(r.amount());
        p.setPaymentMethod(r.paymentMethod());
        p.setReferenceNumber(r.referenceNumber() == null || r.referenceNumber().isBlank()
                ? null : r.referenceNumber().trim().toUpperCase(Locale.ROOT));
        p.setPaidByName(blankToNull(r.paidByName()));
        p.setPaidByPhone(blankToNull(r.paidByPhone()));
        p.setReceivedBy(blankToNull(r.receivedBy()));
        p.setPaymentDateTime(r.paymentDateTime() != null ? r.paymentDateTime() : LocalDateTime.now());
        p.setComment(blankToNull(r.comment()));
        TransactionPayment saved = paymentRepository.saveAndFlush(p);
        refreshPaid(t);
        return TransactionPaymentResponse.from(saved);
    }

    /** Soft-deletes a payment (e.g. entered by mistake). Not allowed once the vehicle is cleared. */
    public void deletePayment(UUID transactionId, UUID paymentId) {
        VehicleServiceTransaction t = find(transactionId);
        if (!isOpen(t)) {
            throw new BusinessRuleException("Payments cannot be removed after the vehicle is " + label(t.getVehicleClearanceStatus()));
        }
        TransactionPayment p = paymentRepository
                .findByIdAndTransactionIdAndRecordStatusNot(paymentId, transactionId, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        p.setRecordStatus(RecordStatus.DELETED);
        paymentRepository.flush();
        refreshPaid(t);
    }

    // ------------------------------------------------------------------ Helpers

    private VehicleServiceTransaction find(UUID id) {
        return repository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle service transaction not found: " + id));
    }

    private void refreshPaid(VehicleServiceTransaction t) {
        t.setAmountPaid(paymentRepository.sumPaid(t.getId(), RecordStatus.DELETED));
        derivePaymentStatus(t, null);
        repository.saveAndFlush(t);
    }

    /** PENDING / PARTIALLY_PAID / PAID from the amounts; CANCELLED only when explicitly requested. */
    private static void derivePaymentStatus(VehicleServiceTransaction t, PaymentStatus requested) {
        if (requested == PaymentStatus.CANCELLED
                || (requested == null && t.getPaymentStatus() == PaymentStatus.CANCELLED)) {
            t.setPaymentStatus(PaymentStatus.CANCELLED);
            return;
        }
        BigDecimal paid = t.getAmountPaid() == null ? BigDecimal.ZERO : t.getAmountPaid();
        if (paid.signum() <= 0) {
            t.setPaymentStatus(t.getAmountCharged().signum() == 0 ? PaymentStatus.PAID : PaymentStatus.PENDING);
        } else if (paid.compareTo(t.getAmountCharged()) < 0) {
            t.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        } else {
            t.setPaymentStatus(PaymentStatus.PAID);
        }
    }

    private static BigDecimal balance(VehicleServiceTransaction t) {
        return t.getAmountCharged().subtract(t.getAmountPaid() == null ? BigDecimal.ZERO : t.getAmountPaid());
    }

    private static boolean isOpen(VehicleServiceTransaction t) {
        return t.getVehicleClearanceStatus() == VehicleClearanceStatus.IN_SERVICE
                || t.getVehicleClearanceStatus() == VehicleClearanceStatus.READY;
    }

    private static String label(VehicleClearanceStatus s) {
        return s.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static void appendComment(VehicleServiceTransaction t, VehicleActionRequest r) {
        String note = r == null ? null : blankToNull(r.comment());
        if (note == null) return;
        t.setGeneralComment(t.getGeneralComment() == null ? note : t.getGeneralComment() + "\n" + note);
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private static void apply(VehicleServiceTransaction t, VehicleServiceTransactionRequest r, boolean creating) {
        t.setClientRef(r.clientRef());
        t.setCustomerPhoneNumber(r.customerPhoneNumber().trim());
        t.setCustomerAlternativePhone(blankToNull(r.customerAlternativePhone()));
        // "uap 123x" -> "UAP 123X"
        t.setVehicleNumberPlate(r.vehicleNumberPlate().trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT));
        t.setServiceType(r.serviceType().trim());
        t.setAmountCharged(r.amountCharged());
        t.setAssignedTo(blankToNull(r.assignedTo()));
        t.setVehicleKeyShared(Boolean.TRUE.equals(r.vehicleKeyShared()));
        if (r.entryDateTime() != null) t.setEntryDateTime(r.entryDateTime());
        t.setExpectedExitTime(r.expectedExitTime());
        // Exit time is set by the exit action; edits may only correct it after release.
        if (t.getVehicleClearanceStatus() == VehicleClearanceStatus.RELEASED && r.actualExitDateTime() != null) {
            t.setActualExitDateTime(r.actualExitDateTime());
        }
        t.setCustomerName(blankToNull(r.customerName()));
        t.setCustomerNationality(blankToNull(r.customerNationality()));
        t.setCustomerIdDocumentType(r.customerIdDocumentType());
        t.setCustomerIdNumber(r.customerIdNumber() == null || r.customerIdNumber().isBlank()
                ? null : r.customerIdNumber().trim().toUpperCase(Locale.ROOT));
        t.setCustomerAddress(blankToNull(r.customerAddress()));
        t.setGeneralComment(blankToNull(r.generalComment()));
        // New transactions always start IN_SERVICE (any requested status is ignored).
        // Edits may only move between IN_SERVICE and READY; CLEARED/RELEASED go through clear()/exit().
        if (creating) {
            t.setVehicleClearanceStatus(VehicleClearanceStatus.IN_SERVICE);
        } else {
            VehicleClearanceStatus wanted = r.vehicleClearanceStatus();
            boolean manual = wanted == VehicleClearanceStatus.IN_SERVICE || wanted == VehicleClearanceStatus.READY;
            if (manual && isOpen(t)) {
                t.setVehicleClearanceStatus(wanted);
            }
        }
    }
}
