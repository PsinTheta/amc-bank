package com.amcbank.simple;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.amcbank.simple.dto.FinancialHealthResponse;
import com.amcbank.simple.dto.ForexConversionRequest;
import com.amcbank.simple.dto.ForexConversionResponse;
import com.amcbank.simple.dto.FraudEvaluationResult;
import com.amcbank.simple.model.Account;
import com.amcbank.simple.model.Customer;
import com.amcbank.simple.model.FixedDeposit;
import com.amcbank.simple.model.Loan;
import com.amcbank.simple.repository.AccountRepository;
import com.amcbank.simple.repository.LoanRepository;
import com.amcbank.simple.repository.TransactionRepository;
import com.amcbank.simple.service.FinancialHealthService;
import com.amcbank.simple.service.ForexService;
import com.amcbank.simple.service.FraudDetectionService;

import static org.junit.jupiter.api.Assertions.*;

class BankFeaturesTest {

    @Test
    @DisplayName("Should detect high-value transaction and severe balance drain in fraud detection")
    void testFraudDetection() {
        TransactionRepository txRepo = Mockito.mock(TransactionRepository.class);
        FraudDetectionService fraudService = new FraudDetectionService(txRepo);

        Account account = new Account();
        account.setId(1L);
        account.setAccountNumber("AC12345678");
        account.setBalance(new BigDecimal("100000.00"));

        // Large withdrawal taking 90% of funds
        FraudEvaluationResult result = fraudService.evaluateTransaction(
                account, new BigDecimal("90000.00"), "WITHDRAWAL");

        assertNotNull(result);
        assertTrue(result.getRiskScore() >= 55, "Score should be elevated for high value + drain");
        assertFalse(result.getRiskFactors().isEmpty());
    }

    @Test
    @DisplayName("Should calculate quarterly compound interest for Fixed Deposit")
    void testFixedDepositCalculation() {
        FixedDeposit fd = new FixedDeposit();
        fd.setDepositAmount(new BigDecimal("100000.00"));
        fd.setInterestRate(8.0);
        fd.setTenureMonths(12); // 1 year = 4 quarters
        fd.setStartDate(LocalDate.now());
        fd.setMaturityDate(LocalDate.now().plusMonths(12));

        fd.calculateMaturity();

        // 100000 * (1 + 0.08/4)^4 = 100000 * (1.02)^4 ≈ 108243.22
        assertNotNull(fd.getMaturityAmount());
        assertTrue(fd.getMaturityAmount().compareTo(new BigDecimal("108000.00")) > 0);
        assertTrue(fd.getMaturityAmount().compareTo(new BigDecimal("109000.00")) < 0);
    }

    @Test
    @DisplayName("Should calculate forex exchange rates and bank conversion fees")
    void testForexConversion() {
        ForexService forexService = new ForexService();
        ForexConversionRequest request = new ForexConversionRequest("USD", "INR", new BigDecimal("100.00"));

        ForexConversionResponse response = forexService.convert(request);

        assertNotNull(response);
        assertEquals("USD", response.getFromCurrency());
        assertEquals("INR", response.getToCurrency());
        assertTrue(response.getConvertedAmount().compareTo(new BigDecimal("8000.00")) > 0);
        assertTrue(response.getFeeAmount().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(response.getNetPayoutAmount().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should compute CIBIL credit score and rating for customer")
    void testFinancialHealthAssessment() {
        AccountRepository accountRepo = Mockito.mock(AccountRepository.class);
        LoanRepository loanRepo = Mockito.mock(LoanRepository.class);
        FinancialHealthService healthService = new FinancialHealthService(accountRepo, loanRepo);

        Customer customer = new Customer("John Doe", "john@example.com", "Mumbai", "ABCDE1234F");
        customer.setId(10L);

        Account account = new Account();
        account.setId(101L);
        account.setBalance(new BigDecimal("250000.00"));
        account.setStatus("ACTIVE");

        Mockito.when(accountRepo.findByCustomerId(10L)).thenReturn(List.of(account));
        Mockito.when(loanRepo.findByCustomerId(10L)).thenReturn(List.of());

        FinancialHealthResponse health = healthService.assessCustomer(customer);

        assertNotNull(health);
        assertTrue(health.getCreditScore() >= 750, "High savings with zero debt should yield Excellent rating");
        assertEquals("EXCELLENT", health.getRating());
        assertTrue(health.getPreApprovedLoanLimit().compareTo(BigDecimal.ZERO) > 0);
    }
}
