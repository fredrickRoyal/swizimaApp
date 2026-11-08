package com.swizima.paymentsapi.entity.enums;

/** Lifecycle of a client-submitted credit top-up request. */
public enum CreditRequestStatus {
    /** Waiting for Super Admin verification and approval. */
    PENDING,
    /** Approved by Super Admin; actual credit has been loaded into the client's wallet. */
    APPROVED,
    /** Rejected by Super Admin (e.g. unverified payment reference). */
    REJECTED
}

