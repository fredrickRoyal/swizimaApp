package com.swizima.paymentsapi.service;

import com.swizima.paymentsapi.dto.BillingActivityResponse;
import com.swizima.paymentsapi.dto.BillingWalletResponse;
import com.swizima.paymentsapi.dto.ChargeQuoteResponse;
import com.swizima.paymentsapi.dto.CreditLoadRequest;
import com.swizima.paymentsapi.dto.CreditLoadResponse;
import com.swizima.paymentsapi.dto.CreditLoadReversalRequest;
import com.swizima.paymentsapi.dto.CreditLoadSummaryResponse;
import com.swizima.paymentsapi.dto.CreditTopUpApprovalRequest;
import com.swizima.paymentsapi.dto.CreditTopUpRejectionRequest;
import com.swizima.paymentsapi.dto.CreditTopUpRequestCreate;
import com.swizima.paymentsapi.dto.CreditTopUpRequestResponse;
import com.swizima.paymentsapi.dto.PageResponse;
import com.swizima.paymentsapi.dto.TransactionChargeRequest;
import com.swizima.paymentsapi.entity.enums.BillingActivityType;
import com.swizima.paymentsapi.entity.enums.CreditLoadStatus;
import com.swizima.paymentsapi.entity.enums.CreditPaymentMethod;
import com.swizima.paymentsapi.entity.enums.CreditRequestStatus;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface BillingService {

    // ---- Wallets ----
    BillingWalletResponse getWalletByClient(UUID clientRef);

    PageResponse<BillingWalletResponse> getWallets(BigDecimal maxBalance, Pageable pageable);

    // ---- Credit loads ----
    CreditLoadResponse loadCredit(UUID clientRef, CreditLoadRequest request);

    CreditLoadResponse reverseCreditLoad(UUID creditLoadId, CreditLoadReversalRequest request);

    CreditLoadResponse getCreditLoad(UUID id);

    PageResponse<CreditLoadResponse> getCreditLoads(UUID clientRef, CreditLoadStatus status,
                                                    CreditPaymentMethod method, LocalDateTime from,
                                                    LocalDateTime to, String search, Pageable pageable);

    CreditLoadSummaryResponse getCreditLoadSummary(UUID clientRef, LocalDateTime from, LocalDateTime to);

    // ---- Credit top-up requests ----
    CreditTopUpRequestResponse submitCreditRequest(UUID clientRef, CreditTopUpRequestCreate request);

    PageResponse<CreditTopUpRequestResponse> getCreditRequests(UUID clientRef, CreditRequestStatus status,
                                                               CreditPaymentMethod method, String search,
                                                               Pageable pageable);

    CreditTopUpRequestResponse approveCreditRequest(UUID requestId, CreditTopUpApprovalRequest request);

    CreditTopUpRequestResponse rejectCreditRequest(UUID requestId, CreditTopUpRejectionRequest request);

    // ---- Charges ----
    /** Deducts plan percentage x transactionAmount from the client's wallet (idempotent per transactionRef). */
    BillingActivityResponse chargeTransaction(TransactionChargeRequest request);

    BillingActivityResponse reverseCharge(String transactionRef, String reason);

    ChargeQuoteResponse quote(UUID clientRef, BigDecimal transactionAmount);

    // ---- Activity ledger ----
    PageResponse<BillingActivityResponse> getActivities(UUID clientRef, BillingActivityType type,
                                                        String transactionRef, LocalDateTime from,
                                                        LocalDateTime to, Pageable pageable);
}
