package com.example.payment.repository;

import com.example.payment.entity.CustInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustInfoRepository extends JpaRepository<CustInfo, Long> {
    CustInfo findByCustId(String custId);
}
