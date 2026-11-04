package com.swizima.paymentsapi.entity.enums;

/**
 * Kind of movement recorded in the billing ledger.
 */
public enum BillingActivityType {
    /** Prepaid credit loaded by/for the client (increases balance). */
    CREDIT_LOAD,
    /** Undo of a CREDIT_LOAD (decreases balance). */
    CREDIT_LOAD_REVERSAL,
    /** Percentage fee deducted for a client transaction (decreases balance). */
    TRANSACTION_CHARGE,
    /** Refund of a previous TRANSACTION_CHARGE (increases balance). */
    CHARGE_REVERSAL
}
