package com.amcbank.simple.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "fixed_deposits")
public class FixedDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String fdNumber;

    @Column(nullable = false)
    private BigDecimal depositAmount;

    @Column(nullable = false)
    private double interestRate;

    @Column(nullable = false)
    private int tenureMonths;

    @Column(nullable = false)
    private BigDecimal maturityAmount;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate maturityDate;

    // ACTIVE, CLOSED
    @Column(nullable = false)
    private String status = "ACTIVE";

    private String fundingAccountNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    public FixedDeposit() {
    }

    public void calculateMaturity() {
        if (depositAmount == null || tenureMonths <= 0) {
            maturityAmount = BigDecimal.ZERO;
            return;
        }

        // Standard Quarterly Compounded Fixed Deposit Formula:
        // A = P * (1 + r / (4 * 100)) ^ (4 * (tenureMonths / 12))
        double p = depositAmount.doubleValue();
        double r = interestRate / 100.0;
        double quarters = tenureMonths / 3.0;
        double a = p * Math.pow(1.0 + (r / 4.0), quarters);

        this.maturityAmount = BigDecimal.valueOf(a).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFdNumber() {
        return fdNumber;
    }

    public void setFdNumber(String fdNumber) {
        this.fdNumber = fdNumber;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(double interestRate) {
        this.interestRate = interestRate;
    }

    public int getTenureMonths() {
        return tenureMonths;
    }

    public void setTenureMonths(int tenureMonths) {
        this.tenureMonths = tenureMonths;
    }

    public BigDecimal getMaturityAmount() {
        return maturityAmount;
    }

    public void setMaturityAmount(BigDecimal maturityAmount) {
        this.maturityAmount = maturityAmount;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }

    public void setMaturityDate(LocalDate maturityDate) {
        this.maturityDate = maturityDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFundingAccountNumber() {
        return fundingAccountNumber;
    }

    public void setFundingAccountNumber(String fundingAccountNumber) {
        this.fundingAccountNumber = fundingAccountNumber;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Long getCustomerId() {
        return customer != null ? customer.getId() : null;
    }

    public String getCustomerName() {
        return customer != null ? customer.getName() : null;
    }
}
