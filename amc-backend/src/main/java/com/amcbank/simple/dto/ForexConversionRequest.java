package com.amcbank.simple.dto;

import java.math.BigDecimal;

public class ForexConversionRequest {

    private String fromCurrency; // e.g. "USD", "INR", "EUR", "GBP"
    private String toCurrency;
    private BigDecimal amount;

    public ForexConversionRequest() {
    }

    public ForexConversionRequest(String fromCurrency, String toCurrency, BigDecimal amount) {
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.amount = amount;
    }

    public String getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(String fromCurrency) {
        this.fromCurrency = fromCurrency;
    }

    public String getToCurrency() {
        return toCurrency;
    }

    public void setToCurrency(String toCurrency) {
        this.toCurrency = toCurrency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
