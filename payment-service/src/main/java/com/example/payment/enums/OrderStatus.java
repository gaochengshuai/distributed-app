package com.example.payment.enums;

public enum OrderStatus {
    /**
     * 未支付Un-pay
     */
    U,
    /**
     * 支付中Journey
     */
    J,
    /**
     * 已支付Success
     */
    S,
    /**
     * 支付失败Failed
     */
    F,
    /**
     * 已重提Re-pay
     */
    R,
    /**
     * 已撤销Cancel
     */
    C,
    /**
     * 已失效Void
     */
    V;
}
