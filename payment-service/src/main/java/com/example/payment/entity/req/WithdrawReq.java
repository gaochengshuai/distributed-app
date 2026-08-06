package com.example.payment.entity.req;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 提款请求实体
 * */

@Data
public class WithdrawReq implements Serializable {
    private static final long serialVersionUID = 1L;
    /*外部订单号*/
    private String exBizOrderId;
    /*提款金额*/
    private BigDecimal withdrawAmt;
    /*手续费*/
    private BigDecimal withdrawFee;
    /*贷款金额*/
    private BigDecimal LoanPrin;
    /*期数*/
    private int loanTerm;
    /*还款方式*/
    private String repayType;
    /*申请人ID*/
    private String applyUserId;
    /*申请人姓名*/
    private String applyUserName;
}
