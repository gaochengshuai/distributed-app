package com.example.payment.repository;

import com.example.payment.entity.ClsLoanEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClsLoanEventRepository extends JpaRepository<ClsLoanEvent, Long> {
    
    /**
     * 根据借据号查询事件
     */
    List<ClsLoanEvent> findByBillNo(String billNo);
    
    /**
     * 根据合同号查询事件
     */
    List<ClsLoanEvent> findByContrNo(String contrNo);
    
    /**
     * 根据客户ID查询事件
     */
    List<ClsLoanEvent> findByCustId(String custId);
    
    /**
     * 根据事件类型查询
     */
    List<ClsLoanEvent> findByLoanEventType(String loanEventType);
}
