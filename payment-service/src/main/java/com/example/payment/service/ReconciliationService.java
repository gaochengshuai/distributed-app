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
import org.springframework.transaction.annotation.Propagation;
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
    private ClsLoanEventRepository eventRepo;

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
     * 注意：每个订单的处理使用独立事务，异常记录在外层管理
     */
    public void reconcilePaySuccessCoreFail() {
        logger.info("对账：支付成功，核心未入账");

        // 查询支付成功但核心无记录的订单
        List<ClsOrder> orders = orderRepo.findByOrderStatus(String.valueOf(OrderStatus.S));

        for (ClsOrder order : orders) {
            try {
                boolean coreHasRecord = checkCoreHasRecord(order);

                if (!coreHasRecord) {
                    logger.warn("发现支付成功但核心未入账的订单，orderId: {}", order.getOrderId());

                    // 创建对账异常记录（独立事务）
                    ReconException exception = createReconExceptionInOuterTransaction(order, "PAY_SUCCESS_CORE_FAIL", 
                            "支付已成功但核心系统无记录");

                    try {
                        // 每个订单使用独立事务处理业务逻辑
                        processSingleOrderReconciliation(order);
                        
                        // 成功后更新异常状态（独立事务）
                        updateReconExceptionStatusAfterSuccess(order.getOrderId(), "自动补账成功");
                        
                    } catch (Exception e) {
                        logger.error("对账处理异常，orderId: {}", order.getOrderId(), e);
                        
                        // 更新异常状态为失败（独立事务）
                        updateReconExceptionStatusInOuterTransaction(
                            exception.getExceptionId(), "H", "自动处理失败: " + e.getMessage());
                    }
                    
                    logger.info("订单对账完成，orderId: {}", order.getOrderId());
                }

            } catch (Exception e) {
                // 外层异常：checkCoreHasRecord 或 createReconException 失败
                logger.error("对账前置检查异常，orderId: {}", order.getOrderId(), e);
            }
        }
        
        logger.info("对账：支付成功核心未入账处理完成");
    }

    /**
     * 检查核心是否有记录
     * 
     * @param order 订单信息
     * @return true-核心已有记录，false-核心无记录（需要对账补账）
     */
    private boolean checkCoreHasRecord(ClsOrder order) {
        if ("WITHDRAW".equals(order.getOrderType())) {
            // 放款：通过 orderId 检查是否有贷款登记记录
            // 注意：不使用 extOrderId，因为对账时订单可能没有外部订单号
            return loanRegRepo.findByOrderId(order.getOrderId()).isPresent();
        } else if ("REPAY".equals(order.getOrderType())) {
            // 还款：通过 billNo 检查是否有还款记录
            // TODO: 实现还款记录的查询逻辑
            logger.warn("还款对账检查逻辑待完善，orderId: {}", order.getOrderId());
            return false; // 暂时返回 false，触发对账处理
        }
        return false;
    }

    /**
     * 在外层事务中创建对账异常记录
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReconException createReconExceptionInOuterTransaction(ClsOrder order, String exceptionType, String remark) {
        ReconException exception = new ReconException();
        exception.setOrderId(order.getOrderId());
        exception.setBillNo(order.getBillNo());
        exception.setCustId(null);
        exception.setExceptionType(exceptionType);
        exception.setPayAmt(order.getOrderAmt());
        exception.setCoreAmt(BigDecimal.ZERO);
        exception.setDiffAmt(order.getOrderAmt());
        exception.setStatus("P");
        exception.setCreateTime(new Date());
        exception.setRemark(remark);

        em.persist(exception);
        em.flush(); // 立即刷新，确保ID生成
        
        logger.info("创建对账异常记录，exceptionId: {}, orderId: {}", exception.getExceptionId(), order.getOrderId());
        return exception;
    }

    /**
     * 处理单个订单的对账（独立事务）
     * 注意：外层已确保只有需要处理的订单才会调用此方法
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleOrderReconciliation(ClsOrder order) {
        logger.info("开始处理单个订单对账，orderId: {}", order.getOrderId());

        try {
            // 根据订单类型处理（外层已确认需要处理，无需再次检查）
            if ("WITHDRAW".equals(order.getOrderType())) {
                // 放款：重新触发借据创建、期供生成和额度占用
                handleWithdrawReconcile(order);
                // 成功后更新对账异常状态（独立事务）
                updateReconExceptionStatusAfterSuccess(order.getOrderId(), "自动补账成功");
                
            } else if ("REPAY".equals(order.getOrderType())) {
                // 还款：调用入账接口，重新执行冲销、生成分录、更新期供状态
                handleRepayReconcile(order);
                // 成功后更新对账异常状态（独立事务）
                updateReconExceptionStatusAfterSuccess(order.getOrderId(), "自动补账成功");
            }
            
            logger.info("订单对账处理成功，orderId: {}", order.getOrderId());
            
        } catch (Exception e) {
            logger.error("单个订单对账处理失败，orderId: {}", order.getOrderId(), e);
            
            // 不更新异常状态，由外层方法处理
            throw new RuntimeException("订单对账处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理放款对账
     * 场景：支付成功，但核心系统未生成借据和还款计划
     */
    @Transactional
    public void handleWithdrawReconcile(ClsOrder order) {
        logger.info("处理放款对账补账，orderId: {}", order.getOrderId());

        try {
            // Step 1: 查找关联的贷款登记记录
            ClsLoanReg loanReg = loanRegRepo.findByOrderId(order.getOrderId())
                    .orElseThrow(() -> new RuntimeException("未找到关联的贷款登记记录，orderId: " + order.getOrderId()));

            logger.info("找到贷款登记记录，loanRegId: {}, contrNo: {}, billNo: {}", 
                loanReg.getLoanRegId(), loanReg.getContrNo(), loanReg.getBillNo());

            // Step 2: 检查是否已经生成借据号
            if (loanReg.getBillNo() != null && !loanReg.getBillNo().isEmpty()) {
                logger.warn("借据号已存在，无需补账，loanRegId: {}, billNo: {}", 
                    loanReg.getLoanRegId(), loanReg.getBillNo());
                updateReconExceptionStatus(order.getOrderId(), "H", "借据已存在，无需补账");
                return;
            }

            // Step 3: 生成借据号
            String billNo = generateBillNoForReconcile();
            loanReg.setBillNo(billNo);
            em.merge(loanReg);
            
            logger.info("生成借据号成功，billNo: {}", billNo);

            // Step 4: 更新订单的借据号
            order.setBillNo(billNo);
            order.setUpdateTime(new Date());
            em.merge(order);

            // Step 5: 创建还款计划
            createRepayPlanForReconcile(loanReg);
            
            logger.info("创建还款计划成功，billNo: {}", billNo);

            // Step 6: 记录对账事件
            recordReconEvent(loanReg, "RECON_WITHDRAW_SUCCESS", 
                "对账补账：支付成功后生成借据和还款计划，billNo: " + billNo);

            // Step 7: 更新对账异常状态为已处理
            updateReconExceptionStatus(order.getOrderId(), "H", "自动补账成功，已生成借据和还款计划");

            logger.info("放款补账处理完成，orderId: {}, billNo: {}", order.getOrderId(), billNo);

        } catch (Exception e) {
            logger.error("放款补账处理失败，orderId: {}", order.getOrderId(), e);
            updateReconExceptionStatus(order.getOrderId(), "H", "自动处理失败，需人工介入: " + e.getMessage());
            throw new RuntimeException("放款补账处理失败", e);
        }
    }

    /**
     * 为对账补账生成借据号
     */
    private String generateBillNoForReconcile() {
        return "BILL_RECON_" + System.currentTimeMillis();
    }

    /**
     * 为对账补账创建还款计划
     */
    private void createRepayPlanForReconcile(ClsLoanReg loanReg) {
        logger.info("开始创建还款计划，loanRegId: {}, loanPrin: {}", 
            loanReg.getLoanRegId(), loanReg.getLoanPrin());
        
        // TODO: 从产品配置或贷款登记中获取期数，这里默认12期
        int termCount = 12;
        BigDecimal totalPrin = loanReg.getLoanPrin();
        BigDecimal prinPerTerm = totalPrin.divide(new BigDecimal(termCount), 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal feePerTerm = loanReg.getTxnFeeAmt().divide(new BigDecimal(termCount), 2, BigDecimal.ROUND_HALF_UP);
        
        Date startDate = new Date();
        
        for (int i = 1; i <= termCount; i++) {
            ClsRepayPlan plan = new ClsRepayPlan();
            plan.setBillNo(loanReg.getBillNo());
            plan.setContrNo(loanReg.getContrNo());
            plan.setCustId(loanReg.getCustId());
            plan.setTermNo(i);
            plan.setDueDate(addMonths(startDate, i));
            plan.setPrinAmt(prinPerTerm);
            plan.setFeeAmt(feePerTerm);
            plan.setStatus("U"); // 未还
            plan.setCreateTime(new Date());
            plan.setUpdateTime(new Date());
            
            em.persist(plan);
        }
        
        logger.info("还款计划创建完成，共{}期", termCount);
    }

    /**
     * 日期增加月份
     */
    private Date addMonths(Date date, int months) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(date);
        cal.add(java.util.Calendar.MONTH, months);
        return cal.getTime();
    }

    /**
     * 记录对账事件
     */
    private void recordReconEvent(ClsLoanReg loanReg, String eventType, String memo) {
        try {
            ClsLoanEvent event = new ClsLoanEvent();
            event.setCustId(loanReg.getCustId());
            event.setContrNo(loanReg.getContrNo());
            event.setBillNo(loanReg.getBillNo());
            event.setLoanEventType(eventType);
            event.setMemo(memo);
            event.setSetupTime(new Date());
            event.setOptUserName("SYSTEM_RECON");
            event.setCreateTime(new Date());
            event.setCreateUser("SYSTEM");
            event.setJpaVersion(0L);
            
            em.persist(event);
            logger.info("对账事件记录成功，eventType: {}", eventType);
        } catch (Exception e) {
            logger.error("记录对账事件失败", e);
            // 不抛出异常，避免影响主流程
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
     * 注意：每个订单的处理使用独立事务，异常记录在外层管理
     */
    public void reconcileCoreSuccessPayFail() {
        logger.info("对账：核心已入账，支付未成功");

        // 查询支付失败但核心有记录的订单
        List<ClsOrder> orders = orderRepo.findByOrderStatus(String.valueOf(OrderStatus.F));

        for (ClsOrder order : orders) {
            try {
                boolean coreHasRecord = checkCoreHasRecord(order);

                if (coreHasRecord) {
                    logger.warn("发现核心已入账但支付失败的订单，orderId: {}", order.getOrderId());

                    // 创建对账异常记录（独立事务）
                    ReconException exception = createReconExceptionInOuterTransaction(order, "CORE_SUCCESS_PAY_FAIL", 
                            "核心系统已入账但支付失败");

                    try {
                        // 每个订单使用独立事务处理冲正逻辑
                        processSingleOrderReversal(order);
                        
                        // 成功后更新异常状态（独立事务）
                        updateReconExceptionStatusAfterSuccess(order.getOrderId(), "冲正成功");
                        
                    } catch (Exception e) {
                        logger.error("冲正处理异常，orderId: {}", order.getOrderId(), e);
                        
                        // 更新异常状态为失败（独立事务）
                        // 注意：如果更新失败，异常会被外层 catch 捕获并记录
                        updateReconExceptionStatusInOuterTransaction(
                            exception.getExceptionId(), "H", "冲正失败: " + e.getMessage());
                    }
                    
                    logger.info("订单冲正完成，orderId: {}", order.getOrderId());
                }

            } catch (Exception e) {
                // 外层异常：checkCoreHasRecord 或 createReconException 失败
                logger.error("冲正前置检查异常，orderId: {}", order.getOrderId(), e);
            }
        }
        
        logger.info("对账：核心已入账支付未成功处理完成");
    }

    /**
     * 处理单个订单的冲正（独立事务）
     * 注意：外层已确保只有需要处理的订单才会调用此方法
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleOrderReversal(ClsOrder order) {
        logger.info("开始处理单个订单冲正，orderId: {}", order.getOrderId());

        try {
            // 尝试撤销之前的入账操作（生成反向分录）
            // 外层已确认核心有记录，无需再次检查
            handleReversal(order);
            // 成功后更新对账异常状态（独立事务）
            updateReconExceptionStatusAfterSuccess(order.getOrderId(), "冲正成功");
            
            logger.info("订单冲正处理成功，orderId: {}", order.getOrderId());
            
        } catch (Exception e) {
            logger.error("单个订单冲正处理失败，orderId: {}", order.getOrderId(), e);
            throw new RuntimeException("订单冲正处理失败: " + e.getMessage(), e);
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
     * 场景：核心已生成借据和还款计划，但支付失败
     * 操作：回退借据状态、删除还款计划、记录冲正事件
     */
    private void reverseWithdraw(ClsOrder order) {
        logger.info("执行放款冲正逻辑，orderId: {}", order.getOrderId());

        // Step 1: 查找贷款登记记录
        ClsLoanReg loanReg = loanRegRepo.findByOrderId(order.getOrderId())
                .orElseThrow(() -> new RuntimeException("未找到关联的贷款登记记录，orderId: " + order.getOrderId()));

        logger.info("找到贷款登记，loanRegId: {}, billNo: {}", 
            loanReg.getLoanRegId(), loanReg.getBillNo());

        // Step 2: 检查是否有借据号
        if (loanReg.getBillNo() == null || loanReg.getBillNo().isEmpty()) {
            logger.warn("借据号为空，无需冲正，loanRegId: {}", loanReg.getLoanRegId());
            return;
        }

        String billNo = loanReg.getBillNo();

        // Step 3: 删除还款计划（如果存在）
        List<ClsRepayPlan> plans = repayPlanRepo.findByBillNoOrderByTermNo(billNo);
        if (!plans.isEmpty()) {
            logger.info("删除还款计划，billNo: {}, 共{}期", billNo, plans.size());
            for (ClsRepayPlan plan : plans) {
                em.remove(plan);
            }
        }

        // Step 4: 回退借据状态
        loanReg.setBillNo(null);  // 清空借据号
        loanReg.setAuditResult("C");  // 审核结果改为已冲正
        loanReg.setLoanRegStatus("C");  // 贷款状态改为已冲正
        loanReg.setUpdateTime(new Date());
        em.merge(loanReg);

        logger.info("借据状态已回退，billNo: {}", billNo);

        // Step 5: 记录冲正事件
        recordReconEvent(loanReg, "RECON_WITHDRAW_REVERSAL", 
            "对账冲正：支付失败回退借据，原借据号: " + billNo);

        logger.info("放款冲正完成，orderId: {}, billNo: {}", order.getOrderId(), billNo);
    }

    /**
     * 还款冲正
     * 场景：核心已入账还款，但支付失败
     * 操作：回退还款计划状态、删除还款记录、记录冲正事件
     */
    private void reverseRepayment(ClsOrder order) {
        logger.info("执行还款冲正逻辑，orderId: {}", order.getOrderId());

        // Step 1: 检查订单是否有借据号
        if (order.getBillNo() == null || order.getBillNo().isEmpty()) {
            throw new RuntimeException("订单借据号为空，无法执行冲正");
        }

        String billNo = order.getBillNo();
        BigDecimal repayAmt = order.getOrderAmt();

        logger.info("开始还款冲正，billNo: {}, repayAmt: {}", billNo, repayAmt);

        // Step 2: 查找并删除还款记录
        List<ClsRepayRecord> records = repaymentService.findRepayRecordsByOrderId(order.getOrderId());
        if (!records.isEmpty()) {
            logger.info("删除还款记录，共{}条", records.size());
            for (ClsRepayRecord record : records) {
                em.remove(record);
            }
        }

        // Step 3: 回退还款计划状态
        List<ClsRepayPlan> plans = repayPlanRepo.findByBillNoOrderByTermNo(billNo);
        if (plans.isEmpty()) {
            logger.warn("未找到还款计划，billNo: {}", billNo);
            return;
        }

        // 按到期日排序，从最早到最晚冲销
        plans.sort((p1, p2) -> p1.getDueDate().compareTo(p2.getDueDate()));

        BigDecimal remainingAmt = repayAmt;
        int reversedCount = 0;

        for (ClsRepayPlan plan : plans) {
            if (remainingAmt.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            // 只回退已还或部分还款的计划
            if ("P".equals(plan.getStatus()) || "S".equals(plan.getStatus())) {
                // 计算该期已还总额
                BigDecimal paidPrin = plan.getPaidPrin() != null ? plan.getPaidPrin() : BigDecimal.ZERO;
                BigDecimal paidInterest = plan.getPaidInterest() != null ? plan.getPaidInterest() : BigDecimal.ZERO;
                BigDecimal paidFee = plan.getPaidFee() != null ? plan.getPaidFee() : BigDecimal.ZERO;
                BigDecimal totalPaid = paidPrin.add(paidInterest).add(paidFee);
                
                if (totalPaid.compareTo(BigDecimal.ZERO) <= 0) {
                    // 该期实际上未还款，跳过
                    continue;
                }
                
                // 计算该期应还总额
                BigDecimal prinAmt = plan.getPrinAmt() != null ? plan.getPrinAmt() : BigDecimal.ZERO;
                BigDecimal interestAmt = plan.getInterestAmt() != null ? plan.getInterestAmt() : BigDecimal.ZERO;
                BigDecimal feeAmt = plan.getFeeAmt() != null ? plan.getFeeAmt() : BigDecimal.ZERO;
                BigDecimal totalDue = prinAmt.add(interestAmt).add(feeAmt);
                
                if (remainingAmt.compareTo(totalPaid) >= 0) {
                    // 完全冲销该期
                    plan.setPaidPrin(BigDecimal.ZERO);
                    plan.setPaidInterest(BigDecimal.ZERO);
                    plan.setPaidFee(BigDecimal.ZERO);
                    plan.setRemainAmt(totalDue);
                    plan.setStatus("U");  // 改回未还
                    remainingAmt = remainingAmt.subtract(totalPaid);
                    reversedCount++;
                    logger.info("完全冲销第{}期，已还总额: {}", plan.getTermNo(), totalPaid);
                } else {
                    // 部分冲销：按比例回退已还金额
                    BigDecimal ratio = remainingAmt.divide(totalPaid, 6, BigDecimal.ROUND_HALF_UP);
                    
                    BigDecimal revertPrin = paidPrin.multiply(ratio).setScale(2, BigDecimal.ROUND_HALF_UP);
                    BigDecimal revertInterest = paidInterest.multiply(ratio).setScale(2, BigDecimal.ROUND_HALF_UP);
                    BigDecimal revertFee = paidFee.multiply(ratio).setScale(2, BigDecimal.ROUND_HALF_UP);
                    
                    // 更新已还金额
                    plan.setPaidPrin(paidPrin.subtract(revertPrin));
                    plan.setPaidInterest(paidInterest.subtract(revertInterest));
                    plan.setPaidFee(paidFee.subtract(revertFee));
                    
                    // 重新计算剩余金额
                    BigDecimal newTotalPaid = plan.getPaidPrin().add(plan.getPaidInterest()).add(plan.getPaidFee());
                    plan.setRemainAmt(totalDue.subtract(newTotalPaid));
                    
                    // 更新状态
                    if (newTotalPaid.compareTo(BigDecimal.ZERO) <= 0) {
                        plan.setStatus("U");  // 改回未还
                    } else if (newTotalPaid.compareTo(totalDue) >= 0) {
                        plan.setStatus("S");  // 保持已还清
                    } else {
                        plan.setStatus("P");  // 保持部分还款
                    }
                    
                    logger.info("部分冲销第{}期，冲销金额: {}, 剩余已还: {}", 
                        plan.getTermNo(), remainingAmt, newTotalPaid);
                    remainingAmt = BigDecimal.ZERO;
                    reversedCount++;
                    break;
                }
                
                plan.setUpdateTime(new Date());
                em.merge(plan);
            }
        }

        logger.info("还款计划状态已回退，共冲销{}期", reversedCount);

        // Step 4: 记录冲正事件
        try {
            ClsLoanReg loanReg = loanRegRepo.findByBillNo(billNo).orElse(null);
            if (loanReg != null) {
                recordReconEvent(loanReg, "RECON_REPAY_REVERSAL", 
                    "对账冲正：支付失败回退还款，冲销金额: " + repayAmt + ", 冲销期数: " + reversedCount);
            }
        } catch (Exception e) {
            logger.error("记录冲正事件失败", e);
        }

        logger.info("还款冲正完成，orderId: {}, billNo: {}", order.getOrderId(), billNo);
    }

    /**
     * 对账场景3：金额不一致
     * 注意：每个异常记录的处理使用独立事务
     */
    public void reconcileAmountMismatch() {
        logger.info("对账：金额不一致");

        // 查询所有待处理的对账异常
        List<ReconException> exceptions = reconExceptionRepo.findByStatus("P");

        for (ReconException exception : exceptions) {
            try {
                // 每个异常记录使用独立事务处理
                processSingleExceptionReconciliation(exception);
            } catch (Exception e) {
                logger.error("金额差异处理异常，exceptionId: {}", exception.getExceptionId(), e);
            }
        }
        
        logger.info("对账：金额不一致处理完成");
    }

    /**
     * 处理单个对账异常（独立事务）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleExceptionReconciliation(ReconException exception) {
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
            logger.error("单个对账异常处理失败，exceptionId: {}", exception.getExceptionId(), e);
            throw e;
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
        // 如果是放款的话，这里从订单中取；还款是从交易流水之和
        exception.setCoreAmt(BigDecimal.ZERO); // 简化处理
        // 这里金额是渠道金额-核心金额
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
     * 对账成功后更新异常状态（独立事务）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateReconExceptionStatusAfterSuccess(String orderId, String result) {
        try {
            List<ReconException> exceptions = reconExceptionRepo.findByOrderId(orderId);
            for (ReconException exception : exceptions) {
                if ("P".equals(exception.getStatus())) {
                    exception.setStatus("R");  // R - Resolved 已解决
                    exception.setHandleMethod("AUTO");
                    exception.setHandleTime(new Date());
                    exception.setHandleResult(result);
                    em.merge(exception);
                    logger.info("更新对账异常状态为已解决，orderId: {}", orderId);
                }
            }
        } catch (Exception e) {
            logger.error("更新对账异常状态失败，orderId: {}", orderId, e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 在外层事务中更新对账异常状态
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateReconExceptionStatusInOuterTransaction(Long exceptionId, String status, String result) {
        ReconException exception = reconExceptionRepo.findById(exceptionId).orElse(null);
        if (exception != null) {
            exception.setStatus(status);
            exception.setHandleMethod("AUTO");
            exception.setHandleTime(new Date());
            exception.setHandleResult(result);
            em.merge(exception);
            logger.info("更新对账异常状态，exceptionId: {}, status: {}", exceptionId, status);
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
