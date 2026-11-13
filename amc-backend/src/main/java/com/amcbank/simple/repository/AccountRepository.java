package com.amcbank.simple.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import com.amcbank.simple.model.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {

    // Account also stores a Customer object rather than a customerId field.
    @Query("select a from Account a where a.customer.id = :customerId")
    List<Account> findByCustomerId(@Param("customerId") Long customerId);

    Optional<Account> findByAccountNumber(String accountNumber);

    long countByStatus(String status);

    @Query("select coalesce(sum(a.balance), 0) from Account a where a.status = 'ACTIVE'")
    java.math.BigDecimal sumTotalActiveBalance();
}
