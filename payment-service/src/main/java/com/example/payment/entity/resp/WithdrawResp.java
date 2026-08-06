package com.example.payment.entity.resp;

import lombok.Data;

import java.io.Serializable;

@Data
public class WithdrawResp extends BaseResp implements Serializable {
    private static final long serialVersionUID = 1L;
    private String billNo;

}
