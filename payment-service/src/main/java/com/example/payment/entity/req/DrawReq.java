package com.example.payment.entity.req;

import com.example.payment.entity.ClsContract;
import com.example.payment.entity.CustCard;
import com.example.payment.entity.CustInfo;
import lombok.Data;

import java.io.Serializable;

@Data
public class DrawReq implements Serializable {
    private static final long serialVersionUID = 1L;
    private CustInfo custInfo;
    private ClsContract clsContract;
    private WithdrawReq withdrawReq;
    private CustCard custCard;
}
