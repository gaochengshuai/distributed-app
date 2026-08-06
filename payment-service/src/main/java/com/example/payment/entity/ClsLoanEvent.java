package com.example.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

import java.util.Date;


@Data
@Entity
@Table(name = "cls_loan_event")
public class ClsLoanEvent {
    @Id
    @Column(name = "LOAN_EVENT_ID", nullable = false)
    private Long loanEventId;

    @Column(name = "CUST_ID", nullable = false, length = 32)
    private String custId;

    @Column(name = "CONTR_NO", length = 32)
    private String contrNo;

    @Column(name = "BILL_NO", length = 32)
    private String billNo;

    @Column(name = "TERM_NO")
    private Integer termNo;

    @Column(name = "LOAN_EVENT_TYPE", nullable = false, length = 40)
    private String loanEventType;

    @ColumnDefault("''")
    @Column(name = "MEMO", nullable = false, length = 400)
    private String memo;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "SETUP_TIME", nullable = false)
    private Date setupTime;

    @Column(name = "OPT_USER_NAME", length = 40)
    private String optUserName;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "CREATE_TIME", nullable = false)
    private Date createTime;

    @ColumnDefault("'SYSTEM'")
    @Column(name = "CREATE_USER", nullable = false, length = 32)
    private String createUser;

    @ColumnDefault("0")
    @Column(name = "JPA_VERSION", nullable = false)
    private Long jpaVersion;

}