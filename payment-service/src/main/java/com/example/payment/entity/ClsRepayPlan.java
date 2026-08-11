package com.example.payment.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 还款计划表
 */
@Data
@Entity
@Table(name = "cls_repay_plan")
public class ClsRepayPlan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PLAN_ID")
    private Long planId;
    
    @Column(name = "BILL_NO", length = 32, nullable = false)
    private String billNo;
    
    @Column(name = "CONTR_NO", length = 32)
    private String contrNo;
    
    @Column(name = "CUST_ID", length = 32)
    private String custId;
    
    @Column(name = "TERM_NO")
    private Integer termNo;
    
    @Column(name = "DUE_DATE")
    private Date dueDate;
    
    @Column(name = "PRIN_AMT", precision = 15, scale = 2)
    private BigDecimal prinAmt;
    
    @Column(name = "INTEREST_AMT", precision = 15, scale = 2)
    private BigDecimal interestAmt;
    
    @Column(name = "FEE_AMT", precision = 15, scale = 2)
    private BigDecimal feeAmt;
    
    @Column(name = "TOTAL_AMT", precision = 15, scale = 2)
    private BigDecimal totalAmt;
    
    @Column(name = "PAID_PRIN", precision = 15, scale = 2)
    private BigDecimal paidPrin;
    
    @Column(name = "PAID_INTEREST", precision = 15, scale = 2)
    private BigDecimal paidInterest;
    
    @Column(name = "PAID_FEE", precision = 15, scale = 2)
    private BigDecimal paidFee;
    
    @Column(name = "REMAIN_AMT", precision = 15, scale = 2)
    private BigDecimal remainAmt;
    
    @Column(name = "STATUS", length = 4)
    private String status; // U-未还, P-部分还款, S-已还清, O-逾期
    
    @Column(name = "OVERDUE_DAYS")
    private Integer overdueDays;
    
    @Column(name = "CREATE_TIME")
    private Date createTime;
    
    @Column(name = "UPDATE_TIME")
    private Date updateTime;
}
