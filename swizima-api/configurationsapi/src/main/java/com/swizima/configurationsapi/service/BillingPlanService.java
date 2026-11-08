package com.swizima.configurationsapi.service;

import com.swizima.configurationsapi.dto.BillingPlanRequest;
import com.swizima.configurationsapi.dto.BillingPlanResponse;
import com.swizima.configurationsapi.dto.PageResponse;
import com.swizima.configurationsapi.entity.enums.BillingPlanStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BillingPlanService {

    BillingPlanResponse createPlan(BillingPlanRequest request);

    BillingPlanResponse updatePlan(UUID id, BillingPlanRequest request);

    BillingPlanResponse updatePlanStatus(UUID id, BillingPlanStatus status);

    BillingPlanResponse getPlan(UUID id);

    /** Cached: this is the lookup the Payments API makes on every charge. */
    BillingPlanResponse getPlanByClient(UUID clientRef);

    PageResponse<BillingPlanResponse> getPlans(BillingPlanStatus status, Pageable pageable);

    void deletePlan(UUID id);
}
