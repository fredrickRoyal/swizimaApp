package com.swizima.paymentsapi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Total completed top-ups for a client over an optional period. */
public record CreditLoadSummaryResponse(
        UUID clientRef,
        LocalDateTime from,
        LocalDateTime to,
        BigDecimal totalLoaded,
        String currency
) {
}
