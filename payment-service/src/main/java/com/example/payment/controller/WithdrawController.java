package com.example.payment.controller;

import com.example.payment.entity.*;
import com.example.payment.entity.req.WithdrawReq;
import com.example.payment.enums.RepayMethod;
import com.example.payment.enums.WithdrawMode;
import com.example.payment.service.CardInquirer;
import com.example.payment.service.ContractInquirer;
import com.example.payment.service.CustInquirer;
import com.example.payment.service.RepaymentService;
import com.example.payment.service.ReconciliationService;
import com.example.payment.service.WithdrawService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 提款控制层
 */
@RestController
@RequestMapping("/api/loan")
public class WithdrawController {

    @Autowired
    private WithdrawService withdrawService;

    @Autowired
    private RepaymentService repaymentService;

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private ContractInquirer contractInquirer;

    @Autowired
    private CustInquirer custInquirer;
    
    @Autowired
    private CardInquirer cardInquirer;

    /**
     * 发起提款申请
     */
    @PostMapping("/withdraw")
    public Map<String, Object> withdraw(@RequestBody WithdrawReq req) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 根据客户ID查询有效合同（而不是直接用exBizOrderId作为合同号）
            ClsContract contract = contractInquirer.getFirstActiveContractByCustId(req.getApplyUserId());
            if (contract == null) {
                result.put("success", false);
                result.put("message", "客户没有有效合同，custId: " + req.getApplyUserId());
                return result;
            }

            // 2. 加载客户信息
            CustInfo custInfo = custInquirer.findByCustId(contract.getCustId());
            if (custInfo == null) {
                result.put("success", false);
                result.put("message", "客户信息不存在");
                return result;
            }
            
            // 3. 加载客户银行卡信息
            List<CustCard> cards = cardInquirer.findByCustId(custInfo.getCustId());
            if (cards == null || cards.isEmpty()) {
                result.put("success", false);
                result.put("message", "客户银行卡不存在");
                return result;
            }
            CustCard custCard = cards.get(0); // 使用第一张卡

            // 4. 构建产品信息（简化处理，实际应从配置或数据库获取）
            Product product = new Product();
            product.setProductId("PROD001");
            product.setProductName("个人消费贷款");
            product.setWithdrawMode(WithdrawMode.L); // 放款到客户
            product.setRepayMethod(RepayMethod.AT); // 随借随还

            // 5. 构建贷款信息
            LoanInfo loanInfo = new LoanInfo();
            loanInfo.setProduct(product);
            loanInfo.setClsContr(contract);
            loanInfo.setCustInfo(custInfo);
            loanInfo.setCustCard(custCard);

            // 6. 处理提款
            String orderId = withdrawService.processWithdrawMode(req, custInfo, loanInfo, false);

            result.put("success", true);
            result.put("orderId", orderId);
            result.put("message", "提款申请已提交");

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "提款失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 人工审核通过
     *
     */
    @PostMapping("/approve/{loanRegId}")
    public Map<String, Object> approve(@PathVariable String loanRegId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (loanRegId == null || loanRegId.isEmpty()) {
                result.put("success", false);
                result.put("message", "贷款登记ID不能为空");
                return result;
            }
            
            String billNo = withdrawService.approveWithdraw(loanRegId);
            
            result.put("success", true);
            result.put("billNo", billNo);
            result.put("message", "审核通过");

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "审核失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 重试提款（支付失败后重提）
     */
    @PostMapping("/retry/{orderId}")
    public Map<String, Object> retryWithdraw(@PathVariable String orderId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String newOrderId = withdrawService.retryWithdraw(orderId);
            
            result.put("success", true);
            result.put("orderId", newOrderId);
            result.put("message", "订单已重置，可以重新支付");

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "重试失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 发起还款
     */
    @PostMapping("/repay")
    public Map<String, Object> repay(@RequestParam String billNo, 
                                     @RequestParam String amount,
                                     @RequestParam(required = false, defaultValue = "NORMAL") String repayType) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String orderId = repaymentService.processRepayment(billNo, new java.math.BigDecimal(amount), repayType);
            
            result.put("success", true);
            result.put("orderId", orderId);
            result.put("message", "还款处理中");

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "还款失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 重试还款
     */
    @PostMapping("/repay/retry/{orderId}")
    public Map<String, Object> retryRepay(@PathVariable String orderId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            repaymentService.retryFailedRepayment(orderId);
            
            result.put("success", true);
            result.put("message", "订单已重置，可以重新支付");

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "重试失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 手动触发对账
     */
    @PostMapping("/reconcile")
    public Map<String, Object> reconcile() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            reconciliationService.executeReconciliation();
            
            result.put("success", true);
            result.put("message", "对账完成");

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "对账失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 查询待处理的对账异常
     */
    @GetMapping("/reconcile/exceptions")
    public Map<String, Object> getExceptions() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            var exceptions = reconciliationService.getPendingExceptions();
            
            result.put("success", true);
            result.put("data", exceptions);
            result.put("count", exceptions.size());

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 人工处理对账异常
     */
    @PostMapping("/reconcile/handle")
    public Map<String, Object> handleException(@RequestParam Long exceptionId,
                                               @RequestParam String handleMethod,
                                               @RequestParam String result) {
        Map<String, Object> resp = new HashMap<>();
        
        try {
            reconciliationService.handleExceptionManually(exceptionId, handleMethod, result);
            
            resp.put("success", true);
            resp.put("message", "处理完成");

        } catch (Exception e) {
            resp.put("success", false);
            resp.put("message", "处理失败: " + e.getMessage());
        }

        return resp;
    }
}
