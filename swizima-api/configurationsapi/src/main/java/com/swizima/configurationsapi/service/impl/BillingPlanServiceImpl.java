package com.swizima.configurationsapi.service.impl;

import com.swizima.configurationsapi.config.CacheConfig.CacheNames;
import com.swizima.configurationsapi.dto.BillingPlanRequest;
import com.swizima.configurationsapi.dto.BillingPlanResponse;
import com.swizima.configurationsapi.dto.PageResponse;
import com.swizima.configurationsapi.entity.ClientBillingPlan;
import com.swizima.configurationsapi.entity.enums.BillingPlanStatus;
import com.swizima.configurationsapi.entity.enums.RecordStatus;
import com.swizima.configurationsapi.exception.DuplicateResourceException;
import com.swizima.configurationsapi.exception.ResourceNotFoundException;
import com.swizima.configurationsapi.repository.ClientBillingPlanRepository;
import com.swizima.configurationsapi.service.BillingPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.RoundingMode;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingPlanServiceImpl implements BillingPlanService {

    private static final String DEFAULT_CURRENCY = "UGX";

    private final ClientBillingPlanRepository planRepository;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public BillingPlanResponse createPlan(BillingPlanRequest request) {
        if (planRepository.existsByClientRef(request.clientRef())) {
            throw new DuplicateResourceException(
                    "Billing plan already exists for client '" + request.clientRef() + "'");
        }
        ClientBillingPlan plan = new ClientBillingPlan();
        plan.setClientRef(request.clientRef());
        plan.setStatus(BillingPlanStatus.ACTIVE);
        plan.setRecordStatus(RecordStatus.ACTIVE);
        applyRequest(plan, request);
        ClientBillingPlan saved = planRepository.save(plan);
        evictAfterCommit(saved.getClientRef());
        return BillingPlanResponse.from(saved);
    }

    @Override
    @Transactional
    public BillingPlanResponse updatePlan(UUID id, BillingPlanRequest request) {
        ClientBillingPlan plan = findPlan(id);
        // A rate change applies to future charges only; past charges keep the rate they were computed with.
        applyRequest(plan, request);
        evictAfterCommit(plan.getClientRef());
        return BillingPlanResponse.from(planRepository.save(plan));
    }

    @Override
    @Transactional
    public BillingPlanResponse updatePlanStatus(UUID id, BillingPlanStatus status) {
        ClientBillingPlan plan = findPlan(id);
        plan.setStatus(status);
        evictAfterCommit(plan.getClientRef());
        return BillingPlanResponse.from(planRepository.save(plan));
    }

    @Override
    public BillingPlanResponse getPlan(UUID id) {
        return BillingPlanResponse.from(findPlan(id));
    }

    /** Not-found results are not cached (the exception propagates), so new plans are picked up immediately. */
    @Override
    @Cacheable(cacheNames = CacheNames.BILLING_PLANS_BY_CLIENT, key = "#clientRef")
    public BillingPlanResponse getPlanByClient(UUID clientRef) {
        return planRepository.findByClientRefAndRecordStatusNot(clientRef, RecordStatus.DELETED)
                .map(BillingPlanResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No billing plan configured for client '" + clientRef + "'"));
    }

    @Override
    public PageResponse<BillingPlanResponse> getPlans(BillingPlanStatus status, Pageable pageable) {
        return PageResponse.from(planRepository.search(status, RecordStatus.DELETED, pageable)
                .map(BillingPlanResponse::from));
    }

    @Override
    @Transactional
    public void deletePlan(UUID id) {
        ClientBillingPlan plan = findPlan(id);
        plan.setRecordStatus(RecordStatus.DELETED);
        planRepository.save(plan);
        evictAfterCommit(plan.getClientRef());
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Evicts the cached plan once the change is committed, so a concurrent read can't
     * re-cache the old value between eviction and commit.
     */
    private void evictAfterCommit(UUID clientRef) {
        Runnable evict = () -> {
            Cache cache = cacheManager.getCache(CacheNames.BILLING_PLANS_BY_CLIENT);
            if (cache != null) cache.evict(clientRef);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict.run();
                }
            });
        } else {
            evict.run();
        }
    }

    private void applyRequest(ClientBillingPlan plan, BillingPlanRequest r) {
        plan.setPercentageRate(r.percentageRate());
        plan.setCurrency(r.currency() == null || r.currency().isBlank()
                ? (plan.getCurrency() != null ? plan.getCurrency() : DEFAULT_CURRENCY)
                : r.currency().trim().toUpperCase(Locale.ROOT));
        plan.setLowBalanceThreshold(r.lowBalanceThreshold() == null ? null
                : r.lowBalanceThreshold().setScale(2, RoundingMode.HALF_UP));
        plan.setNotes(r.notes());
        if (r.status() != null) {
            plan.setStatus(r.status());
        }
    }

    private ClientBillingPlan findPlan(UUID id) {
        return planRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Billing plan with id '" + id + "' not found"));
    }

}
