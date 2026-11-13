# 🌟 AMC Bank - Advanced Enterprise Features & Teacher Demo Guide

This document outlines the extra enterprise-grade features implemented in the AMC Bank backend to showcase advanced engineering, fintech security standards, and real-world banking logic to your teachers and evaluators.

---

## 🚀 Overview of New Features

| Feature | Description | Academic Value |
| :--- | :--- | :--- |
| **1. Double-Entry Transaction Ledger** | Every deposit, withdrawal, and transfer records an immutable audit trail with reference code, balance before/after, and timestamp. | Demonstrates database consistency, ACID transactions, and compliance. |
| **2. Peer-to-Peer (P2P) Fund Transfer** | Allows instant transfer between any two AMC Bank accounts with atomic debits/credits (`@Transactional`). | Essential core banking capability. |
| **3. AI/Rule-Based Fraud Detection Engine** | Real-time risk analyzer checking transaction thresholds (>₹50,000), velocity anomalies (<2 min), and balance drain ratios (>85%). | Demonstrates cybersecurity, heuristic analysis, and fintech risk management. |
| **4. Fixed Deposit (FD) Module** | Term deposit creation with quarterly compounding interest formula: $A = P(1 + r/4)^{4t}$, premature liquidation, and account linking. | Financial math and product diversification. |
| **5. CIBIL-style Credit & Financial Health Score** | Analyzes customer liquidity, debt-to-income ratio, and loan burden to generate a credit score (300-850), rating, and pre-approved loan limit. | Demonstrates credit scoring algorithms and personalized customer insights. |
| **6. Official Printable Statement / Passbook** | Generates detailed statement metrics and formatted ASCII passbook ready for printing. | Real-world customer self-service capability. |
| **7. Multi-Currency Forex Calculator** | Live currency converter supporting USD, EUR, GBP, AED, JPY, CAD, and SGD with bank spread calculation. | International remittance and exchange rate modeling. |
| **8. Real-Time Admin KPI Dashboard** | Enhanced analytics for total bank reserves, loan disbursement, term deposits, and active fraud alerts. | Executive reporting and business intelligence. |

---

## 📋 Comprehensive API Reference

### 1. Peer-to-Peer Fund Transfer
* **Customer Endpoint**: `POST /api/customer/me/transfer` *(Requires `ROLE_CUSTOMER` token)*
* **Admin Endpoint**: `POST /api/accounts/transfer` *(Requires `ROLE_ADMIN` token)*

**Request Payload:**
```json
{
  "fromAccountId": 1,
  "toAccountNumber": "AC87B2C910",
  "amount": 5000.00,
  "description": "Semester tuition fees"
}
```

**Response Payload:**
```json
{
  "referenceNumber": "TXN-1727721600000-B8A1D",
  "status": "SUCCESS",
  "amount": 5000.00,
  "fromAccountNumber": "AC12345678",
  "toAccountNumber": "AC87B2C910",
  "recipientName": "Alice Smith",
  "senderRemainingBalance": 45000.00,
  "riskScore": 10,
  "riskLevel": "LOW",
  "flaggedForReview": false,
  "timestamp": "2026-10-01T10:15:30"
}
```

---

### 2. Transaction History & Audit Ledger
* **Customer Transactions**: `GET /api/customer/me/transactions`
* **Account Specific Transactions**: `GET /api/customer/me/accounts/{accountId}/transactions`
* **Admin All Transactions**: `GET /api/transactions`
* **Admin Account Transactions**: `GET /api/accounts/{accountId}/transactions`

---

### 3. Official Account Statement & Passbook Generator
* **Customer Endpoint**: `GET /api/customer/me/accounts/{accountId}/statement`
* **Admin Endpoint**: `GET /api/accounts/{accountId}/statement`

**Response includes structured data and printable passbook:**
```text
========================================================================================
                                AMC BANK LIMITED                                       
                          OFFICIAL ACCOUNT STATEMENT                                   
========================================================================================
Customer Name  : Rahul Sharma                   Date Generated: 01-Oct-2026 10:30
Account Number : AC94827163                     Account Type  : SAVINGS
Current Balance: INR 45,000.00                  Status        : ACTIVE
----------------------------------------------------------------------------------------
Date / Time        | Reference        | Type           | Amount (INR) | Balance (INR)
----------------------------------------------------------------------------------------
01-Oct-2026 09:15  | TXN-17277215-A1  | DEPOSIT        |    +50000.00 |     50000.00
01-Oct-2026 10:15  | TXN-17277216-B8  | TRANSFER_OUT   |     -5000.00 |     45000.00
----------------------------------------------------------------------------------------
Total Deposits : INR 50000.00           Total Withdrawals: INR 5000.00
Net Cash Flow  : INR 45000.00           Closing Balance   : INR 45000.00
========================================================================================
```

