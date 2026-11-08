package com.swizima.configurationsapi.entity.enums;

public enum BillingModel {
    /** Client is charged a percentage on every transaction from their prepaid wallet. */
    COMMISSION,
    /** Client pays an agreed fixed amount at the end of every month (no per-transaction commission). */
    MONTHLY_FLAT
}

