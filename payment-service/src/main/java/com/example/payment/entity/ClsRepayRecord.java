package com.example.payment.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 还款记录表
 */
@Data
@Entity
@Table(name = "cls_repay_record")
public class ClsRepayRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RECORD_ID")
    private Long recordId;
    
    @Column(name = "BILL_NO", length = 32)
    private String billNo;
    
    @Column(name = "CONTR_NO", length = 32)
    private String contrNo;
    
    @Column(name = "CUST_ID", length = 32)
    private String custId;
    
    @Column(name = "ORDER_ID", length = 32)
    private String orderId;
    
    @Column(name = "REPAY_AMT", precision = 15, scale = 2)
    private BigDecimal repayAmt;
    
    @Column(name = "PRIN_AMT", precision = 15, scale = 2)
    private BigDecimal prinAmt;
    
    @Column(name = "INTEREST_AMT", precision = 15, scale = 2)
    private BigDecimal interestAmt;
    
    @Column(name = "FEE_AMT", precision = 15, scale = 2)
    private BigDecimal feeAmt;
    
    @Column(name = "OVERPAY_AMT", precision = 15, scale = 2)
    private BigDecimal overpayAmt; // 溢缴款
    
    @Column(name = "REPAY_TYPE", length = 32)
    private String repayType; // NORMAL-正常还款, EARLY-提前还款, OVERDUE-逾期还款
    
    @Column(name = "PAY_STATUS", length = 4)
    private String payStatus; // S-成功, F-失败, P-处理中
    
    @Column(name = "FAIL_REASON", length = 500)
    private String failReason;
    
    @Column(name = "PAY_TIME")
    private Date payTime;
    
    @Column(name = "ACCOUNT_TIME")
    private Date accountTime; // 入账时间
    
    @Column(name = "CREATE_TIME")
    private Date createTime;
    
    @Column(name = "REMARK", length = 500)
    private String remark;
}
