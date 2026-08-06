package com.example.payment.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "cls_contract")
public class ClsContract {
    @Id
    private String contrNo;
    private String custId;
    private String productId;
    private BigDecimal signAmt;
    private String contrStatus;
}
