package com.example.payment.service;

import com.example.payment.entity.*;
import com.example.payment.entity.req.WithdrawReq;
import com.example.payment.enums.OrderStatus;
import com.example.payment.enums.OrderType;
import com.example.payment.enums.RepayMethod;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

@Component
public class WithdrawService {

    @PersistenceContext
    private EntityManager em;
    private static Logger logger = LoggerFactory.getLogger(WithdrawService.class);
    // ===== WithdrawService.processWithdrawMode() =====
    public void processWithdrawMode(WithdrawReq wd, CustInfo cust, LoanInfo info, boolean isTrial) {

        // 【关键步骤1】判断是否需要人工审核
        String needAudit = "Y";
        // 返回："超出自动审批金额上限" （因为50000 > 30000）

        // 【关键步骤2】借据定价
        this.newLoanDef(wd, cust, info);
        /*
         * 生成内容：
         * - 年利率：12%（协议利率）
         * - 月利率：1%
         * - 服务费定义：
         *   - 提款手续费：0.5%（趸交）
         *   - 印花税：0.05%（趸交）
         *   - 增值税：6%（按期收取）
         */

        // 【关键步骤3】计算提款手续费
        BigDecimal withdrawTxnFee = this.genWithdrawFee(wd,info);
        // withdrawTxnFee = 50000 * 0.5% = 250元

        // 【关键步骤4】创建贷款登记记录
        ClsLoanReg loanReg = this.newLoanReg(wd, withdrawTxnFee, info);
        /*
         * ClsLoanReg字段：
         * - loanRegId: 自动生成
         * - contrNo: "CONTR202606130001"
         * - billNo: null（审核后生成）
         * - lendingAmt: 50000 - 250 = 49750（实际到账金额）
         * - txnFeeAmt: 250
         * - loanAction: A（新增）
         * - auditResult: U（待审核）
         * - loanRegStatus: U（待审核）
         * - exBizOrderId: "EXT_ORDER_20260613_001"
         * - attachInfo: JSON序列化WithdrawItem
         */

        // 【关键步骤5】持久化定价信息
        if(!isTrial){
            saveLoanRegLoanDef(info, loanReg);
            /*
             * 插入表：
             * - CLS_LOAN_DEF：借据定义（利率、费用定义）
             * - CLS_LOAN_FEE_DEF：费用明细
             */
        }

        // 【关键步骤6】根据放款模式分支处理
        switch (info.getProduct().withdrawMode) {
            case L:  // 放款到客户
                if(StringUtils.isBlank(needAudit)){
                    // 无需审核时直接创建支付订单
                    ClsOrder order = new ClsOrder();
                    order.setContrNo(info.getClsContr().getContrNo());
                    if(info != null){
                        order.setBillNo(info.getOrder().getBillNo());
                    } else {
                        order.setBillNo("");
                    }
                    order.setOrderType(String.valueOf(OrderType.WITHDRAW));
                    order.setOrderAmt(loanReg.getLendingAmt().subtract(loanReg.getTxnFeeAmt()));
                    order.setCardNo(info.getCustCard().getCardNo());
                    order.setBankCode(info.getCustCard().getBankCode());
                    order.setExtOrderId(wd.getExBizOrderId());
                    order.setOrderStatus(String.valueOf(OrderStatus.U));
                    info.setOrder(order);
                    loanReg.setOrderId(order.getOrderId());
                } else {
                    // 需要审核时，暂不创建订单，等待审核通过后再创建
                    logger.debug("需要人工审核，暂不创建支付订单");
                }
                break;
        }
    }
    /**
     * 生成提款手续费
     * @param wd WithdrawReq
     * @param info
     * @return
     */
    private BigDecimal genWithdrawFee(WithdrawReq wd, LoanInfo info) {
        if(info.getProduct().repayMethod!= RepayMethod.AT){
            return BigDecimal.ZERO;
        }
        BigDecimal txnFeeAmt = BigDecimal.ZERO;
        txnFeeAmt = wd.getWithdrawAmt().multiply(wd.getWithdrawFee());
        return txnFeeAmt;
    }

    /**
     * 创建贷款定价及费用定价
     * @param wd
     * @param cust
     * @param info
     */
    private void newLoanDef(WithdrawReq wd, CustInfo cust, LoanInfo info) {
        // 协议利率处理
        Product product = info.getProduct();

        ClsLoanDef loanDef = new ClsLoanDef();
        loanDef.setLoanRegId(-1l);
        loanDef.setInterestRate(BigDecimal.ZERO);
        loanDef.setInterestRateType("Y");
        loanDef.setFloatRate(new BigDecimal(0.05));
        em.persist(loanDef);
    }

    /**
     * 创建贷款登记信息
     * @param wd
     * @param withdrawTxnFee
     * @param info
     * @return
     */
    private ClsLoanReg newLoanReg(WithdrawReq wd, BigDecimal withdrawTxnFee, LoanInfo info) {
        ClsLoanReg loanReg = new ClsLoanReg();
        loanReg.setLoanRegId(null);
        loanReg.setCustId(info.getCustInfo().getCustId());
        loanReg.setContrNo(info.getClsContr().getContrNo());
        loanReg.setBillNo("");
        loanReg.setLoanRegStatus(String.valueOf(OrderStatus.U));
        loanReg.setLoanPrin(wd.getLoanPrin());
        loanReg.setOrderId(null);
        loanReg.setLendingAmt(wd.getWithdrawAmt());
        loanReg.setTxnFeeAmt(withdrawTxnFee);
        loanReg.setApllyTime(new Date());
        em.persist(loanReg);
        return loanReg;
    }


    private void saveLoanRegLoanDef(LoanInfo info, ClsLoanReg loanReg) {
        em.persist(loanReg);
        info.getLoanReg().setLoanRegId(loanReg.getLoanRegId());
        em.persist(info.getLoanReg());
    }

    /**
     * 
     * */
}
