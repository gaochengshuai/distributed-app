package com.example.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Entity
@Table(name = "cust_card")
public class CustCard {
    @Id
    @Column(name = "CARD_NO", nullable = false, length = 32)
    private String cardNo;

    @Column(name = "BANK_CODE", nullable = false, length = 32)
    private String bankCode;

    @Column(name = "PAY_DIRECTION", nullable = false, length = 32)
    private String payDirection;

    @Column(name = "CUST_ID", nullable = false, length = 32)
    private String custId;

}