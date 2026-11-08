package com.swizima.configurationsapi.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring caching (Caffeine, configured via spring.cache.* properties).
 * Cache names are declared in {@link CacheNames}.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final class CacheNames {
        /** Billing plan by clientRef - read by the Payments API on every transaction charge. */
        public static final String BILLING_PLANS_BY_CLIENT = "billingPlansByClient";

        private CacheNames() {
        }
    }
}
