package com.example.payment.entity;

import com.example.payment.enums.RepayMethod;
import com.example.payment.enums.WithdrawMode;
import lombok.Data;

import java.io.Serializable;

@Data
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 产品代码
     */
    private String productCd;

    /**
     * 产品名称
     */
    private String productName;


    /**
     * 描述
     */
    private String description;


    /**
     * 还款方式
     */
    public WithdrawMode withdrawMode;
    /**
     *
     * */
    public RepayMethod repayMethod;

}
