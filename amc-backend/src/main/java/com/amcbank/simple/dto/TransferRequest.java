package com.amcbank.simple.dto;

import java.math.BigDecimal;

public class TransferRequest {

    private Long fromAccountId;
    private String toAccountNumber;
    private BigDecimal amount;
    private String description;

    public TransferRequest() {
    }

    public TransferRequest(Long fromAccountId, String toAccountNumber, BigDecimal amount, String description) {
        this.fromAccountId = fromAccountId;
        this.toAccountNumber = toAccountNumber;
        this.amount = amount;
        this.description = description;
    }

    public Long getFromAccountId() {
        return fromAccountId;
    }

    public void setFromAccountId(Long fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    public String getToAccountNumber() {
        return toAccountNumber;
    }

    public void setToAccountNumber(String toAccountNumber) {
        this.toAccountNumber = toAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
