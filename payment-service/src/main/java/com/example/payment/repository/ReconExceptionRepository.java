package com.example.payment.repository;

import com.example.payment.entity.ReconException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReconExceptionRepository extends JpaRepository<ReconException, Long> {
    
    /**
     * 根据异常类型查询
     */
    List<ReconException> findByExceptionType(String exceptionType);
    
    /**
     * 根据状态查询
     */
    List<ReconException> findByStatus(String status);
    
    /**
     * 根据订单号查询
     */
    List<ReconException> findByOrderId(String orderId);
}
