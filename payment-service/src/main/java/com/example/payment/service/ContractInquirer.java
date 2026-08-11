package com.example.payment.service;

import com.example.payment.entity.ClsContract;
import com.example.payment.repository.ClsContractRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContractInquirer {
    
    @Autowired
    private ClsContractRepository clsContractRepo;
    
    private static Logger logger = LoggerFactory.getLogger(ContractInquirer.class);

    /**
     * 根据合同号查询合同信息
     */
    public ClsContract getClsContrInfo(String contrNo) {
        logger.debug("查询合同信息，contrNo: {}", contrNo);
        // 注意：这里应该使用外部订单号或其他业务标识来查询合同
        // 当前简化处理，假设exBizOrderId就是contrNo
        ClsContract contr = clsContractRepo.findByContrNo(contrNo);
        if (contr == null) {
            logger.warn("合同不存在，contrNo: {}", contrNo);
        }
        return contr;
    }
    
    /**
     * 根据客户ID查询该客户的第一个有效合同
     * @param custId 客户ID
     * @return 合同信息，如果不存在返回null
     */
    public ClsContract getFirstActiveContractByCustId(String custId) {
        logger.debug("查询客户的有效合同，custId: {}", custId);
        
        // 查询该客户的所有有效合同
        List<ClsContract> contracts = clsContractRepo.findByCustIdAndContrStatus(custId, "A");
        
        if (contracts == null || contracts.isEmpty()) {
            logger.warn("客户没有有效合同，custId: {}", custId);
            return null;
        }
        
        // 返回第一个有效合同
        ClsContract contract = contracts.get(0);
        logger.info("找到客户合同，contrNo: {}, custId: {}", contract.getContrNo(), custId);
        return contract;
    }
}
