package com.amcbank.simple.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class FinancialHealthResponse {

    private int creditScore;
    private String rating; // EXCELLENT, GOOD, FAIR, NEEDS_IMPROVEMENT
    private BigDecimal totalLiquidBalance;
    private BigDecimal totalOutstandingLoan;
    private int activeAccountsCount;
    private int activeLoansCount;
    private BigDecimal preApprovedLoanLimit;
    private List<String> positiveFactors;
    private List<String> recommendations;
    private LocalDateTime assessedAt;

    public FinancialHealthResponse() {
    }

    public int getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(int creditScore) {
        this.creditScore = creditScore;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public BigDecimal getTotalLiquidBalance() {
        return totalLiquidBalance;
    }

    public void setTotalLiquidBalance(BigDecimal totalLiquidBalance) {
        this.totalLiquidBalance = totalLiquidBalance;
    }

    public BigDecimal getTotalOutstandingLoan() {
        return totalOutstandingLoan;
    }

    public void setTotalOutstandingLoan(BigDecimal totalOutstandingLoan) {
        this.totalOutstandingLoan = totalOutstandingLoan;
    }

    public int getActiveAccountsCount() {
        return activeAccountsCount;
    }

    public void setActiveAccountsCount(int activeAccountsCount) {
        this.activeAccountsCount = activeAccountsCount;
    }

    public int getActiveLoansCount() {
        return activeLoansCount;
    }

    public void setActiveLoansCount(int activeLoansCount) {
        this.activeLoansCount = activeLoansCount;
    }

    public BigDecimal getPreApprovedLoanLimit() {
        return preApprovedLoanLimit;
    }

    public void setPreApprovedLoanLimit(BigDecimal preApprovedLoanLimit) {
        this.preApprovedLoanLimit = preApprovedLoanLimit;
    }

    public List<String> getPositiveFactors() {
        return positiveFactors;
    }

    public void setPositiveFactors(List<String> positiveFactors) {
        this.positiveFactors = positiveFactors;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public LocalDateTime getAssessedAt() {
        return assessedAt;
    }

    public void setAssessedAt(LocalDateTime assessedAt) {
        this.assessedAt = assessedAt;
    }
}
