package com.example.payment.service;

import com.example.payment.entity.CustInfo;
import com.example.payment.repository.CustInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CustInquirer {
    
    @Autowired
    private CustInfoRepository custInfoRepo;
    
    private static Logger logger = LoggerFactory.getLogger(CustInquirer.class);
    
    /**
     * 根据客户ID查询客户信息
     */
    public CustInfo findByCustId(String custId) {
        logger.debug("查询客户信息，custId: {}", custId);
        CustInfo custInfo = custInfoRepo.findByCustId(custId);
        if (custInfo == null) {
            logger.warn("客户信息不存在，custId: {}", custId);
        }
        return custInfo;
    }
}
