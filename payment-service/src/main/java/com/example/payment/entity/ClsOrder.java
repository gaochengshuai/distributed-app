package com.example.payment.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ClsOrder {

    private String orderId;
    private String contrNo;
    private String billNo;
    private String orderType;
    private BigDecimal orderAmt;
    private String cardNo;
    private String bankCode;
    private String extOrderId;
    private String orderStatus;
}
