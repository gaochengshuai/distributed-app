package com.example.payment.enums;

public enum WithdrawMode {
    /**
     * 放款到客户 Lending (建contr建loan建order)
     */
    L,
    /**
     * 结算到商户 Merchant (建contr建loan不order)
     */
    M,
    /**
     * 放款到客户不记账
     */
    P,
    /**
     * 结算到商户不记账
     */
    N;
}
