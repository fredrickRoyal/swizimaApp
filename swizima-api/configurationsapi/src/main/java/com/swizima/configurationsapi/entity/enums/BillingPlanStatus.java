package com.swizima.configurationsapi.entity.enums;

/** Whether a client's transactions may currently be charged. */
public enum BillingPlanStatus {
    /** Transactions are charged normally. */
    ACTIVE,
    /** Charges are rejected, which blocks new transactions for the client. */
    SUSPENDED
}
