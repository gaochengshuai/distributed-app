-- ============================================
-- Flyway 修复脚本
-- 用于清理失败的迁移记录并重新执行迁移
-- ============================================

-- 1. 查看当前 Flyway schema history
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- 2. 删除失败的 V2 迁移记录
DELETE FROM flyway_schema_history WHERE version = '2' AND success = 0;

-- 3. 如果表已部分创建，需要手动删除它们
-- 注意：执行前请确认这些表是否应该被删除
DROP TABLE IF EXISTS cls_order;
DROP TABLE IF EXISTS cls_loan_reg;
DROP TABLE IF EXISTS cls_repay_plan;
DROP TABLE IF EXISTS cls_repay_record;
DROP TABLE IF EXISTS recon_exception;
DROP TABLE IF EXISTS cust_card;
DROP TABLE IF EXISTS cls_term;
DROP TABLE IF EXISTS cls_term_fee;
DROP TABLE IF EXISTS cls_txn;
DROP TABLE IF EXISTS cls_loan_def;
DROP TABLE IF EXISTS cls_contract;
DROP TABLE IF EXISTS cls_loan_event;
DROP TABLE IF EXISTS cust_info;
DROP TABLE IF EXISTS pruduct;

-- 4. 验证清理结果
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- 5. 重启应用后，Flyway 将重新执行 V2 迁移
