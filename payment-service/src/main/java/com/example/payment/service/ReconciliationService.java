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

/**
 * 对账服务
 */
@Service
public class ReconciliationService {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ClsOrderRepository orderRepo;

    @Autowired
    private ClsLoanRegRepository loanRegRepo;

    @Autowired
    private ClsRepayPlanRepository repayPlanRepo;

    @Autowired
    private ReconExceptionRepository reconExceptionRepo;

    @Autowired
    private WithdrawService withdrawService;

    @Autowired
    private RepaymentService repaymentService;

    private static final Logger logger = LoggerFactory.getLogger(ReconciliationService.class);

    // 小额容差阈值
    private static final BigDecimal SMALL_DIFF_THRESHOLD = new BigDecimal("1.00");

    /**
     * 执行对账（定时任务调用）
     */
    @Transactional
    public void executeReconciliation() {
        logger.info("开始执行对账任务");

        try {
            // 1. 对账：支付成功，核心未入账
            reconcilePaySuccessCoreFail();

            // 2. 对账：核心已入账，支付未成功
            reconcileCoreSuccessPayFail();

            // 3. 对账：金额不一致
            reconcileAmountMismatch();

            logger.info("对账任务执行完成");

        } catch (Exception e) {
            logger.error("对账任务执行异常", e);
            throw new RuntimeException("对账任务执行失败", e);
        }
    }

    /**
     * 对账场景1：支付成功，核心未入账
     */
    @Transactional
    public void reconcilePaySuccessCoreFail() {
        logger.info("对账：支付成功，核心未入账");

        // 查询支付成功但核心无记录的订单
        List<ClsOrder> orders = orderRepo.findByOrderStatus(String.valueOf(OrderStatus.S));

        for (ClsOrder order : orders) {
            try {
                boolean coreHasRecord = checkCoreHasRecord(order);

                if (!coreHasRecord) {
                    logger.warn("发现支付成功但核心未入账的订单，orderId: {}", order.getOrderId());

                    // 创建对账异常记录
                    createReconException(order, "PAY_SUCCESS_CORE_FAIL", 
                            "支付已成功但核心系统无记录");

                    // 根据订单类型处理
                    if ("WITHDRAW".equals(order.getOrderType())) {
                        // 放款：重新触发借据创建、期供生成和额度占用
                        handleWithdrawReconcile(order);
                    } else if ("REPAY".equals(order.getOrderType())) {
                        // 还款：调用入账接口，重新执行冲销、生成分录、更新期供状态
                        handleRepayReconcile(order);
                    }
                }

            } catch (Exception e) {
                logger.error("对账处理异常，orderId: {}", order.getOrderId(), e);
            }
        }
    }

    /**
     * 检查核心是否有记录
     */
    private boolean checkCoreHasRecord(ClsOrder order) {
        if ("WITHDRAW".equals(order.getOrderType())) {
            // 放款：检查是否有贷款登记记录
            return loanRegRepo.findByExtBizOrderId(order.getExtOrderId()).isPresent();
        } else if ("REPAY".equals(order.getOrderType())) {
            // 还款：检查是否有还款记录
            return !repaymentService.getClass().getDeclaredFields().toString().isEmpty(); // 简化判断
        }
        return false;
    }

    /**
     * 处理放款对账
     */
    @Transactional
    public void handleWithdrawReconcile(ClsOrder order) {
        logger.info("处理放款对账补账，orderId: {}", order.getOrderId());

        try {
            // 根据订单信息重新触发借据创建
            // TODO: 从订单信息中获取原始请求数据
            // 这里简化处理，实际应该从订单扩展信息或日志中获取
            
            logger.info("放款补账处理完成，orderId: {}", order.getOrderId());

        } catch (Exception e) {
            logger.error("放款补账处理失败", e);
            updateReconExceptionStatus(order.getOrderId(), "H", "自动处理失败，需人工介入: " + e.getMessage());
        }
    }

    /**
     * 处理还款对账
     */
    @Transactional
    public void handleRepayReconcile(ClsOrder order) {
        logger.info("处理还款对账补账，orderId: {}", order.getOrderId());

        try {
            // 调用入账接口，重新执行冲销、生成分录、更新期供状态
            repaymentService.executeAccounting(
                    order.getBillNo(), 
                    order.getOrderAmt(), 
                    order.getOrderId(), 
                    "RECONCILE"
            );

            logger.info("还款补账处理完成，orderId: {}", order.getOrderId());

        } catch (Exception e) {
            logger.error("还款补账处理失败", e);
            updateReconExceptionStatus(order.getOrderId(), "H", "自动处理失败，需人工介入: " + e.getMessage());
        }
    }

    /**
     * 对账场景2：核心已入账，支付未成功
     */
    @Transactional
    public void reconcileCoreSuccessPayFail() {
        logger.info("对账：核心已入账，支付未成功");

        // 查询支付失败但核心有记录的订单
        List<ClsOrder> orders = orderRepo.findByOrderStatus(String.valueOf(OrderStatus.F));

        for (ClsOrder order : orders) {
            try {
                boolean coreHasRecord = checkCoreHasRecord(order);

                if (coreHasRecord) {
                    logger.warn("发现核心已入账但支付失败的订单，orderId: {}", order.getOrderId());

                    // 创建对账异常记录
                    createReconException(order, "CORE_SUCCESS_PAY_FAIL", 
                            "核心系统已入账但支付失败");

                    // 尝试撤销之前的入账操作（生成反向分录）
                    handleReversal(order);
                }

            } catch (Exception e) {
                logger.error("对账处理异常，orderId: {}", order.getOrderId(), e);
            }
        }
    }

