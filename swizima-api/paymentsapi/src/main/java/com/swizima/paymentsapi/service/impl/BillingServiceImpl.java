package com.swizima.paymentsapi.service.impl;

import com.swizima.paymentsapi.client.BillingPlanClient;
import com.swizima.paymentsapi.dto.BillingActivityResponse;
import com.swizima.paymentsapi.dto.BillingPlan;
import com.swizima.paymentsapi.dto.BillingWalletResponse;
import com.swizima.paymentsapi.dto.ChargeQuoteResponse;
import com.swizima.paymentsapi.dto.CreditLoadRequest;
import com.swizima.paymentsapi.dto.CreditLoadResponse;
import com.swizima.paymentsapi.dto.CreditLoadReversalRequest;
import com.swizima.paymentsapi.dto.CreditLoadSummaryResponse;
import com.swizima.paymentsapi.dto.PageResponse;
import com.swizima.paymentsapi.dto.TransactionChargeRequest;
import com.swizima.paymentsapi.entity.BillingActivity;
import com.swizima.paymentsapi.entity.BillingWallet;
import com.swizima.paymentsapi.entity.CreditLoad;
import com.swizima.paymentsapi.entity.enums.BillingActivityType;
import com.swizima.paymentsapi.entity.enums.CreditLoadStatus;
import com.swizima.paymentsapi.entity.enums.CreditPaymentMethod;
import com.swizima.paymentsapi.entity.enums.RecordStatus;
import com.swizima.paymentsapi.exception.BusinessRuleException;
import com.swizima.paymentsapi.exception.DuplicateResourceException;
import com.swizima.paymentsapi.exception.InsufficientCreditException;
import com.swizima.paymentsapi.exception.ResourceNotFoundException;
import com.swizima.paymentsapi.repository.BillingActivityRepository;
import com.swizima.paymentsapi.repository.BillingWalletRepository;
import com.swizima.paymentsapi.repository.CreditLoadRepository;
import com.swizima.paymentsapi.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingServiceImpl implements BillingService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int MONEY_SCALE = 2;

    private final BillingWalletRepository walletRepository;
    private final BillingActivityRepository activityRepository;
    private final CreditLoadRepository creditLoadRepository;
    private final BillingPlanClient planClient;

    // =====================================================================
    // Wallets
    // =====================================================================

    @Override
    public BillingWalletResponse getWalletByClient(UUID clientRef) {
        BillingPlan plan = planClient.getPlan(clientRef);
        BillingWallet wallet = walletRepository.findByClientRefAndRecordStatusNot(clientRef, RecordStatus.DELETED)
                .orElseGet(() -> emptyWallet(clientRef, plan.currency()));
        return BillingWalletResponse.from(wallet, plan);
    }

    @Override
    public PageResponse<BillingWalletResponse> getWallets(BigDecimal maxBalance, Pageable pageable) {
        return PageResponse.from(walletRepository.search(maxBalance, RecordStatus.DELETED, pageable)
                .map(BillingWalletResponse::from));
    }

    // =====================================================================
    // Credit loads
    // =====================================================================

    @Override
    @Transactional
    public CreditLoadResponse loadCredit(UUID clientRef, CreditLoadRequest request) {
        BillingPlan plan = planClient.getPlan(clientRef); // only clients with a billing plan can hold credit
        BillingWallet wallet = lockOrCreateWallet(clientRef, plan.currency());
        BigDecimal amount = money(request.amount());

        String reference = blankToNull(request.paymentReference());
        if (reference != null) {
            reference = reference.toUpperCase(Locale.ROOT);
            if (creditLoadRepository.existsByPaymentMethodAndPaymentReferenceIgnoreCase(
                    request.paymentMethod(), reference)) {
                throw new DuplicateResourceException("A credit load with " + request.paymentMethod()
                        + " reference '" + reference + "' has already been recorded");
            }
        }

        BigDecimal before = wallet.getBalance();
        wallet.setTotalCredited(wallet.getTotalCredited().add(amount));
        BillingActivity activity = applyMovement(wallet, BillingActivityType.CREDIT_LOAD, amount);

        CreditLoad load = new CreditLoad();
        load.setWallet(wallet);
        load.setClientRef(clientRef);
        load.setAmount(amount);
        load.setCurrency(wallet.getCurrency());
        load.setPaymentMethod(request.paymentMethod());
        load.setPaymentReference(reference);
        load.setPayerName(blankToNull(request.payerName()));
        load.setPayerPhone(blankToNull(request.payerPhone()));
        load.setReceivedBy(blankToNull(request.receivedBy()));
        load.setLoadedAt(request.loadedAt() != null ? request.loadedAt() : LocalDateTime.now());
        load.setBalanceBefore(before);
        load.setBalanceAfter(wallet.getBalance());
        load.setNotes(blankToNull(request.notes()));
        load.setStatus(CreditLoadStatus.COMPLETED);
        load.setRecordStatus(RecordStatus.ACTIVE);
        load = creditLoadRepository.save(load);

        activity.setCreditLoad(load);
        activity.setPaymentReference(reference);
        activity.setDescription("Credit load via " + request.paymentMethod()
                + (load.getNotes() != null ? " - " + load.getNotes() : ""));
        activityRepository.save(activity);

        return CreditLoadResponse.from(load);
    }

    @Override
    @Transactional
    public CreditLoadResponse reverseCreditLoad(UUID creditLoadId, CreditLoadReversalRequest request) {
        CreditLoad load = creditLoadRepository.lockById(creditLoadId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit load with id '" + creditLoadId + "' not found"));
        if (load.getStatus() == CreditLoadStatus.REVERSED) {
            throw new BusinessRuleException("This credit load has already been reversed");
        }
        BillingWallet wallet = lockWallet(load.getWallet().getId());
        if (wallet.getBalance().compareTo(load.getAmount()) < 0) {
            throw new BusinessRuleException("Cannot reverse: " + load.getAmount() + " " + load.getCurrency()
                    + " exceeds the current balance of " + wallet.getBalance()
                    + " (part of this credit has already been used)");
        }

        wallet.setTotalCredited(wallet.getTotalCredited().subtract(load.getAmount()));
        BillingActivity activity = applyMovement(wallet, BillingActivityType.CREDIT_LOAD_REVERSAL,
                load.getAmount().negate());
        activity.setCreditLoad(load);
        activity.setPaymentReference(load.getPaymentReference());
        activity.setDescription("Credit load reversed: " + request.reason());
        activityRepository.save(activity);

        load.setStatus(CreditLoadStatus.REVERSED);
        load.setReversedAt(LocalDateTime.now());
        load.setReversedBy(blankToNull(request.reversedBy()));
        load.setReversalReason(request.reason());
        return CreditLoadResponse.from(creditLoadRepository.save(load));
    }

    @Override
    public CreditLoadResponse getCreditLoad(UUID id) {
        return creditLoadRepository.findById(id).map(CreditLoadResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Credit load with id '" + id + "' not found"));
    }

    @Override
    public PageResponse<CreditLoadResponse> getCreditLoads(UUID clientRef, CreditLoadStatus status,
                                                           CreditPaymentMethod method, LocalDateTime from,
                                                           LocalDateTime to, String search, Pageable pageable) {
        return PageResponse.from(creditLoadRepository
                .search(clientRef, status, method, from, to, blankToNull(search), pageable)
                .map(CreditLoadResponse::from));
    }

    @Override
    public CreditLoadSummaryResponse getCreditLoadSummary(UUID clientRef, LocalDateTime from, LocalDateTime to) {
        String currency = walletRepository.findByClientRefAndRecordStatusNot(clientRef, RecordStatus.DELETED)
                .map(BillingWallet::getCurrency).orElse(null);
        BigDecimal total = creditLoadRepository.sumAmount(clientRef, CreditLoadStatus.COMPLETED, from, to);
        return new CreditLoadSummaryResponse(clientRef, from, to, money(total), currency);
    }

    // =====================================================================
    // Charges
    // =====================================================================

    @Override
    @Transactional
    public BillingActivityResponse chargeTransaction(TransactionChargeRequest request) {
        String txnRef = request.transactionRef().trim();

        // Idempotency: a transaction is only ever charged once
        var existing = activityRepository.findByTransactionRefAndActivityType(txnRef,
                BillingActivityType.TRANSACTION_CHARGE);
        if (existing.isPresent()) {
            BillingActivity prior = existing.get();
            if (!prior.getClientRef().equals(request.clientRef())) {
                throw new BusinessRuleException("Transaction '" + txnRef + "' was already charged to another client");
            }
            return BillingActivityResponse.from(prior);
        }

        // Rate comes from the Configurations API (cached there)
        BillingPlan plan = planClient.getPlan(request.clientRef());
        if (!plan.isActive()) {
            throw new BusinessRuleException("Billing for this client is " + plan.status()
                    + ". Please contact support.");
        }

        BillingWallet wallet = lockOrCreateWallet(request.clientRef(), plan.currency());
        BigDecimal transactionAmount = money(request.transactionAmount());
        BigDecimal charge = computeCharge(transactionAmount, plan.percentageRate());

        if (wallet.getBalance().compareTo(charge) < 0) {
            throw new InsufficientCreditException(charge, wallet.getBalance(), wallet.getCurrency());
        }

        wallet.setTotalCharged(wallet.getTotalCharged().add(charge));
        BillingActivity activity = applyMovement(wallet, BillingActivityType.TRANSACTION_CHARGE, charge.negate());
        activity.setTransactionRef(txnRef);
        activity.setTransactionAmount(transactionAmount);
        activity.setPercentageApplied(plan.percentageRate());
        activity.setDescription(request.description() != null ? request.description()
                : plan.percentageRate().stripTrailingZeros().toPlainString() + "% of " + transactionAmount);

        return BillingActivityResponse.from(activityRepository.save(activity));
    }

    @Override
    @Transactional
    public BillingActivityResponse reverseCharge(String transactionRef, String reason) {
        String txnRef = transactionRef.trim();
        if (activityRepository.findByTransactionRefAndActivityType(txnRef, BillingActivityType.CHARGE_REVERSAL)
                .isPresent()) {
            throw new BusinessRuleException("Charge for transaction '" + txnRef + "' has already been reversed");
        }
        BillingActivity charge = activityRepository
                .findByTransactionRefAndActivityType(txnRef, BillingActivityType.TRANSACTION_CHARGE)
                .orElseThrow(() -> new ResourceNotFoundException("No charge found for transaction '" + txnRef + "'"));

        BillingWallet wallet = lockWallet(charge.getWallet().getId());
        BigDecimal refund = charge.getAmount().negate(); // charge amounts are stored negative

        wallet.setTotalCharged(wallet.getTotalCharged().subtract(refund));
        BillingActivity activity = applyMovement(wallet, BillingActivityType.CHARGE_REVERSAL, refund);
        activity.setTransactionRef(txnRef);
        activity.setTransactionAmount(charge.getTransactionAmount());
        activity.setPercentageApplied(charge.getPercentageApplied());
        activity.setDescription(reason != null && !reason.isBlank() ? reason : "Reversal of transaction charge");

        return BillingActivityResponse.from(activityRepository.save(activity));
    }

    @Override
    public ChargeQuoteResponse quote(UUID clientRef, BigDecimal transactionAmount) {
        BillingPlan plan = planClient.getPlan(clientRef);
        BigDecimal balance = walletRepository.findByClientRefAndRecordStatusNot(clientRef, RecordStatus.DELETED)
                .map(BillingWallet::getBalance).orElse(BigDecimal.ZERO.setScale(MONEY_SCALE));
        BigDecimal amount = money(transactionAmount);
        BigDecimal charge = computeCharge(amount, plan.percentageRate());
        return new ChargeQuoteResponse(clientRef, amount, plan.percentageRate(), charge,
                plan.currency(), balance, balance.compareTo(charge) >= 0);
    }

    // =====================================================================
    // Ledger
    // =====================================================================

    @Override
    public PageResponse<BillingActivityResponse> getActivities(UUID clientRef, BillingActivityType type,
                                                               String transactionRef, LocalDateTime from,
                                                               LocalDateTime to, Pageable pageable) {
        return PageResponse.from(activityRepository
                .search(clientRef, type, blankToNull(transactionRef), from, to, pageable)
                .map(BillingActivityResponse::from));
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /** charge = transactionAmount x rate / 100, rounded half-up to 2 dp. */
    static BigDecimal computeCharge(BigDecimal transactionAmount, BigDecimal percentageRate) {
        return transactionAmount.multiply(percentageRate)
                .divide(HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** Applies a signed amount to the balance and builds the matching ledger row (not yet saved). */
    private BillingActivity applyMovement(BillingWallet wallet, BillingActivityType type, BigDecimal amount) {
        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(amount);
        wallet.setBalance(after);
        walletRepository.save(wallet);

        BillingActivity activity = new BillingActivity();
        activity.setWallet(wallet);
        activity.setClientRef(wallet.getClientRef());
        activity.setActivityType(type);
        activity.setAmount(amount);
        activity.setBalanceBefore(before);
        activity.setBalanceAfter(after);
        activity.setCurrency(wallet.getCurrency());
        activity.setRecordStatus(RecordStatus.ACTIVE);
        return activity;
    }

    /** Creates the wallet on first use (race-safe), then row-locks it. */
    private BillingWallet lockOrCreateWallet(UUID clientRef, String currency) {
        walletRepository.insertIfAbsent(clientRef, currency == null ? "UGX" : currency);
        return walletRepository.lockByClientRef(clientRef, RecordStatus.DELETED)
                .orElseThrow(() -> new IllegalStateException("Wallet for client " + clientRef + " not found"));
    }

    private BillingWallet lockWallet(UUID id) {
        return walletRepository.lockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Billing wallet with id '" + id + "' not found"));
    }

    private static BillingWallet emptyWallet(UUID clientRef, String currency) {
        BillingWallet w = new BillingWallet();
        w.setClientRef(clientRef);
        w.setCurrency(currency == null ? "UGX" : currency);
        w.setBalance(BigDecimal.ZERO.setScale(MONEY_SCALE));
        w.setTotalCredited(BigDecimal.ZERO.setScale(MONEY_SCALE));
        w.setTotalCharged(BigDecimal.ZERO.setScale(MONEY_SCALE));
        return w;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
