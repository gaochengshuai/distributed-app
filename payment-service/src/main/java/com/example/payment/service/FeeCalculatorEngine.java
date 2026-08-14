package com.example.payment.service;

import com.example.payment.enums.RepayMethod;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 费用计算引擎
 * 支持多种还款方式和费用类型的可复用计算逻辑
 */
@Slf4j
@Component
public class FeeCalculatorEngine {

    /**
     * 计算精度：保留2位小数
     */
    private static final int SCALE = 2;
    
    /**
     * 舍入模式：四舍五入
     */
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * 计算还款计划（根据还款方式）
     * 
     * @param principal 本金
     * @param annualRate 年利率（小数形式，如0.05表示5%）
     * @param termCount 期数
     * @param repayMethod 还款方式
     * @param feeRate 手续费率（可选）
     * @return 还款计划列表
     */
    public List<RepaymentTerm> calculateRepaymentPlan(BigDecimal principal, BigDecimal annualRate, 
                                                       int termCount, RepayMethod repayMethod,
                                                       BigDecimal feeRate) {
        log.info("开始计算还款计划，本金: {}, 年利率: {}, 期数: {}, 还款方式: {}", 
            principal, annualRate, termCount, repayMethod);
        
        // 选择对应的计算器
        RepaymentCalculator calculator = getCalculator(repayMethod);
        
        // 计算每期明细
        List<RepaymentTerm> terms = calculator.calculate(principal, annualRate, termCount);
        
        // 如果有手续费率，计算手续费
        if (feeRate != null && feeRate.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalFee = principal.multiply(feeRate).setScale(SCALE, ROUNDING_MODE);
            BigDecimal feePerTerm = totalFee.divide(new BigDecimal(termCount), SCALE, ROUNDING_MODE);
            
            // 将手续费分摊到每期
            for (int i = 0; i < terms.size(); i++) {
                RepaymentTerm term = terms.get(i);
                // 最后一期调整手续费，避免除不尽的误差
                if (i == terms.size() - 1) {
                    BigDecimal paidFee = feePerTerm.multiply(new BigDecimal(i));
                    term.setFee(totalFee.subtract(paidFee));
                } else {
                    term.setFee(feePerTerm);
                }
                term.recalculateTotal();
            }
        }
        
        log.info("还款计划计算完成，共{}期", terms.size());
        return terms;
    }

    /**
     * 获取对应的计算器（工厂方法）
     */
    private RepaymentCalculator getCalculator(RepayMethod repayMethod) {
        switch (repayMethod) {
            case AP:
                return new EqualPrincipalCalculator();
            case AI:
                return new EqualInstallmentCalculator();
            case IF:
                return new InterestFirstCalculator();
            case OT:
                return new OneTimeRepayCalculator();
            default:
                throw new IllegalArgumentException("不支持的还款方式: " + repayMethod);
        }
    }