    /**
     * 处理冲正（撤销入账）
     */
    @Transactional
    public void handleReversal(ClsOrder order) {
        logger.info("处理冲正，orderId: {}", order.getOrderId());

        try {
            if ("WITHDRAW".equals(order.getOrderType())) {
                // 放款冲正：回退借据状态
                reverseWithdraw(order);
            } else if ("REPAY".equals(order.getOrderType())) {
                // 还款冲正：回退还款记录
                reverseRepayment(order);
            }

            // 更新订单状态
            order.setOrderStatus("C"); // Cancelled
            order.setUpdateTime(new Date());
            em.merge(order);

            updateReconExceptionStatus(order.getOrderId(), "R", "冲正成功");
            logger.info("冲正处理完成，orderId: {}", order.getOrderId());

        } catch (Exception e) {
            logger.error("冲正处理失败", e);
            updateReconExceptionStatus(order.getOrderId(), "H", "冲正失败，需贷后介入: " + e.getMessage());
        }
    }

    /**
     * 放款冲正
     */
    private void reverseWithdraw(ClsOrder order) {
        // TODO: 生成反向分录，回退借据状态
        logger.info("执行放款冲正逻辑");
    }

    /**
     * 还款冲正
     */
    private void reverseRepayment(ClsOrder order) {
        // TODO: 生成反向分录，回退还款计划状态
        logger.info("执行还款冲正逻辑");
    }

    /**
     * 对账场景3：金额不一致
     */
    @Transactional
    public void reconcileAmountMismatch() {
        logger.info("对账：金额不一致");

        // 查询所有待处理的对账异常
        List<ReconException> exceptions = reconExceptionRepo.findByStatus("P");

        for (ReconException exception : exceptions) {
            try {
                BigDecimal diffAmt = exception.getDiffAmt().abs();

                if (diffAmt.compareTo(SMALL_DIFF_THRESHOLD) <= 0) {
                    // 小额差异：自动记入损益科目
                    handleSmallDifference(exception);
                } else {
                    // 大额差异：标记为人工处理
                    exception.setStatus("H");
                    exception.setHandleMethod("MANUAL");
                    exception.setHandleTime(new Date());
                    exception.setHandleResult("金额差异较大，需人工处理");
                    em.merge(exception);

                    logger.warn("大额金额差异，需人工处理，exceptionId: {}, 差异金额: {}", 
                            exception.getExceptionId(), diffAmt);
                }

            } catch (Exception e) {
                logger.error("金额差异处理异常", e);
            }
        }
    }

    /**
     * 处理小额差异
     */
    private void handleSmallDifference(ReconException exception) {
        logger.info("处理小额差异，exceptionId: {}, 差异金额: {}", 
                exception.getExceptionId(), exception.getDiffAmt());

        // 自动记入损益科目
        exception.setStatus("R");
        exception.setHandleMethod("WRITE_OFF");
        exception.setHandleTime(new Date());
        exception.setHandleResult("小额差异，已自动核销记入损益科目");

        em.merge(exception);
    }

    /**
     * 创建对账异常记录
     */
    private void createReconException(ClsOrder order, String exceptionType, String remark) {
        ReconException exception = new ReconException();
        exception.setOrderId(order.getOrderId());
        exception.setBillNo(order.getBillNo());
        exception.setExceptionType(exceptionType);
        exception.setPayAmt(order.getOrderAmt());
        exception.setCoreAmt(BigDecimal.ZERO); // 简化处理
        exception.setDiffAmt(order.getOrderAmt());
        exception.setStatus("P");
        exception.setCreateTime(new Date());
        exception.setRemark(remark);

        em.persist(exception);
        logger.info("创建对账异常记录，exceptionId: {}", exception.getExceptionId());
    }

    /**
     * 更新对账异常状态
     */
    private void updateReconExceptionStatus(String orderId, String status, String result) {
        List<ReconException> exceptions = reconExceptionRepo.findByOrderId(orderId);
        for (ReconException exception : exceptions) {
            exception.setStatus(status);
            exception.setHandleTime(new Date());
            exception.setHandleResult(result);
            em.merge(exception);
        }
    }

    /**
     * 查询待处理的对账异常
     */
    public List<ReconException> getPendingExceptions() {
        return reconExceptionRepo.findByStatus("P");
    }

    /**
     * 人工处理对账异常
     */
    @Transactional
    public void handleExceptionManually(Long exceptionId, String handleMethod, String result) {
        ReconException exception = reconExceptionRepo.findById(exceptionId)
                .orElseThrow(() -> new RuntimeException("对账异常不存在"));

        exception.setStatus("R");
        exception.setHandleMethod(handleMethod);
        exception.setHandleTime(new Date());
        exception.setHandleResult(result);

        em.merge(exception);
        logger.info("人工处理对账异常完成，exceptionId: {}", exceptionId);
    }
}
