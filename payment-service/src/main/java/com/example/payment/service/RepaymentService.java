package com.example.payment.service;

import com.example.payment.entity.*;
import com.example.payment.enums.OrderStatus;
import com.example.payment.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 还款服务
 */
@Service
public class RepaymentService {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ClsRepayPlanRepository repayPlanRepo;

    @Autowired
    private ClsRepayRecordRepository repayRecordRepo;

    @Autowired
    private ClsOrderRepository orderRepo;

    @Autowired
    private ClsLoanRegRepository loanRegRepo;

    @Autowired
    private ReconExceptionRepository reconExceptionRepo;

    private static final Logger logger = LoggerFactory.getLogger(RepaymentService.class);

    // 小额容差阈值（1元）
    private static final BigDecimal SMALL_DIFF_THRESHOLD = new BigDecimal("1.00");
    // 大额超额阈值（100元）
    private static final BigDecimal LARGE_OVERPAY_THRESHOLD = new BigDecimal("100.00");

    /**
     * 处理还款请求
     */
    @Transactional
    public String processRepayment(String billNo, BigDecimal repayAmt, String repayType) {
        logger.info("开始处理还款，billNo: {}, 金额: {}, 类型: {}", billNo, repayAmt, repayType);

        try {
            // 1. 查询还款计划
            List<ClsRepayPlan> plans = repayPlanRepo.findByBillNoOrderByTermNo(billNo);
            if (plans.isEmpty()) {
                throw new RuntimeException("借据不存在或无还款计划");
            }

            // 2. 计算应还总额
            BigDecimal totalDue = calculateTotalDue(plans);
            logger.info("应还总额: {}", totalDue);

            // 3. 创建还款订单
            ClsOrder order = createRepayOrder(billNo, repayAmt, repayType);

            // 4. 调用支付网关（模拟）
            boolean paySuccess = callPaymentGateway(order.getOrderId(), repayAmt);

            if (!paySuccess) {
                // 支付失败处理
                handleRepayPayFail(order.getOrderId(), "支付网关扣款失败");
                return order.getOrderId();
            }

            // 5. 支付成功，执行入账
            try {
                executeAccounting(billNo, repayAmt, order.getOrderId(), repayType);
                
                // 更新订单状态为成功
                order.setOrderStatus(String.valueOf(OrderStatus.S));
                order.setPayTime(new Date());
                order.setUpdateTime(new Date());
                em.merge(order);

                logger.info("还款处理成功，orderId: {}", order.getOrderId());
                return order.getOrderId();

            } catch (Exception e) {
                // 核心入账异常处理
                logger.error("核心入账异常", e);
                handleRepayAccountFail(order.getOrderId(), repayAmt, e.getMessage());
                throw new RuntimeException("还款入账失败，等待对账处理: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            logger.error("还款处理异常", e);
            throw new RuntimeException("还款处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 计算应还总额
     */
    private BigDecimal calculateTotalDue(List<ClsRepayPlan> plans) {
        BigDecimal total = BigDecimal.ZERO;
        for (ClsRepayPlan plan : plans) {
            if ("U".equals(plan.getStatus()) || "P".equals(plan.getStatus())) {
                total = total.add(plan.getRemainAmt());
            }
        }
        return total;
    }

    /**
     * 创建还款订单
     */
    private ClsOrder createRepayOrder(String billNo, BigDecimal repayAmt, String repayType) {
        ClsLoanReg loanReg = loanRegRepo.findByBillNo(billNo)
                .orElseThrow(() -> new RuntimeException("借据不存在"));

        ClsOrder order = new ClsOrder();
        order.setOrderId(generateOrderId());
        order.setContrNo(loanReg.getContrNo());
        order.setBillNo(billNo);
        order.setOrderType("REPAY");
        order.setOrderAmt(repayAmt);
        order.setCardNo(""); // TODO: 从客户信息获取
        order.setBankCode("");
        order.setExtOrderId("REP" + System.currentTimeMillis());
        order.setOrderStatus(String.valueOf(OrderStatus.J)); // 支付中
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());

        em.persist(order);
        return order;
    }

    /**
     * 生成订单号
     */
    private String generateOrderId() {
        return "REP" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * 调用支付网关（模拟）
     */
    private boolean callPaymentGateway(String orderId, BigDecimal amount) {
        // TODO: 实际调用支付网关
        logger.info("调用支付网关，orderId: {}, amount: {}", orderId, amount);
        // 模拟90%成功率
        return Math.random() > 0.1;
    }

    /**
     * 执行入账逻辑（冲销引擎：费-息-本）
     */
    @Transactional
    public void executeAccounting(String billNo, BigDecimal repayAmt, String orderId, String repayType) {
        logger.info("执行入账逻辑，billNo: {}, repayAmt: {}", billNo, repayAmt);

        List<ClsRepayPlan> plans = repayPlanRepo.findByBillNoOrderByTermNo(billNo);
        BigDecimal remainingAmt = repayAmt;

        // 冲销顺序：费-息-本
        for (ClsRepayPlan plan : plans) {
            if (remainingAmt.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            if ("S".equals(plan.getStatus())) {
                continue; // 已还清，跳过
            }

            // 1. 冲销费用
            BigDecimal feeToPay = plan.getFeeAmt().subtract(plan.getPaidFee());
            if (feeToPay.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal actualFee = remainingAmt.min(feeToPay);
                plan.setPaidFee(plan.getPaidFee().add(actualFee));
                remainingAmt = remainingAmt.subtract(actualFee);
                logger.info("冲销费用: {}", actualFee);
            }

            // 2. 冲销利息
            BigDecimal interestToPay = plan.getInterestAmt().subtract(plan.getPaidInterest());
            if (interestToPay.compareTo(BigDecimal.ZERO) > 0 && remainingAmt.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal actualInterest = remainingAmt.min(interestToPay);
                plan.setPaidInterest(plan.getPaidInterest().add(actualInterest));
                remainingAmt = remainingAmt.subtract(actualInterest);
                logger.info("冲销利息: {}", actualInterest);
            }

            // 3. 冲销本金
            BigDecimal prinToPay = plan.getPrinAmt().subtract(plan.getPaidPrin());
            if (prinToPay.compareTo(BigDecimal.ZERO) > 0 && remainingAmt.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal actualPrin = remainingAmt.min(prinToPay);
                plan.setPaidPrin(plan.getPaidPrin().add(actualPrin));
                remainingAmt = remainingAmt.subtract(actualPrin);
                logger.info("冲销本金: {}", actualPrin);
            }

            // 更新剩余应还
            BigDecimal paidTotal = plan.getPaidPrin().add(plan.getPaidInterest()).add(plan.getPaidFee());
            plan.setRemainAmt(plan.getTotalAmt().subtract(paidTotal));

            // 更新状态
            if (plan.getRemainAmt().compareTo(BigDecimal.ZERO) <= 0) {
                plan.setStatus("S"); // 已还清
            } else if (paidTotal.compareTo(BigDecimal.ZERO) > 0) {
                plan.setStatus("P"); // 部分还款
            }

            plan.setUpdateTime(new Date());
            em.merge(plan);
        }

        // 处理溢缴款
        if (remainingAmt.compareTo(BigDecimal.ZERO) > 0) {
            handleOverpayment(billNo, remainingAmt, orderId);
        }

        // 创建还款记录
        createRepayRecord(billNo, repayAmt, orderId, repayType);
    }

    /**
     * 处理溢缴款
     */
    private void handleOverpayment(String billNo, BigDecimal overpayAmt, String orderId) {
        logger.info("检测到溢缴款，billNo: {}, 金额: {}", billNo, overpayAmt);

        if (overpayAmt.compareTo(LARGE_OVERPAY_THRESHOLD) >= 0) {
            // 大额超额，记入溢缴款账户，用于下期抵扣
            logger.warn("大额溢缴款，记入溢缴款账户");
            // TODO: 创建溢缴款记录
        } else {
            // 小额，直接入账或提示
            logger.info("小额溢缴款，直接处理");
        }
    }

    /**
     * 创建还款记录
     */
    private void createRepayRecord(String billNo, BigDecimal repayAmt, String orderId, String repayType) {
        ClsRepayRecord record = new ClsRepayRecord();
        record.setBillNo(billNo);
        record.setOrderId(orderId);
        record.setRepayAmt(repayAmt);
        record.setRepayType(repayType);
        record.setPayStatus("S"); // 成功
        record.setPayTime(new Date());
        record.setAccountTime(new Date());
        record.setCreateTime(new Date());

        em.persist(record);
    }

    /**
     * 还款支付失败处理
     */
    @Transactional
    public void handleRepayPayFail(String orderId, String failReason) {
        logger.warn("还款支付失败，orderId: {}, 原因: {}", orderId, failReason);

        ClsOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 更新订单状态为失败
        order.setOrderStatus(String.valueOf(OrderStatus.F));
        order.setFailReason(failReason);
        order.setUpdateTime(new Date());
        em.merge(order);

        // 不生成账务数据
        logger.info("订单状态已更新为失败，未生成账务数据");
    }

    /**
     * 还款入账异常处理
     */
    @Transactional
    public void handleRepayAccountFail(String orderId, BigDecimal repayAmt, String failReason) {
        logger.error("还款入账异常，orderId: {}, 金额: {}, 原因: {}", orderId, repayAmt, failReason);

        ClsOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 订单状态保持为支付中
        order.setOrderStatus(String.valueOf(OrderStatus.J));
        order.setFailReason(failReason);
        order.setUpdateTime(new Date());
        em.merge(order);

        // 创建对账异常记录
        createReconException(orderId, order.getBillNo(), repayAmt, BigDecimal.ZERO, 
                "CORE_SUCCESS_PAY_FAIL", failReason);

        logger.warn("订单状态保持为支付中，已创建对账异常记录，等待对账处理");
    }

    /**
     * 批量扣款部分失败处理
     */
    @Transactional
    public void handleBatchRepayPartialFail(List<String> failedOrderIds, String failReason) {
        logger.warn("批量扣款部分失败，失败订单数: {}", failedOrderIds.size());

        for (String orderId : failedOrderIds) {
            try {
                ClsOrder order = orderRepo.findById(orderId).orElse(null);
                if (order == null) {
                    continue;
                }

                // 标记为处理异常
                order.setOrderStatus("E"); // Exception
                order.setFailReason(failReason);
                order.setUpdateTime(new Date());
                em.merge(order);

                // 创建对账异常记录
                createReconException(orderId, order.getBillNo(), order.getOrderAmt(), 
                        BigDecimal.ZERO, "BATCH_PARTIAL_FAIL", failReason);

                logger.info("订单 {} 已标记为异常，等待重试", orderId);

            } catch (Exception e) {
                logger.error("处理失败订单异常，orderId: {}", orderId, e);
            }
        }
    }

    /**
     * 重试失败的还款
     */
    @Transactional
    public void retryFailedRepayment(String orderId) {
        logger.info("重试还款，orderId: {}", orderId);

        ClsOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!OrderStatus.F.name().equals(order.getOrderStatus()) && 
            !"E".equals(order.getOrderStatus())) {
            throw new RuntimeException("只有失败或异常的订单才能重试");
        }

        // 重置订单状态
        order.setOrderStatus(String.valueOf(OrderStatus.U));
        order.setFailReason(null);
        order.setUpdateTime(new Date());
        em.merge(order);

        logger.info("订单状态已重置，可以重新发起支付");
    }

    /**
     * 创建对账异常记录
     */
    private void createReconException(String orderId, String billNo, BigDecimal payAmt, 
                                     BigDecimal coreAmt, String exceptionType, String remark) {
        ReconException exception = new ReconException();
        exception.setOrderId(orderId);
        exception.setBillNo(billNo);
        exception.setExceptionType(exceptionType);
        exception.setPayAmt(payAmt);
        exception.setCoreAmt(coreAmt);
        exception.setDiffAmt(payAmt.subtract(coreAmt));
        exception.setStatus("P"); // 待处理
        exception.setCreateTime(new Date());
        exception.setRemark(remark);

        em.persist(exception);
        logger.info("创建对账异常记录，exceptionId: {}", exception.getExceptionId());
    }
}
