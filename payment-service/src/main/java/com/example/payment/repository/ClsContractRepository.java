package com.example.payment.repository;

import com.example.payment.entity.ClsContract;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClsContractRepository extends JpaRepository<ClsContract, Long> {
    ClsContract findByContrNo(String contrNo);
}
