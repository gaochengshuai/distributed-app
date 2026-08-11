package com.example.payment.entity;

import lombok.Data;

@Data
public class LoanInfo {
    private Product product;
    private ClsContract clsContr;
    private ClsLoanReg loanReg;
    private CustCard custCard;
    private ClsOrder order;
    private CustInfo custInfo;
    private ClsLoanDef loanDef;
}
