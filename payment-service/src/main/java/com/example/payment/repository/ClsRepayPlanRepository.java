package com.example.payment.repository;

import com.example.payment.entity.ClsRepayPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClsRepayPlanRepository extends JpaRepository<ClsRepayPlan, Long> {
    
    /**
     * 根据借据号查询还款计划
     */
    List<ClsRepayPlan> findByBillNoOrderByTermNo(String billNo);
    
    /**
     * 根据合同号查询
     */
    List<ClsRepayPlan> findByContrNoOrderByTermNo(String contrNo);
    
    /**
     * 查询逾期的还款计划
     */
    List<ClsRepayPlan> findByStatusAndOverdueDaysGreaterThan(String status, int days);
    
    /**
     * 查询某期还款计划
     */
    Optional<ClsRepayPlan> findByBillNoAndTermNo(String billNo, Integer termNo);
}
