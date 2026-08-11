package com.example.payment.repository;

import com.example.payment.entity.ClsRepayRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClsRepayRecordRepository extends JpaRepository<ClsRepayRecord, Long> {
    
    /**
     * 根据借据号查询还款记录
     */
    List<ClsRepayRecord> findByBillNoOrderByPayTimeDesc(String billNo);
    
    /**
     * 根据订单号查询
     */
    List<ClsRepayRecord> findByOrderId(String orderId);
    
    /**
     * 查询失败的还款记录
     */
    List<ClsRepayRecord> findByPayStatus(String payStatus);
}
