package com.example.payment.service;

import com.example.payment.entity.*;
import com.example.payment.entity.req.WithdrawReq;
import com.example.payment.enums.OrderStatus;
import com.example.payment.enums.OrderType;
import com.example.payment.enums.RepayMethod;
import com.example.payment.repository.ClsLoanRegRepository;
import com.example.payment.repository.ClsOrderRepository;
import com.example.payment.repository.ClsLoanEventRepository;
import com.example.payment.repository.ClsRepayPlanRepository;
import com.example.payment.repository.ReconExceptionRepository;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class WithdrawService {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ClsLoanRegRepository loanRegRepo;

    @Autowired
    private ClsOrderRepository orderRepo;

    @Autowired
    private ClsLoanEventRepository eventRepo;

    @Autowired
    private ClsRepayPlanRepository repayPlanRepo;

    @Autowired
    private ReconExceptionRepository reconExceptionRepo;

    private static Logger logger = LoggerFactory.getLogger(WithdrawService.class);

    /**
     * 处理提款业务（完整流程）
     *
     * @param wd      提款请求
     * @param cust    客户信息
     * @param info    贷款信息
     * @param isTrial 是否试算
     * @return 订单ID
     */
    @Transactional
    public String processWithdrawMode(WithdrawReq wd, CustInfo cust, LoanInfo info, boolean isTrial) {

        logger.info("开始处理提款业务，外部订单号: {}, 金额: {}", wd.getExBizOrderId(), wd.getWithdrawAmt());

        try {
            // 【关键步骤0】幂等性校验 - 防止重复提交
            Optional<ClsLoanReg> existingReg = loanRegRepo.findByExtBizOrderId(wd.getExBizOrderId());
            if (existingReg.isPresent()) {
                ClsLoanReg existing = existingReg.get();
                logger.warn("检测到重复提款申请，extBizOrderId: {}, 已有记录ID: {}, 状态: {}", 
                    wd.getExBizOrderId(), existing.getLoanRegId(), existing.getLoanRegStatus());
                
                // 如果已有订单，直接返回
                if (StringUtils.isNotBlank(existing.getOrderId())) {
                    logger.info("返回已存在的订单ID: {}", existing.getOrderId());
                    return existing.getOrderId();
                }
                
                // 如果没有订单但有待审核记录，返回贷款登记ID（需要前端提示用户等待审核）
                throw new RuntimeException("该外部订单号已存在，贷款登记ID: " + existing.getLoanRegId() 
                    + "，状态: " + existing.getLoanRegStatus() + "，请勿重复提交");
            }

            // 【关键步骤1】判断是否需要人工审核
            String needAudit = checkNeedAudit(wd);
            logger.info("审核结果: {}", needAudit);

            // 【关键步骤2】计算提款手续费
            BigDecimal withdrawTxnFee = this.genWithdrawFee(wd, info);
            logger.info("提款手续费: {}", withdrawTxnFee);

            // 【关键步骤3】创建贷款登记记录（先生成 loanRegId）
            ClsLoanReg loanReg = this.newLoanReg(wd, withdrawTxnFee, info);

            // 【关键步骤4】借据定价（使用 loanReg 的 ID）
            ClsLoanDef loanDef = this.newLoanDef(wd, cust, info, loanReg);

            // 如果需要审核，设置审核状态
            if ("Y".equals(needAudit)) {
                loanReg.setAuditResult("U"); // 待审核
                loanReg.setLoanRegStatus("U"); // 待审核
                logger.info("需要人工审核，贷款登记状态设置为待审核");
            } else {
                loanReg.setAuditResult("A"); // 自动通过
                loanReg.setLoanRegStatus("A"); // 审核通过
            }

            // 【关键步骤5】持久化定价信息
            if (!isTrial) {
                saveLoanRegLoanDef(info, loanReg, loanDef);
                logger.info("贷款登记和定价信息已持久化，loanRegId: {}", loanReg.getLoanRegId());
            } else {
                logger.info("试算模式，不持久化数据");
                return null;
            }

            // 【关键步骤6】根据放款模式分支处理
            String orderId = null;
            switch (info.getProduct().getWithdrawMode()) {
                case L:  // 放款到客户
                    // ✅ 无论是否需要审核，都创建订单
                    ClsOrder order = createWithdrawOrder(wd, loanReg, info, needAudit);
                    orderId = order.getOrderId();
                    info.setOrder(order);
                    loanReg.setOrderId(orderId);
                    em.merge(loanReg);
                    logger.info("创建提款订单成功，orderId: {}, needAudit: {}", orderId, needAudit);
                    
                    if (!"Y".equals(needAudit)) {
                        // 无需审核时直接生成借据号和还款计划
                        String billNo = generateBillNo();
                        loanReg.setBillNo(billNo);
                        em.merge(loanReg);
                        
                        // 更新订单的借据号并激活
                        order.setBillNo(billNo);
                        order.setAuditStatus("N"); // 无需审核
                        order.setUpdateTime(new Date());
                        em.merge(order);
                        
                        // 创建还款计划
                        createRepayPlan(loanReg);
                        
                        logger.info("自动生成借据和还款计划，billNo: {}", billNo);
                    } else {
                        // 需要审核，订单状态保持为待审核
                        order.setAuditStatus("U"); // 待审核
                        order.setNeedAudit("Y");
                        order.setUpdateTime(new Date());
                        em.merge(order);
                        logger.info("订单已创建，等待人工审核，orderId: {}", orderId);
                    }
                    break;

                case M:  // 结算到商户
                    // TODO: 实现商户结算逻辑
                    logger.warn("商户结算模式暂未实现");
                    break;

                default:
                    logger.warn("未知的放款模式: {}", info.getProduct().getWithdrawMode());
            }

            // 【关键步骤7】记录事件
            if (orderId != null) {
                recordLoanEvent(loanReg, "WITHDRAW_CREATE", "提款申请创建成功");
            }

            logger.info("提款业务处理完成，orderId: {}", orderId);
            return orderId;

        } catch (Exception e) {
            logger.error("提款业务处理异常", e);
            throw new RuntimeException("提款业务处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查是否需要人工审核
     * @param wd 提款请求
     * @return "Y" 需要审核, "N" 自动通过
     */
    private String checkNeedAudit(WithdrawReq wd) {
        // 业务规则：金额超过5000需要人工审核
        BigDecimal threshold = new BigDecimal("5000");
        if (wd.getWithdrawAmt().compareTo(threshold) > 0) {
            logger.info("提款金额 {} 超过阈值 {}，需要人工审核", wd.getWithdrawAmt(), threshold);
            return "Y";
        }
        logger.info("提款金额 {} 未超过阈值 {}，自动审核通过", wd.getWithdrawAmt(), threshold);
        return "N";
    }

    /**
     * 创建提款订单
     * @param wd 提款请求
     * @param loanReg 贷款登记
     * @param info 贷款信息（包含产品、合同、银行卡等）
     * @param needAudit 是否需要人工审核
     */
    private ClsOrder createWithdrawOrder(WithdrawReq wd, ClsLoanReg loanReg, LoanInfo info, String needAudit) {
        ClsOrder order = new ClsOrder();
        order.setOrderId(generateOrderId());
        order.setContrNo(info.getClsContr().getContrNo());
        order.setBillNo(""); // 审核后生成借据号
        order.setOrderType(String.valueOf(OrderType.WITHDRAW));

        // 实际到账金额 = 提款金额 - 手续费
        BigDecimal actualAmt = loanReg.getLendingAmt().subtract(loanReg.getTxnFeeAmt());
        order.setOrderAmt(actualAmt);

        order.setCardNo(info.getCustCard().getCardNo());
        order.setBankCode(info.getCustCard().getBankCode());
        order.setExtOrderId(wd.getExBizOrderId());
        
        // ✅ 保存产品ID和放款方式（关键！审核通过时需要这些信息）
        order.setProductId(info.getProduct().getProductId());
        order.setWithdrawMode(String.valueOf(info.getProduct().getWithdrawMode()));
        
        // 设置审核相关字段
        order.setNeedAudit(needAudit);
        if ("Y".equals(needAudit)) {
            order.setOrderStatus(String.valueOf(OrderStatus.U)); // 未支付
            order.setAuditStatus("U"); // 待审核
        } else {
            order.setOrderStatus(String.valueOf(OrderStatus.U)); // 未支付
            order.setAuditStatus("N"); // 无需审核
        }
        
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());

        em.persist(order);
        return order;
    }

    /**
     * 生成订单号
     */
    private String generateOrderId() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * 生成提款手续费
     */
    private BigDecimal genWithdrawFee(WithdrawReq wd, LoanInfo info) {
        if (info.getProduct().getRepayMethod() != RepayMethod.AT) {
            return BigDecimal.ZERO;
        }
        // 手续费 = 提款金额 * 费率
        return wd.getWithdrawAmt().multiply(wd.getWithdrawFee());
    }

    /**
     * 创建贷款定价及费用定价
     *
     * @param wd      提款请求
     * @param cust    客户信息
     * @param info    贷款信息
     * @param loanReg 贷款登记记录（已保存，包含生成的ID）
     * @return 借据定义对象
     */
    private ClsLoanDef newLoanDef(WithdrawReq wd, CustInfo cust, LoanInfo info, ClsLoanReg loanReg) {
        ClsLoanDef loanDef = new ClsLoanDef();
        // 使用 loanReg 的 ID 作为外键
        loanDef.setLoanRegId(loanReg.getLoanRegId());
        loanDef.setInterestRate(BigDecimal.ZERO); // 协议利率
        loanDef.setInterestRateType("Y"); // 年利率
        loanDef.setFloatRate(new BigDecimal("0.05")); // 浮动利率5%

        em.persist(loanDef);

        // 将loanDef关联到info
        info.setLoanDef(loanDef);

        logger.info("创建借据定义成功，loanRegId: {}", loanReg.getLoanRegId());
        return loanDef;
    }

    /**
     * 创建贷款登记信息
     */
    private ClsLoanReg newLoanReg(WithdrawReq wd, BigDecimal withdrawTxnFee, LoanInfo info) {
        ClsLoanReg loanReg = new ClsLoanReg();
        loanReg.setCustId(info.getCustInfo().getCustId());
        loanReg.setContrNo(info.getClsContr().getContrNo());
        loanReg.setBillNo(""); // 审核后生成
        loanReg.setLoanRegStatus("U"); // 待审核
        loanReg.setLoanPrin(wd.getLoanPrin());
        loanReg.setOrderId(null);
        loanReg.setLendingAmt(wd.getWithdrawAmt());
        loanReg.setTxnFeeAmt(withdrawTxnFee);
        loanReg.setApllyTime(new Date());
        loanReg.setExtBizOrderId(wd.getExBizOrderId());
        loanReg.setAuditResult("U"); // 待审核
        loanReg.setLoanAction("消费贷款");

        em.persist(loanReg);
        return loanReg;
    }

    /**
     * 持久化贷款登记和定价
     */
    private void saveLoanRegLoanDef(LoanInfo info, ClsLoanReg loanReg, ClsLoanDef loanDef) {
        // loanReg和loanDef都已经在各自的方法中persist了
        // 这里可以添加额外的持久化逻辑，如果有的话
        // 目前主要是确保关系正确建立
        info.setLoanReg(loanReg);
    }

    /**
     * 记录贷款事件
     */
    private void recordLoanEvent(ClsLoanReg loanReg, String eventType, String memo) {
        ClsLoanEvent event = new ClsLoanEvent();
        event.setLoanEventId(System.currentTimeMillis());
        event.setCustId(loanReg.getCustId());
        event.setContrNo(loanReg.getContrNo());
        event.setBillNo(loanReg.getBillNo());
        event.setLoanEventType(eventType);
        event.setMemo(memo);
        event.setSetupTime(new Date());
        event.setCreateTime(new Date());
        event.setCreateUser("SYSTEM");
        event.setJpaVersion(0L);

        em.persist(event);
    }

    /**
     * 审核通过后创建借据和还款计划
     */
    @Transactional
    public String approveWithdraw(String loanRegId) {
        logger.info("开始审核通过，loanRegId: {}", loanRegId);

        ClsLoanReg loanReg = loanRegRepo.findById(Long.parseLong(loanRegId))
                .orElseThrow(() -> new RuntimeException("贷款登记不存在"));

        if (!"U".equals(loanReg.getAuditResult())) {
            throw new RuntimeException("贷款登记状态不是待审核");
        }

        // 更新审核状态
        loanReg.setAuditResult("A"); // 审核通过
        loanReg.setLoanRegStatus("A"); // 审核通过

        // 生成借据号
        String billNo = generateBillNo();
        loanReg.setBillNo(billNo);

        em.merge(loanReg);

        // ✅ 激活订单（如果存在）
        if (StringUtils.isNotBlank(loanReg.getOrderId())) {
            ClsOrder order = orderRepo.findById(loanReg.getOrderId()).orElse(null);
            if (order != null) {
                order.setBillNo(billNo);
                order.setAuditStatus("A"); // 审核通过
                order.setUpdateTime(new Date());
                em.merge(order);
                logger.info("订单已激活，orderId: {}, billNo: {}", order.getOrderId(), billNo);
            }
        } else {
            logger.warn("贷款登记没有关联的订单ID，loanRegId: {}", loanRegId);
        }

        // 创建还款计划
        createRepayPlan(loanReg);

        // 记录事件
        recordLoanEvent(loanReg, "WITHDRAW_APPROVE", "提款审核通过，生成借据: " + billNo);

        logger.info("审核通过完成，billNo: {}", billNo);
        return billNo;
    }

    /**
     * 生成借据号
     */
    private String generateBillNo() {
        return "BILL" + System.currentTimeMillis();
    }

    /**
     * 创建还款计划
     * @param loanReg 贷款登记记录
     */
    private void createRepayPlan(ClsLoanReg loanReg) {
        logger.info("开始创建还款计划，loanRegId: {}, loanPrin: {}", 
            loanReg.getLoanRegId(), loanReg.getLoanPrin());
        
        // TODO: 根据还款方式（等额本息、等额本金等）生成详细的还款计划
        // 当前简化处理：根据贷款期限生成多期计划
        
        int termCount = 12; // 默认12期，实际应从产品配置或请求中获取
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
            
            // 计算到期日（每月一期）
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            cal.add(Calendar.MONTH, i);
            plan.setDueDate(cal.getTime());
            
            // 最后一期调整本金，避免除不尽的误差
            BigDecimal currentPrin = (i == termCount) ? 
                totalPrin.subtract(prinPerTerm.multiply(new BigDecimal(termCount - 1))) : 
                prinPerTerm;
            
            plan.setPrinAmt(currentPrin);
            plan.setInterestAmt(BigDecimal.ZERO); // TODO: 根据利率计算利息
            plan.setFeeAmt(feePerTerm);
            plan.setTotalAmt(currentPrin.add(plan.getInterestAmt()).add(feePerTerm));
            
            plan.setPaidPrin(BigDecimal.ZERO);
            plan.setPaidInterest(BigDecimal.ZERO);
            plan.setPaidFee(BigDecimal.ZERO);
            plan.setRemainAmt(plan.getTotalAmt());
            plan.setStatus("U"); // 未还
            plan.setOverdueDays(0);
            plan.setCreateTime(new Date());
            plan.setUpdateTime(new Date());

            em.persist(plan);
            logger.debug("创建第{}期还款计划，planId: {}, dueDate: {}", 
                i, plan.getPlanId(), plan.getDueDate());
        }
        
        logger.info("创建还款计划成功，共{}期，billNo: {}", termCount, loanReg.getBillNo());
    }

    /**
     * 放款失败处理 - 支付网关调用失败
     */
    @Transactional
    public void handleWithdrawPayFail(String orderId, String failReason) {
        logger.warn("放款支付失败，orderId: {}, 原因: {}", orderId, failReason);

        ClsOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 更新订单状态为失败
        order.setOrderStatus(String.valueOf(OrderStatus.F));
        order.setFailReason(failReason);
        order.setUpdateTime(new Date());
        em.merge(order);

        // 不生成借据和还款计划，核心的账务没有生成
        // 用户可以在页面点击重提

        logger.info("订单状态已更新为失败，支持重提");
    }

    /**
     * 放款入账异常处理 - 支付已成功但后续保存数据异常
     */
    @Transactional
    public void handleWithdrawAccountFail(String orderId, String failReason) {
        logger.error("放款入账异常，orderId: {}, 原因: {}", orderId, failReason);

        ClsOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 订单状态保持为支付中或恢复原状
        order.setOrderStatus(String.valueOf(OrderStatus.J)); // 支付中
        order.setFailReason(failReason);
        order.setUpdateTime(new Date());
        em.merge(order);

        // 借据不落地，通过异步对账或人工干预来修改状态
        logger.warn("订单状态保持为支付中，等待对账或人工干预");
    }

    /**
     * 重试放款（用于支付失败后重提）
     */
    @Transactional
    public String retryWithdraw(String orderId) {
        logger.info("重试放款，orderId: {}", orderId);

        ClsOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!OrderStatus.F.name().equals(order.getOrderStatus())) {
            throw new RuntimeException("只有失败的订单才能重提");
        }

        // 重置订单状态
        order.setOrderStatus(String.valueOf(OrderStatus.U));
        order.setFailReason(null);
        order.setUpdateTime(new Date());
        em.merge(order);

        logger.info("订单状态已重置，可以重新发起支付");
        return orderId;
    }
}