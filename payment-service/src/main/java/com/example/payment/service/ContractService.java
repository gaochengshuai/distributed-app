package com.example.payment.service;

import com.example.payment.entity.ClsContract;
import com.example.payment.repository.ClsContractRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 合同信息服务类
 * 负责合同的创建、查询、更新等业务逻辑
 */
@Service
public class ContractService {

    @Autowired
    private ClsContractRepository contractRepository;

    private static final Logger logger = LoggerFactory.getLogger(ContractService.class);

    /**
     * 创建新合同
     * @param contrNo 合同号
     * @param custId 客户ID
     * @param productId 产品ID
     * @param signAmt 签署金额
     * @return 创建的合同对象
     */
    @Transactional
    public ClsContract createContract(String contrNo, String custId, String productId, BigDecimal signAmt) {
        logger.info("创建合同，contrNo: {}, custId: {}, productId: {}, signAmt: {}", 
                contrNo, custId, productId, signAmt);

        // 检查合同是否已存在
        if (contractRepository.existsById(contrNo)) {
            logger.warn("合同已存在，contrNo: {}", contrNo);
            throw new IllegalArgumentException("合同已存在: " + contrNo);
        }

        // 创建合同对象
        ClsContract contract = new ClsContract();
        contract.setContrNo(contrNo);
        contract.setCustId(custId);
        contract.setProductId(productId);
        contract.setSignAmt(signAmt);
        contract.setContrStatus("A"); // 默认状态为有效（Active）

        // 保存合同
        ClsContract savedContract = contractRepository.save(contract);
        logger.info("合同创建成功，contrNo: {}", savedContract.getContrNo());

        return savedContract;
    }

    /**
     * 根据合同号查询合同
     * @param contrNo 合同号
     * @return 合同对象，不存在返回null
     */
    public ClsContract getContractByNo(String contrNo) {
        logger.debug("查询合同，contrNo: {}", contrNo);
        return contractRepository.findByContrNo(contrNo);
    }

    /**
     * 根据客户ID查询所有合同
     * @param custId 客户ID
     * @return 合同列表
     */
    public List<ClsContract> getContractsByCustId(String custId) {
        logger.debug("查询客户的所有合同，custId: {}", custId);
        return contractRepository.findByCustId(custId);
    }

    /**
     * 根据客户ID和状态查询合同
     * @param custId 客户ID
     * @param status 合同状态（A-有效，U-待审核，R-已拒绝，C-已关闭）
     * @return 合同列表
     */
    public List<ClsContract> getContractsByCustIdAndStatus(String custId, String status) {
        logger.debug("查询客户的指定状态合同，custId: {}, status: {}", custId, status);
        return contractRepository.findByCustIdAndContrStatus(custId, status);
    }

    /**
     * 获取客户的第一个有效合同
     * @param custId 客户ID
     * @return 合同对象，不存在返回null
     */
    public ClsContract getFirstActiveContract(String custId) {
        logger.debug("获取客户的第一个有效合同，custId: {}", custId);
        List<ClsContract> contracts = contractRepository.findByCustIdAndContrStatus(custId, "A");
        
        if (contracts == null || contracts.isEmpty()) {
            logger.warn("客户没有有效合同，custId: {}", custId);
            return null;
        }
        
        return contracts.get(0);
    }

    /**
     * 更新合同状态
     * @param contrNo 合同号
     * @param newStatus 新状态
     * @return 更新后的合同对象
     */
    @Transactional
    public ClsContract updateContractStatus(String contrNo, String newStatus) {
        logger.info("更新合同状态，contrNo: {}, newStatus: {}", contrNo, newStatus);

        ClsContract contract = contractRepository.findByContrNo(contrNo);
        if (contract == null) {
            logger.warn("合同不存在，contrNo: {}", contrNo);
            throw new IllegalArgumentException("合同不存在: " + contrNo);
        }

        contract.setContrStatus(newStatus);
        ClsContract updatedContract = contractRepository.save(contract);
        
        logger.info("合同状态更新成功，contrNo: {}, status: {}", contrNo, newStatus);
        return updatedContract;
    }

    /**
     * 激活合同（将状态设置为A）
     * @param contrNo 合同号
     * @return 更新后的合同对象
     */
    @Transactional
    public ClsContract activateContract(String contrNo) {
        return updateContractStatus(contrNo, "A");
    }

    /**
     * 关闭合同（将状态设置为C）
     * @param contrNo 合同号
     * @return 更新后的合同对象
     */
    @Transactional
    public ClsContract closeContract(String contrNo) {
        return updateContractStatus(contrNo, "C");
    }

    /**
     * 删除合同
     * @param contrNo 合同号
     */
    @Transactional
    public void deleteContract(String contrNo) {
        logger.info("删除合同，contrNo: {}", contrNo);

        if (!contractRepository.existsById(contrNo)) {
            logger.warn("合同不存在，无法删除，contrNo: {}", contrNo);
            throw new IllegalArgumentException("合同不存在: " + contrNo);
        }

        contractRepository.deleteById(contrNo);
        logger.info("合同删除成功，contrNo: {}", contrNo);
    }

    /**
     * 检查合同是否存在
     * @param contrNo 合同号
     * @return true-存在，false-不存在
     */
    public boolean existsContract(String contrNo) {
        return contractRepository.existsById(contrNo);
    }

    /**
     * 获取所有合同
     * @return 合同列表
     */
    public List<ClsContract> getAllContracts() {
        logger.debug("获取所有合同");
        return contractRepository.findAll();
    }

    /**
     * 根据产品ID查询合同
     * @param productId 产品ID
     * @return 合同列表
     */
    public List<ClsContract> getContractsByProductId(String productId) {
        logger.debug("查询产品的所有合同，productId: {}", productId);
        return contractRepository.findByProductId(productId);
    }
}
