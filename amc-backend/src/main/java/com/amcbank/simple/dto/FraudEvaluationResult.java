package com.amcbank.simple.dto;

import java.util.List;

public class FraudEvaluationResult {

    private int riskScore; // 0 - 100
    private String riskLevel; // LOW, MEDIUM, HIGH
    private boolean flagged;
    private List<String> riskFactors;

    public FraudEvaluationResult() {
    }

    public FraudEvaluationResult(int riskScore, String riskLevel, boolean flagged, List<String> riskFactors) {
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.flagged = flagged;
        this.riskFactors = riskFactors;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public List<String> getRiskFactors() {
        return riskFactors;
    }

    public void setRiskFactors(List<String> riskFactors) {
        this.riskFactors = riskFactors;
    }
}
