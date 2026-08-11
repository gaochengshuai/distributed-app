package com.example.payment.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 对账异常表
 */
@Data
@Entity
@Table(name = "recon_exception")
public class ReconException {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EXCEPTION_ID")
    private Long exceptionId;
    
    @Column(name = "ORDER_ID", length = 32)
    private String orderId;
    
    @Column(name = "BILL_NO", length = 32)
    private String billNo;
    
    @Column(name = "CUST_ID", length = 32)
    private String custId;
    
    @Column(name = "EXCEPTION_TYPE", length = 32, nullable = false)
    private String exceptionType; 
    // PAY_SUCCESS_CORE_FAIL-支付成功核心失败
    // CORE_SUCCESS_PAY_FAIL-核心成功支付失败
    // AMT_MISMATCH-金额不一致
    
    @Column(name = "PAY_AMT", precision = 15, scale = 2)
    private BigDecimal payAmt;
    
    @Column(name = "CORE_AMT", precision = 15, scale = 2)
    private BigDecimal coreAmt;
    
    @Column(name = "DIFF_AMT", precision = 15, scale = 2)
    private BigDecimal diffAmt;
    
    @Column(name = "STATUS", length = 4)
    private String status; // P-待处理, H-人工处理, R-已解决
    
    @Column(name = "HANDLE_METHOD", length = 32)
    private String handleMethod; 
    // AUTO_RETRY-自动重试
    // MANUAL-人工处理
    // WRITE_OFF-核销
    // REVERSAL-冲正
    
    @Column(name = "HANDLE_RESULT", length = 500)
    private String handleResult;
    
    @Column(name = "CREATE_TIME")
    private Date createTime;
    
    @Column(name = "HANDLE_TIME")
    private Date handleTime;
    
    @Column(name = "REMARK", length = 1000)
    private String remark;
}
