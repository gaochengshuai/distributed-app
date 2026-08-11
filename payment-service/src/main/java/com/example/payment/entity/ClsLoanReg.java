package com.example.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "cls_loan_reg")
public class ClsLoanReg {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LOAN_REG_ID", nullable = false)
    private Long loanRegId;

    @Column(name = "CUST_ID", nullable = false, length = 32)
    private String custId;

    @Column(name = "CONTR_NO", length = 32)
    private String contrNo;

    @Column(name = "BILL_NO", length = 32)
    private String billNo;

    @Column(name = "LENDING_AMT")
    private BigDecimal lendingAmt;

    @Column(name = "LOAN_PRIN", nullable = false, precision = 15, scale = 2)
    private BigDecimal loanPrin;

    @ColumnDefault("0.00")
    @Column(name = "TXN_FEE_AMT", nullable = false, precision = 15, scale = 2)
    private BigDecimal txnFeeAmt;

    @Column(name = "LOAN_ACTION", nullable = false, length = 32)
    private String loanAction;

    @ColumnDefault("'U'")
    @Column(name = "AUDIT_RESULT", nullable = false, length = 40)
    private String auditResult;

    @ColumnDefault("'U'")
    @Column(name = "LOAN_REG_STATUS", nullable = false, length = 32)
    private String loanRegStatus;

    @ColumnDefault("'SYSTEM'")
    @Column(name = "EXT_BIZ_ORDER_ID", nullable = false, length = 32)
    private String extBizOrderId;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "APLLY_TIME", nullable = false)
    private Date apllyTime;

    private String orderId;

}
