package com.swizima.paymentsapi.entity.enums;

/** Lifecycle of a prepaid credit top-up. */
public enum CreditLoadStatus {
    /** Credit has been added to the client's balance. */
    COMPLETED,
    /** Top-up was undone (e.g. entered by mistake or payment bounced); credit removed again. */
    REVERSED
}