---

### 4. Smart Fraud & Anomaly Detection Center
* **Admin Flagged Alerts**: `GET /api/security/fraud-alerts`
* **On-Demand Risk Evaluation**: `POST /api/security/evaluate-risk`

**Request Payload:**
```json
{
  "accountId": 1,
  "amount": 95000.00,
  "type": "WITHDRAWAL"
}
```

**Response Payload:**
```json
{
  "riskScore": 75,
  "riskLevel": "HIGH",
  "flagged": true,
  "riskFactors": [
    "High-value transaction flagged (> ₹50,000)",
    "Substantial liquidity drain: over 85% of account balance withdrawn/transferred in a single operation"
  ]
}
```

---

### 5. Fixed Deposit (FD) / Term Investment Manager
* **Open FD**: `POST /api/customer/me/fixed-deposits`
  ```json
  {
    "sourceAccountId": 1,
    "depositAmount": 50000.00,
    "tenureMonths": 12
  }
  ```
  *(Automatically debits source account, applies progressive interest curve, and calculates quarterly compounded maturity value).*
* **View Customer FDs**: `GET /api/customer/me/fixed-deposits`
* **Premature / Matured Liquidation**: `POST /api/customer/me/fixed-deposits/{fdId}/close`
  *(Liquidates principal + earned interest back into the linked account).*
* **Admin View All FDs**: `GET /api/fixed-deposits`

---

### 6. CIBIL-Style Credit & Financial Health Score
* **Customer Check**: `GET /api/customer/me/financial-health`
* **Admin Customer Assessment**: `GET /api/customers/{customerId}/financial-health`

**Response Payload:**
```json
{
  "creditScore": 790,
  "rating": "EXCELLENT",
  "totalLiquidBalance": 125000.00,
  "totalOutstandingLoan": 0.00,
  "activeAccountsCount": 2,
  "activeLoansCount": 0,
  "preApprovedLoanLimit": 625000.00,
  "positiveFactors": [
    "Substantial liquid reserves (₹125000.00) demonstrating strong solvency",
    "Zero outstanding debt; debt-free borrower status",
    "Diversified portfolio with multiple active banking accounts"
  ],
  "recommendations": [
    "Continue maintaining strong balance and timely repayments to preserve top-tier credit rating"
  ],
  "assessedAt": "2026-10-01T10:20:00"
}
```

---

### 7. Multi-Currency Forex Simulator (Public Access)
* **Exchange Rates**: `GET /api/forex/rates`
* **Currency Conversion**: `POST /api/forex/convert`
  ```json
  {
    "fromCurrency": "USD",
    "toCurrency": "INR",
    "amount": 500.00
  }
  ```

**Response Payload:**
```json
{
  "fromCurrency": "USD",
  "toCurrency": "INR",
  "originalAmount": 500.00,
  "exchangeRate": 83.50,
  "convertedAmount": 41750.00,
  "feePercent": 0.50,
  "feeAmount": 208.75,
  "netPayoutAmount": 41541.25,
  "timestamp": "2026-10-01T10:25:00"
}
```

---

### 8. Enhanced Real-Time Admin Dashboard
* **Endpoint**: `GET /api/dashboard`

**Response Payload:**
```json
{
  "customers": 12,
  "activeAccounts": 15,
  "activeLoans": 4,
  "activeFixedDeposits": 3,
  "totalBankReserves": 1450000.00,
  "totalLoansSanctioned": 850000.00,
  "totalTermDeposits": 250000.00,
  "totalTransactionsCount": 48,
  "flaggedSecurityAlerts": 2
}
```

---

## 🎓 Teacher Presentation Script (How to Ace Your Viva)

1. **Start with the Core Banking Flow:**
   - Login as customer.
   - Show how deposits/withdrawals now maintain an immutable **Ledger** (`Transaction`), unlike basic CRUD projects.
2. **Demonstrate Peer-to-Peer Transfer:**
   - Transfer ₹5,000 from Customer A to Customer B using account number.
   - Point out that this uses atomic `@Transactional` boundary and updates both accounts simultaneously.
3. **Show Off the AI / Fraud Detection Engine:**
   - Intentionally attempt to withdraw or transfer ₹1,00,000 or >85% of balance.
   - Show how the backend scores the risk and flags it under `GET /api/security/fraud-alerts` for admin inspection.
4. **Present the Financial Health Score:**
   - Call `GET /api/customer/me/financial-health` and show the CIBIL-like credit score, positive factors, and pre-approved loan limit.
5. **Show the Printable Statement / Passbook:**
   - Generate `GET /api/customer/me/accounts/{accountId}/statement` and display the formatted ASCII passbook.
