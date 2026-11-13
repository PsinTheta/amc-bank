package com.amcbank.simple.dto;

import java.math.BigDecimal;

public class FixedDepositRequest {

    private Long sourceAccountId;
    private BigDecimal depositAmount;
    private int tenureMonths; // e.g. 6, 12, 24, 36, 60

    public FixedDepositRequest() {
    }

    public FixedDepositRequest(Long sourceAccountId, BigDecimal depositAmount, int tenureMonths) {
        this.sourceAccountId = sourceAccountId;
        this.depositAmount = depositAmount;
        this.tenureMonths = tenureMonths;
    }

    public Long getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(Long sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public int getTenureMonths() {
        return tenureMonths;
    }

    public void setTenureMonths(int tenureMonths) {
        this.tenureMonths = tenureMonths;
    }
}
