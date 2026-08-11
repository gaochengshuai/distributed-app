package com.example.payment.repository;

import com.example.payment.entity.ClsLoanReg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClsLoanRegRepository extends JpaRepository<ClsLoanReg, Long> {
    
    /**
     * 根据合同号查询贷款登记
     */
    List<ClsLoanReg> findByContrNo(String contrNo);
    
    /**
     * 根据外部订单号查询
     */
    Optional<ClsLoanReg> findByExtBizOrderId(String extBizOrderId);
    
    /**
     * 根据借据号查询
     */
    Optional<ClsLoanReg> findByBillNo(String billNo);
    
    /**
     * 查询待审核的贷款登记
     */
    @Query("SELECT r FROM ClsLoanReg r WHERE r.auditResult = 'U' AND r.loanRegStatus = 'U'")
    List<ClsLoanReg> findPendingAudit();
    
    /**
     * 根据客户ID查询
     */
    List<ClsLoanReg> findByCustId(String custId);
}
