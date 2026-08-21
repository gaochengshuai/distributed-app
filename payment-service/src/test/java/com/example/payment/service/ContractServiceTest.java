package com.example.payment.service;

import com.example.payment.entity.ClsContract;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 合同服务测试类
 * 演示如何使用 ContractService 进行合同信息管理
 */
@SpringBootTest
public class ContractServiceTest {

    @Autowired
    private ContractService contractService;

    /**
     * 测试创建合同
     */
    @Test
    public void testCreateContract() {
        // 准备数据
        String contrNo = "CONTR_TEST_001";
        String custId = "CUST_TEST_001";
        String productId = "PROD001";
        BigDecimal signAmt = new BigDecimal("100000.00");

        // 创建合同
        ClsContract contract = contractService.createContract(contrNo, custId, productId, signAmt);

        // 验证结果
        assertNotNull(contract);
        assertEquals(contrNo, contract.getContrNo());
        assertEquals(custId, contract.getCustId());
        assertEquals(productId, contract.getProductId());
        assertEquals(signAmt, contract.getSignAmt());
        assertEquals("A", contract.getContrStatus()); // 默认状态为有效

        System.out.println("合同创建成功: " + contract.getContrNo());
    }

    /**
     * 测试查询合同
     */
    @Test
    public void testGetContractByNo() {
        // 先创建测试数据
        String contrNo = "CONTR_TEST_002";
        contractService.createContract(contrNo, "CUST_TEST_002", "PROD001", new BigDecimal("50000.00"));

        // 查询合同
        ClsContract contract = contractService.getContractByNo(contrNo);

        // 验证结果
        assertNotNull(contract);
        assertEquals(contrNo, contract.getContrNo());

        System.out.println("查询到合同: " + contract.getContrNo() + ", 金额: " + contract.getSignAmt());
    }

    /**
     * 测试根据客户ID查询所有合同
     */
    @Test
    public void testGetContractsByCustId() {
        // 创建多个合同
        contractService.createContract("CONTR_CUST_001", "CUST_MULTI", "PROD001", new BigDecimal("100000.00"));
        contractService.createContract("CONTR_CUST_002", "CUST_MULTI", "PROD001", new BigDecimal("80000.00"));
        contractService.createContract("CONTR_CUST_003", "CUST_MULTI", "PROD001", new BigDecimal("60000.00"));

        // 查询客户的所有合同
        List<ClsContract> contracts = contractService.getContractsByCustId("CUST_MULTI");

        // 验证结果
        assertNotNull(contracts);
        assertTrue(contracts.size() >= 3);

        System.out.println("客户 CUST_MULTI 共有 " + contracts.size() + " 个合同");
        contracts.forEach(c -> 
            System.out.println("  - 合同号: " + c.getContrNo() + ", 金额: " + c.getSignAmt())
        );
    }

    /**
     * 测试获取客户的第一个有效合同
     */
    @Test
    public void testGetFirstActiveContract() {
        // 创建测试数据
        String contrNo = "CONTR_ACTIVE_001";
        contractService.createContract(contrNo, "CUST_ACTIVE", "PROD001", new BigDecimal("70000.00"));

        // 获取第一个有效合同
        ClsContract contract = contractService.getFirstActiveContract("CUST_ACTIVE");

        // 验证结果
        assertNotNull(contract);
        assertEquals("A", contract.getContrStatus());

        System.out.println("客户的有效合同: " + contract.getContrNo());
    }

    /**
     * 测试更新合同状态
     */
    @Test
    public void testUpdateContractStatus() {
        // 创建测试合同
        String contrNo = "CONTR_STATUS_001";
        contractService.createContract(contrNo, "CUST_STATUS", "PROD001", new BigDecimal("90000.00"));

        // 更新状态为待审核
        ClsContract updated = contractService.updateContractStatus(contrNo, "U");
        assertEquals("U", updated.getContrStatus());
        System.out.println("状态更新为: " + updated.getContrStatus());

        // 激活合同
        updated = contractService.activateContract(contrNo);
        assertEquals("A", updated.getContrStatus());
        System.out.println("合同已激活: " + updated.getContrStatus());

        // 关闭合同
        updated = contractService.closeContract(contrNo);
        assertEquals("C", updated.getContrStatus());
        System.out.println("合同已关闭: " + updated.getContrStatus());
    }

    /**
     * 测试检查合同是否存在
     */
    @Test
    public void testExistsContract() {
        // 创建测试合同
        String contrNo = "CONTR_EXISTS_001";
        contractService.createContract(contrNo, "CUST_EXISTS", "PROD001", new BigDecimal("40000.00"));

        // 检查存在性
        boolean exists = contractService.existsContract(contrNo);
        assertTrue(exists);

        boolean notExists = contractService.existsContract("CONTR_NOT_EXISTS");
        assertFalse(notExists);

        System.out.println("合同存在性检查完成");
    }

    /**
     * 测试删除合同
     */
    @Test
    public void testDeleteContract() {
        // 创建测试合同
        String contrNo = "CONTR_DELETE_001";
        contractService.createContract(contrNo, "CUST_DELETE", "PROD001", new BigDecimal("30000.00"));

        // 验证创建成功
        assertTrue(contractService.existsContract(contrNo));

        // 删除合同
        contractService.deleteContract(contrNo);

        // 验证删除成功
        assertFalse(contractService.existsContract(contrNo));

        System.out.println("合同删除成功");
    }

    /**
     * 测试异常情况
     */
    @Test
    public void testDuplicateContract() {
        String contrNo = "CONTR_DUPLICATE_001";

        // 第一次创建成功
        contractService.createContract(contrNo, "CUST_DUP", "PROD001", new BigDecimal("50000.00"));

        // 第二次创建应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            contractService.createContract(contrNo, "CUST_DUP", "PROD001", new BigDecimal("60000.00"));
        });

        System.out.println("重复合同检测正常");
    }

    /**
     * 完整业务流程示例
     */
    @Test
    public void testFullBusinessProcess() {
        System.out.println("=== 开始完整业务流程测试 ===\n");

        // 1. 创建合同
        String contrNo = "CONTR_FULL_001";
        String custId = "CUST_FULL_001";
        ClsContract contract = contractService.createContract(
            contrNo, 
            custId, 
            "PROD001", 
            new BigDecimal("120000.00")
        );
        System.out.println("1. 创建合同: " + contract.getContrNo());

        // 2. 查询合同
        ClsContract queried = contractService.getContractByNo(contrNo);
        System.out.println("2. 查询合同: 金额=" + queried.getSignAmt());

        // 3. 获取客户的有效合同（用于提款等场景）
        ClsContract activeContract = contractService.getFirstActiveContract(custId);
        System.out.println("3. 客户有效合同: " + activeContract.getContrNo());

        // 4. 查询客户的所有合同
        List<ClsContract> allContracts = contractService.getContractsByCustId(custId);
        System.out.println("4. 客户总合同数: " + allContracts.size());

        // 5. 更新合同状态
        contractService.updateContractStatus(contrNo, "U");
        System.out.println("5. 更新状态为待审核");

        // 6. 激活合同
        contractService.activateContract(contrNo);
        System.out.println("6. 激活合同");

        // 7. 关闭合同（业务完成后）
        contractService.closeContract(contrNo);
        System.out.println("7. 关闭合同");

        // 8. 清理测试数据
        contractService.deleteContract(contrNo);
        System.out.println("8. 删除合同");

        System.out.println("\n=== 业务流程测试完成 ===");
    }
}
