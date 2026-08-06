package com.example.payment.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "cls_loan_def")
public class ClsLoanDef {
    @Id
    @Column(name = "LOAN_REG_ID", nullable = false)
    private Long loanRegId;

    @Column(name = "INTEREST_RATE", nullable = false, precision = 10, scale = 6)
    private BigDecimal interestRate;

    @Column(name = "INTEREST_RATE_TYPE", length = 32)
    private String interestRateType;

    @Column(name = "FLOAT_RATE", precision = 10, scale = 6)
    private BigDecimal floatRate;

}
