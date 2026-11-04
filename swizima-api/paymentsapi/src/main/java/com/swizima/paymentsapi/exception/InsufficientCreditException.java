package com.swizima.paymentsapi.exception;

import lombok.Getter;

import java.math.BigDecimal;

/** The client's prepaid billing credit cannot cover a transaction charge (mapped to 402 Payment Required). */
@Getter
public class InsufficientCreditException extends RuntimeException {

    private final BigDecimal requiredAmount;
    private final BigDecimal balance;
    private final String currency;

    public InsufficientCreditException(BigDecimal requiredAmount, BigDecimal balance, String currency) {
        super("Insufficient billing credit: this transaction requires " + requiredAmount.toPlainString() + " "
                + currency + " but the balance is " + balance.toPlainString() + " " + currency
                + ". Please top up your credit to continue.");
        this.requiredAmount = requiredAmount;
        this.balance = balance;
        this.currency = currency;
    }
}
