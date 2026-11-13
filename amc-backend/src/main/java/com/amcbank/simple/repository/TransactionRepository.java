package com.amcbank.simple.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.amcbank.simple.model.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("select t from Transaction t where t.account.id = :accountId order by t.timestamp desc")
    List<Transaction> findByAccountIdOrderByTimestampDesc(@Param("accountId") Long accountId);

    @Query("select t from Transaction t where t.account.customer.id = :customerId order by t.timestamp desc")
    List<Transaction> findByCustomerIdOrderByTimestampDesc(@Param("customerId") Long customerId);

    List<Transaction> findAllByOrderByTimestampDesc();

    List<Transaction> findByStatusOrderByTimestampDesc(String status);

    long countByStatus(String status);

    // Used for velocity checking in fraud detection
    @Query("select count(t) from Transaction t where t.account.id = :accountId and t.timestamp >= :since")
    long countRecentTransactions(@Param("accountId") Long accountId, @Param("since") LocalDateTime since);

    @Query("select t from Transaction t where t.account.id = :accountId and t.timestamp between :start and :end order by t.timestamp asc")
    List<Transaction> findTransactionsForPeriod(@Param("accountId") Long accountId,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);
}
