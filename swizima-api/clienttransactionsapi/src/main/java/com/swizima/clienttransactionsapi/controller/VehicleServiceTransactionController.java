package com.swizima.clienttransactionsapi.controller;

import com.swizima.clienttransactionsapi.dto.PageResponse;
import com.swizima.clienttransactionsapi.dto.TransactionPaymentRequest;
import com.swizima.clienttransactionsapi.dto.TransactionPaymentResponse;
import com.swizima.clienttransactionsapi.dto.VehicleActionRequest;
import com.swizima.clienttransactionsapi.dto.VehicleServiceTransactionRequest;
import com.swizima.clienttransactionsapi.dto.VehicleServiceTransactionResponse;
import com.swizima.clienttransactionsapi.entity.enums.PaymentStatus;
import com.swizima.clienttransactionsapi.entity.enums.VehicleClearanceStatus;
import com.swizima.clienttransactionsapi.service.VehicleServiceTransactionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/client-transactions/vehicle-services")
@RequiredArgsConstructor
@Tag(name = "Vehicle Service Transactions", description = "Vehicles checked in for a client's services")
public class VehicleServiceTransactionController {

    private final VehicleServiceTransactionService service;

    @PostMapping
    public ResponseEntity<VehicleServiceTransactionResponse> create(
            @Valid @RequestBody VehicleServiceTransactionRequest request) {
        VehicleServiceTransactionResponse created = service.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<VehicleServiceTransactionResponse>> list(
            @RequestParam(required = false) UUID clientRef,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) VehicleClearanceStatus clearanceStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "entryDateTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.list(clientRef, paymentStatus, clearanceStatus, fromDate, toDate, search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleServiceTransactionResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleServiceTransactionResponse> update(
            @PathVariable UUID id, @Valid @RequestBody VehicleServiceTransactionRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- Workflow ----------

    /** Fully-paid vehicle -> CLEARED. 409 if a balance is outstanding. */
    @PostMapping("/{id}/clear")
    public ResponseEntity<VehicleServiceTransactionResponse> clear(
            @PathVariable UUID id, @Valid @RequestBody(required = false) VehicleActionRequest request) {
        return ResponseEntity.ok(service.clear(id, request));
    }

    /** CLEARED vehicle -> RELEASED, recording the actual exit time. */
    @PostMapping("/{id}/exit")
    public ResponseEntity<VehicleServiceTransactionResponse> exit(
            @PathVariable UUID id, @Valid @RequestBody(required = false) VehicleActionRequest request) {
        return ResponseEntity.ok(service.exit(id, request));
    }

    // ---------- Payments ----------

    @GetMapping("/{id}/payments")
    public ResponseEntity<List<TransactionPaymentResponse>> payments(@PathVariable UUID id) {
        return ResponseEntity.ok(service.payments(id));
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<TransactionPaymentResponse> addPayment(
            @PathVariable UUID id, @Valid @RequestBody TransactionPaymentRequest request) {
        TransactionPaymentResponse created = service.addPayment(id, request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{paymentId}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @DeleteMapping("/{id}/payments/{paymentId}")
    public ResponseEntity<Void> deletePayment(@PathVariable UUID id, @PathVariable UUID paymentId) {
        service.deletePayment(id, paymentId);
        return ResponseEntity.noContent().build();
    }
}
