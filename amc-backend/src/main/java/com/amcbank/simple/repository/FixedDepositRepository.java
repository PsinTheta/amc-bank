package com.amcbank.simple.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.amcbank.simple.model.FixedDeposit;

public interface FixedDepositRepository extends JpaRepository<FixedDeposit, Long> {

    @Query("select fd from FixedDeposit fd where fd.customer.id = :customerId order by fd.id desc")
    List<FixedDeposit> findByCustomerId(@Param("customerId") Long customerId);

    Optional<FixedDeposit> findByFdNumber(String fdNumber);

    long countByStatus(String status);

    @Query("select coalesce(sum(fd.depositAmount), 0) from FixedDeposit fd where fd.status = 'ACTIVE'")
    java.math.BigDecimal sumTotalActiveDeposits();
}
