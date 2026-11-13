package com.amcbank.simple.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.amcbank.simple.dto.FraudEvaluationResult;
import com.amcbank.simple.model.Account;
import com.amcbank.simple.repository.TransactionRepository;

@Service
public class FraudDetectionService {

    private final TransactionRepository transactionRepository;

    public FraudDetectionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Evaluates a transaction in real-time before completion, analyzing amount thresholds,
     * velocity of consecutive transactions, balance depletion ratios, and temporal anomalies.
     */
    public FraudEvaluationResult evaluateTransaction(Account account, BigDecimal amount, String type) {
        List<String> riskFactors = new ArrayList<>();
        int score = 0;

        // 1. Transaction Amount Thresholds
        if (amount.compareTo(new BigDecimal("150000")) >= 0) {
            score += 50;
            riskFactors.add("Critical amount threshold exceeded (> ₹1,50,000)");
        } else if (amount.compareTo(new BigDecimal("50000")) >= 0) {
            score += 30;
            riskFactors.add("High-value transaction flagged (> ₹50,000)");
        }

        // 2. Severe Account Drain Check (for outbound funds)
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0 &&
                ("WITHDRAWAL".equalsIgnoreCase(type) || "TRANSFER_OUT".equalsIgnoreCase(type))) {
            BigDecimal drainRatio = amount.divide(account.getBalance(), 4, java.math.RoundingMode.HALF_UP);
            if (drainRatio.compareTo(new BigDecimal("0.85")) >= 0) {
                score += 25;
                riskFactors.add("Substantial liquidity drain: over 85% of account balance withdrawn/transferred in a single operation");
            }
        }

        // 3. Velocity Check (Rapid consecutive transactions within 2 minutes)
        if (account.getId() != null) {
            LocalDateTime twoMinutesAgo = LocalDateTime.now().minusMinutes(2);
            long recentCount = transactionRepository.countRecentTransactions(account.getId(), twoMinutesAgo);
            if (recentCount >= 3) {
                score += 35;
                riskFactors.add("High velocity detected: " + recentCount + " transactions occurred within the last 2 minutes");
            }
        }

        // 4. Off-Hours Banking Anomaly Check (1 AM - 5 AM)
        int currentHour = LocalDateTime.now().getHour();
        if (currentHour >= 1 && currentHour <= 5 && amount.compareTo(new BigDecimal("25000")) >= 0) {
            score += 15;
            riskFactors.add("Unusual off-peak banking hour operation (between 1:00 AM and 5:00 AM)");
        }

        score = Math.min(100, Math.max(0, score));

        String riskLevel;
        boolean flagged;
        if (score >= 65) {
            riskLevel = "HIGH";
            flagged = true;
        } else if (score >= 35) {
            riskLevel = "MEDIUM";
            flagged = false;
        } else {
            riskLevel = "LOW";
            flagged = false;
            if (riskFactors.isEmpty()) {
                riskFactors.add("Standard low-risk customer banking activity");
            }
        }

        return new FraudEvaluationResult(score, riskLevel, flagged, riskFactors);
    }
}
