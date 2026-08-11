package com.example.payment.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "cls_order")
public class ClsOrder {

    @Id
    @Column(name = "ORDER_ID", length = 32)
    private String orderId;
    
    @Column(name = "CONTR_NO", length = 32)
    private String contrNo;
    
    @Column(name = "BILL_NO", length = 32)
    private String billNo;
    
    @Column(name = "ORDER_TYPE", length = 32)
    private String orderType;
    
    @Column(name = "ORDER_AMT", precision = 15, scale = 2)
    private BigDecimal orderAmt;
    
    @Column(name = "CARD_NO", length = 32)
    private String cardNo;
    
    @Column(name = "BANK_CODE", length = 32)
    private String bankCode;
    
    @Column(name = "EXT_ORDER_ID", length = 32)
    private String extOrderId;
    
    @Column(name = "ORDER_STATUS", length = 4)
    private String orderStatus;
    
    @Column(name = "FAIL_REASON", length = 500)
    private String failReason;
    
    @Column(name = "PAY_TIME")
    private Date payTime;
    
    @Column(name = "CREATE_TIME")
    private Date createTime;
    
    @Column(name = "UPDATE_TIME")
    private Date updateTime;
    
    @Column(name = "AUDIT_STATUS", length = 4)
    private String auditStatus;
    
    @Column(name = "NEED_AUDIT", length = 1)
    private String needAudit;
    
    @Column(name = "PRODUCT_ID", length = 32)
    private String productId;
    
    @Column(name = "WITHDRAW_MODE", length = 4)
    private String withdrawMode;
}
