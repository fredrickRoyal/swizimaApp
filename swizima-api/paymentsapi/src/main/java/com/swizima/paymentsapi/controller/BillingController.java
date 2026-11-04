package com.swizima.paymentsapi.controller;

import com.swizima.paymentsapi.dto.BillingActivityResponse;
import com.swizima.paymentsapi.dto.BillingWalletResponse;
import com.swizima.paymentsapi.dto.ChargeQuoteResponse;
import com.swizima.paymentsapi.dto.ChargeReversalRequest;
import com.swizima.paymentsapi.dto.CreditLoadRequest;
import com.swizima.paymentsapi.dto.CreditLoadResponse;
import com.swizima.paymentsapi.dto.CreditLoadReversalRequest;
import com.swizima.paymentsapi.dto.CreditLoadSummaryResponse;
import com.swizima.paymentsapi.dto.PageResponse;
import com.swizima.paymentsapi.dto.TransactionChargeRequest;
import com.swizima.paymentsapi.entity.enums.BillingActivityType;
import com.swizima.paymentsapi.entity.enums.CreditLoadStatus;
import com.swizima.paymentsapi.entity.enums.CreditPaymentMethod;
import com.swizima.paymentsapi.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments/billing")
@RequiredArgsConstructor
@Validated
@Tag(name = "Billing", description = "Client prepaid wallets, credit loads, per-transaction charges and activity ledger")
public class BillingController {

    private final BillingService billingService;

    // ---------------- Wallets ----------------

    @GetMapping("/wallets/client/{clientRef}")
    @Operation(summary = "Client's credit balance, with rate and low-balance flag from their billing plan")
    public ResponseEntity<BillingWalletResponse> getWalletByClient(@PathVariable UUID clientRef) {
        return ResponseEntity.ok(billingService.getWalletByClient(clientRef));
    }

    @GetMapping("/wallets")
    @Operation(summary = "List wallets; use maxBalance to find clients running low on credit")
    public ResponseEntity<PageResponse<BillingWalletResponse>> getWallets(
            @RequestParam(required = false) BigDecimal maxBalance,
            @PageableDefault(size = 20, sort = "balance", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(billingService.getWallets(maxBalance, pageable));
    }

    // ---------------- Credit loads ----------------

    @PostMapping("/wallets/client/{clientRef}/credits")
    @Operation(summary = "Load prepaid credit (e.g. 50,000 UGX); creates the wallet on first load")
    public ResponseEntity<CreditLoadResponse> loadCredit(@PathVariable UUID clientRef,
                                                         @Valid @RequestBody CreditLoadRequest request) {
        CreditLoadResponse created = billingService.loadCredit(clientRef, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/payments/billing/credit-loads/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/credit-loads")
    @Operation(summary = "Credit load history (search matches payment reference, payer name or phone)")
    public ResponseEntity<PageResponse<CreditLoadResponse>> getCreditLoads(
            @RequestParam(required = false) UUID clientRef,
            @RequestParam(required = false) CreditLoadStatus status,
            @RequestParam(required = false) CreditPaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "loadedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(billingService.getCreditLoads(clientRef, status, paymentMethod, from, to,
                search, pageable));
    }

    @GetMapping("/credit-loads/summary")
    @Operation(summary = "Total completed credit loaded by a client over an optional period")
    public ResponseEntity<CreditLoadSummaryResponse> getCreditLoadSummary(
            @RequestParam UUID clientRef,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(billingService.getCreditLoadSummary(clientRef, from, to));
    }

    @GetMapping("/credit-loads/{id}")
    public ResponseEntity<CreditLoadResponse> getCreditLoad(@PathVariable UUID id) {
        return ResponseEntity.ok(billingService.getCreditLoad(id));
    }

    @PostMapping("/credit-loads/{id}/reversal")
    @Operation(summary = "Reverse a mistaken/bounced credit load (only if the credit is still unused)")
    public ResponseEntity<CreditLoadResponse> reverseCreditLoad(@PathVariable UUID id,
                                                                @Valid @RequestBody CreditLoadReversalRequest request) {
        return ResponseEntity.ok(billingService.reverseCreditLoad(id, request));
    }

    // ---------------- Charges ----------------

    @PostMapping("/charges")
    @Operation(summary = "Deduct the client's plan percentage for a transaction (idempotent per transactionRef)")
    public ResponseEntity<BillingActivityResponse> chargeTransaction(
            @Valid @RequestBody TransactionChargeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billingService.chargeTransaction(request));
    }

    @PostMapping("/charges/{transactionRef}/reversal")
    @Operation(summary = "Refund the charge for a cancelled/voided transaction")
    public ResponseEntity<BillingActivityResponse> reverseCharge(
            @PathVariable String transactionRef,
            @Valid @RequestBody(required = false) ChargeReversalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(billingService.reverseCharge(transactionRef, request == null ? null : request.reason()));
    }

    @GetMapping("/charges/quote")
    @Operation(summary = "Preview the charge for an amount without deducting")
    public ResponseEntity<ChargeQuoteResponse> quote(@RequestParam UUID clientRef,
                                                     @RequestParam @Positive BigDecimal transactionAmount) {
        return ResponseEntity.ok(billingService.quote(clientRef, transactionAmount));
    }

    // ---------------- Activity ledger ----------------

    @GetMapping("/activities")
    @Operation(summary = "Billing activity ledger: credit loads, charges and reversals")
    public ResponseEntity<PageResponse<BillingActivityResponse>> getActivities(
            @RequestParam(required = false) UUID clientRef,
            @RequestParam(required = false) BillingActivityType type,
            @RequestParam(required = false) String transactionRef,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(billingService.getActivities(clientRef, type, transactionRef, from, to, pageable));
    }
}