    /**
     * 计算单笔利息
     * 
     * @param principal 本金
     * @param annualRate 年利率
     * @param days 天数
     * @return 利息
     */
    public BigDecimal calculateInterest(BigDecimal principal, BigDecimal annualRate, int days) {
        if (principal == null || annualRate == null || days <= 0) {
            return BigDecimal.ZERO;
        }
        
        // 日利率 = 年利率 / 365
        BigDecimal dailyRate = annualRate.divide(new BigDecimal("365"), 10, ROUNDING_MODE);
        
        // 利息 = 本金 × 日利率 × 天数
        BigDecimal interest = principal.multiply(dailyRate).multiply(new BigDecimal(days));
        
        return interest.setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * 计算逾期费用
     * 
     * @param overduePrincipal 逾期本金
     * @param overdueInterest 逾期利息
     * @param penaltyRate 罚息利率（年化）
     * @param overdueDays 逾期天数
     * @return 逾期费用（罚息 + 违约金）
     */
    public OverdueFee calculateOverdueFee(BigDecimal overduePrincipal, BigDecimal overdueInterest,
                                          BigDecimal penaltyRate, int overdueDays) {
        OverdueFee fee = new OverdueFee();
        
        // 罚息 = (逾期本金 + 逾期利息) × 罚息利率 / 365 × 逾期天数
        BigDecimal baseAmount = overduePrincipal.add(overdueInterest);
        BigDecimal penaltyInterest = calculateInterest(baseAmount, penaltyRate, overdueDays);
        
        // 违约金 = 逾期本金 × 违约金比例（假设0.05%每天）
        BigDecimal penaltyFeeRate = new BigDecimal("0.0005"); // 0.05%/天
        BigDecimal penaltyFee = overduePrincipal.multiply(penaltyFeeRate)
            .multiply(new BigDecimal(overdueDays))
            .setScale(SCALE, ROUNDING_MODE);
        
        fee.setPenaltyInterest(penaltyInterest);
        fee.setPenaltyFee(penaltyFee);
        fee.setTotalFee(penaltyInterest.add(penaltyFee));
        
        log.info("逾期费用计算完成，罚息: {}, 违约金: {}, 合计: {}", 
            penaltyInterest, penaltyFee, fee.getTotalFee());
        
        return fee;
    }

    /**
     * 计算提前还款费用
     * 
     * @param remainingPrincipal 剩余本金
     * @param earlyRepayPenaltyRate 提前还款违约金率
     * @return 提前还款费用
     */
    public BigDecimal calculateEarlyRepayFee(BigDecimal remainingPrincipal, BigDecimal earlyRepayPenaltyRate) {
        if (earlyRepayPenaltyRate == null || earlyRepayPenaltyRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal fee = remainingPrincipal.multiply(earlyRepayPenaltyRate)
            .setScale(SCALE, ROUNDING_MODE);
        
        log.info("提前还款费用计算完成，费用: {}", fee);
        return fee;
    }

    // ==================== 内部类和接口定义 ====================

    /**
     * 还款期次明细
     */
    @Data
    public static class RepaymentTerm {
        /** 期数 */
        private int termNo;
        
        /** 应还本金 */
        private BigDecimal principal;
        
        /** 应还利息 */
        private BigDecimal interest;
        
        /** 应还手续费 */
        private BigDecimal fee;
        
        /** 应还总额 */
        private BigDecimal total;
        
        /** 剩余本金 */
        private BigDecimal remainingPrincipal;

        /**
         * 重新计算总额
         */
        public void recalculateTotal() {
            this.total = principal.add(interest).add(fee != null ? fee : BigDecimal.ZERO);
        }
    }

    /**
     * 逾期费用
     */
    @Data
    public static class OverdueFee {
        /** 罚息 */
        private BigDecimal penaltyInterest;
        
        /** 违约金 */
        private BigDecimal penaltyFee;
        
        /** 总费用 */
        private BigDecimal totalFee;
    }

    /**
     * 还款计算器接口
     */
    interface RepaymentCalculator {
        /**
         * 计算还款计划
         * 
         * @param principal 本金
         * @param annualRate 年利率
         * @param termCount 期数
         * @return 还款期次列表
         */
        List<RepaymentTerm> calculate(BigDecimal principal, BigDecimal annualRate, int termCount);
    }

    /**
     * 等额本金计算器
     * 特点：每月还款本金固定，利息逐月递减
     */
    static class EqualPrincipalCalculator implements RepaymentCalculator {
        @Override
        public List<RepaymentTerm> calculate(BigDecimal principal, BigDecimal annualRate, int termCount) {
            List<RepaymentTerm> terms = new ArrayList<>();
            
            // 每月固定本金
            BigDecimal monthlyPrincipal = principal.divide(new BigDecimal(termCount), SCALE, ROUNDING_MODE);
            
            // 月利率
            BigDecimal monthlyRate = annualRate.divide(new BigDecimal("12"), 10, ROUNDING_MODE);
            
            BigDecimal remainingPrincipal = principal;
            
            for (int i = 1; i <= termCount; i++) {
                RepaymentTerm term = new RepaymentTerm();
                term.setTermNo(i);
                
                // 最后一期调整本金，避免除不尽的误差
                if (i == termCount) {
                    term.setPrincipal(remainingPrincipal);
                } else {
                    term.setPrincipal(monthlyPrincipal);
                }
                
                // 当月利息 = 剩余本金 × 月利率
                BigDecimal interest = remainingPrincipal.multiply(monthlyRate)
                    .setScale(SCALE, ROUNDING_MODE);
                term.setInterest(interest);
                
                // 更新剩余本金
                remainingPrincipal = remainingPrincipal.subtract(term.getPrincipal());
                term.setRemainingPrincipal(remainingPrincipal.max(BigDecimal.ZERO));
                
                // 手续费暂为空，由外层统一处理
                term.setFee(BigDecimal.ZERO);
                term.recalculateTotal();
                
                terms.add(term);
            }
            
            return terms;
        }
    }

    /**
     * 等额本息计算器
     * 特点：每月还款总额固定，本金逐月递增，利息逐月递减
     */
    static class EqualInstallmentCalculator implements RepaymentCalculator {
        @Override
        public List<RepaymentTerm> calculate(BigDecimal principal, BigDecimal annualRate, int termCount) {
            List<RepaymentTerm> terms = new ArrayList<>();
            
            // 月利率
            BigDecimal monthlyRate = annualRate.divide(new BigDecimal("12"), 10, ROUNDING_MODE);
            
            // 每月还款额 = 本金 × 月利率 × (1+月利率)^期数 / ((1+月利率)^期数 - 1)
            BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
            BigDecimal power = onePlusRate.pow(termCount);
            BigDecimal monthlyPayment = principal.multiply(monthlyRate).multiply(power)
                .divide(power.subtract(BigDecimal.ONE), SCALE, ROUNDING_MODE);
            
            BigDecimal remainingPrincipal = principal;
            
            for (int i = 1; i <= termCount; i++) {
                RepaymentTerm term = new RepaymentTerm();
                term.setTermNo(i);
                
                // 当月利息 = 剩余本金 × 月利率
                BigDecimal interest = remainingPrincipal.multiply(monthlyRate)
                    .setScale(SCALE, ROUNDING_MODE);
                term.setInterest(interest);
                
                // 当月本金 = 月供 - 利息
                BigDecimal currentPrincipal;
                if (i == termCount) {
                    // 最后一期直接还清剩余本金
                    currentPrincipal = remainingPrincipal;
                    term.setPrincipal(currentPrincipal);
                } else {
                    currentPrincipal = monthlyPayment.subtract(interest);
                    term.setPrincipal(currentPrincipal);
                }
                
                // 月供 = 本金 + 利息
                term.setTotal(monthlyPayment);
                
                // 更新剩余本金
                remainingPrincipal = remainingPrincipal.subtract(currentPrincipal);
                term.setRemainingPrincipal(remainingPrincipal.max(BigDecimal.ZERO));
                
                // 手续费暂为空
                term.setFee(BigDecimal.ZERO);
                
                terms.add(term);
            }
            
            return terms;
        }
    }

    /**
     * 先息后本计算器
     * 特点：前期只还利息，最后一期还本金+利息
     */
    static class InterestFirstCalculator implements RepaymentCalculator {
        @Override
        public List<RepaymentTerm> calculate(BigDecimal principal, BigDecimal annualRate, int termCount) {
            List<RepaymentTerm> terms = new ArrayList<>();
            
            // 月利率
            BigDecimal monthlyRate = annualRate.divide(new BigDecimal("12"), 10, ROUNDING_MODE);
            
            // 每月固定利息
            BigDecimal monthlyInterest = principal.multiply(monthlyRate)
                .setScale(SCALE, ROUNDING_MODE);
            
            for (int i = 1; i <= termCount; i++) {
                RepaymentTerm term = new RepaymentTerm();
                term.setTermNo(i);
                term.setInterest(monthlyInterest);
                
                if (i == termCount) {
                    // 最后一期还本金
                    term.setPrincipal(principal);
                    term.setRemainingPrincipal(BigDecimal.ZERO);
                } else {
                    // 前期不还本金
                    term.setPrincipal(BigDecimal.ZERO);
                    term.setRemainingPrincipal(principal);
                }
                
                term.setFee(BigDecimal.ZERO);
                term.recalculateTotal();
                
                terms.add(term);
            }
            
            return terms;
        }
    }

    /**
     * 一次性还本付息计算器
     * 特点：到期一次性归还本金和所有利息
     */
    static class OneTimeRepayCalculator implements RepaymentCalculator {
        @Override
        public List<RepaymentTerm> calculate(BigDecimal principal, BigDecimal annualRate, int termCount) {
            List<RepaymentTerm> terms = new ArrayList<>();
            
            // 只有一期
            RepaymentTerm term = new RepaymentTerm();
            term.setTermNo(1);
            term.setPrincipal(principal);
            
            // 总利息 = 本金 × 年利率 × (期数/12)
            BigDecimal totalInterest = principal.multiply(annualRate)
                .multiply(new BigDecimal(termCount))
                .divide(new BigDecimal("12"), SCALE, ROUNDING_MODE);
            term.setInterest(totalInterest);
            
            term.setRemainingPrincipal(BigDecimal.ZERO);
            term.setFee(BigDecimal.ZERO);
            term.recalculateTotal();
            
            terms.add(term);
            
            return terms;
        }
    }
}
