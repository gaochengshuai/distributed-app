package com.example.payment.service;

import com.example.payment.entity.req.WithdrawReq;
import com.example.payment.entity.resp.WithdrawResp;

public class TxnContext {
    private WithdrawReq reqInfo;
    private WithdrawResp respInfo;

    // 业务数据（Filter之间共享）
    private Object loanInfo;
    private Object orderInfo;
    private Object contInfo;
    private Object custInfo;

    // ========== Getter/Setter ==========

    public WithdrawReq getReqInfo() {
        return reqInfo;
    }

    public void setReqInfo(WithdrawReq reqInfo) {
        this.reqInfo = reqInfo;
    }

    public WithdrawResp getRespInfo() {
        return respInfo;
    }

    public void setRespInfo(WithdrawResp respInfo) {
        this.respInfo = respInfo;
    }

    public Object getLoanInfo() {
        return loanInfo;
    }

    public void setLoanInfo(Object loanInfo) {
        this.loanInfo = loanInfo;
    }

    public Object getOrderInfo() {
        return orderInfo;
    }

    public void setOrderInfo(Object orderInfo) {
        this.orderInfo = orderInfo;
    }

    public void setContInfo(Object contInfo) {
        this.contInfo = contInfo;
    }

    public Object getContInfo() {
        return contInfo;
    }
    public void setCustInfo(Object custInfo) {
        this.custInfo = custInfo;
    }
    public Object getCustInfo() {
        return custInfo;
    }
}
