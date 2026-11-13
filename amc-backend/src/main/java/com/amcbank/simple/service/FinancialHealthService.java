package com.amcbank.simple.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.amcbank.simple.dto.FinancialHealthResponse;
import com.amcbank.simple.model.Account;
import com.amcbank.simple.model.Customer;
import com.amcbank.simple.model.Loan;
import com.amcbank.simple.repository.AccountRepository;
import com.amcbank.simple.repository.LoanRepository;

@Service
public class FinancialHealthService {

    private final AccountRepository accountRepository;
    private final LoanRepository loanRepository;

    public FinancialHealthService(AccountRepository accountRepository, LoanRepository loanRepository) {
        this.accountRepository = accountRepository;
        this.loanRepository = loanRepository;
    }

    public FinancialHealthResponse assessCustomer(Customer customer) {
        List<Account> accounts = accountRepository.findByCustomerId(customer.getId());
        List<Loan> loans = loanRepository.findByCustomerId(customer.getId());

        BigDecimal totalLiquidBalance = accounts.stream()
                .filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus()))
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutstandingLoan = loans.stream()
                .filter(l -> "ACTIVE".equalsIgnoreCase(l.getStatus()))
                .map(Loan::getPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int activeAccounts = (int) accounts.stream()
                .filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus()))
                .count();

        int activeLoans = (int) loans.stream()
                .filter(l -> "ACTIVE".equalsIgnoreCase(l.getStatus()))
                .count();

        // Base CIBIL/FICO credit score starting point
        int score = 650;
        List<String> positiveFactors = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        // 1. Liquid balance evaluation
        if (totalLiquidBalance.compareTo(new BigDecimal("200000")) >= 0) {
            score += 70;
            positiveFactors.add("Substantial liquid reserves (₹" + totalLiquidBalance + ") demonstrating strong solvency");
        } else if (totalLiquidBalance.compareTo(new BigDecimal("50000")) >= 0) {
            score += 40;
            positiveFactors.add("Healthy emergency fund balance (₹" + totalLiquidBalance + ")");
        } else if (totalLiquidBalance.compareTo(new BigDecimal("10000")) < 0) {
            score -= 30;
            recommendations.add("Increase average monthly balance to maintain an emergency buffer above ₹25,000");
        }

        // 2. Debt-to-Asset ratio evaluation
        if (totalLiquidBalance.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal debtRatio = totalOutstandingLoan.divide(totalLiquidBalance, 2, RoundingMode.HALF_UP);
            if (debtRatio.compareTo(new BigDecimal("0.5")) <= 0) {
                score += 50;
                positiveFactors.add("Exceptional debt-to-asset ratio (" + debtRatio + "x), well below prudent bank thresholds");
            } else if (debtRatio.compareTo(new BigDecimal("2.0")) > 0) {
                score -= 45;
                recommendations.add("Current loan exposure exceeds liquid assets by " + debtRatio + "x; prioritize debt repayment");
            }
        } else if (totalOutstandingLoan.compareTo(BigDecimal.ZERO) > 0) {
            score -= 60;
            recommendations.add("Zero liquid balance with active loans impairs debt serviceability");
        }

        // 3. Multi-account relationship
        if (activeAccounts >= 2) {
            score += 20;
            positiveFactors.add("Diversified portfolio with multiple active banking accounts");
        }

        // 4. Loan burden
        if (activeLoans == 0) {
            score += 35;
            positiveFactors.add("Zero outstanding debt; debt-free borrower status");
        } else if (activeLoans >= 3) {
            score -= 35;
            recommendations.add("Managing 3 or more concurrent loans increases monthly EMI pressure");
        }

        // Clamp score within standard 300 to 850 range
        score = Math.min(850, Math.max(300, score));

        String rating;
        if (score >= 750) {
            rating = "EXCELLENT";
        } else if (score >= 680) {
            rating = "GOOD";
        } else if (score >= 580) {
            rating = "FAIR";
        } else {
            rating = "NEEDS_IMPROVEMENT";
        }

        // Pre-approved loan eligibility calculation
        BigDecimal preApprovedLimit;
        if (score >= 750) {
            preApprovedLimit = totalLiquidBalance.multiply(new BigDecimal("5.0")).setScale(2, RoundingMode.HALF_UP);
        } else if (score >= 680) {
            preApprovedLimit = totalLiquidBalance.multiply(new BigDecimal("3.0")).setScale(2, RoundingMode.HALF_UP);
        } else if (score >= 580) {
            preApprovedLimit = totalLiquidBalance.multiply(new BigDecimal("1.5")).setScale(2, RoundingMode.HALF_UP);
        } else {
            preApprovedLimit = BigDecimal.ZERO;
        }

        if (positiveFactors.isEmpty()) {
            positiveFactors.add("Account maintained in standard active standing");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("Continue maintaining strong balance and timely repayments to preserve top-tier credit rating");
        }

        FinancialHealthResponse response = new FinancialHealthResponse();
        response.setCreditScore(score);
        response.setRating(rating);
        response.setTotalLiquidBalance(totalLiquidBalance);
        response.setTotalOutstandingLoan(totalOutstandingLoan);
        response.setActiveAccountsCount(activeAccounts);
        response.setActiveLoansCount(activeLoans);
        response.setPreApprovedLoanLimit(preApprovedLimit);
        response.setPositiveFactors(positiveFactors);
        response.setRecommendations(recommendations);
        response.setAssessedAt(LocalDateTime.now());

        return response;
    }
}
