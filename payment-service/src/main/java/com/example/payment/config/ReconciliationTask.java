package com.example.payment.config;

import com.example.payment.service.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 对账定时任务
 */
@Component
public class ReconciliationTask {

    private static final Logger logger = LoggerFactory.getLogger(ReconciliationTask.class);

    @Autowired
    private ReconciliationService reconciliationService;

    /**
     * 每天凌晨2点执行对账
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void executeDailyReconciliation() {
        logger.info("开始执行每日对账任务");
        try {
            reconciliationService.executeReconciliation();
            logger.info("每日对账任务执行完成");
        } catch (Exception e) {
            logger.error("每日对账任务执行异常", e);
        }
    }

    /**
     * 每小时执行一次增量对账（可选）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void executeHourlyReconciliation() {
        logger.debug("开始执行每小时对账任务");
        try {
            reconciliationService.executeReconciliation();
            logger.debug("每小时对账任务执行完成");
        } catch (Exception e) {
            logger.error("每小时对账任务执行异常", e);
        }
    }
}
