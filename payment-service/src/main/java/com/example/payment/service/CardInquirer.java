package com.example.payment.service;

import com.example.payment.entity.CustCard;
import com.example.payment.repository.CustCardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CardInquirer {
    
    @Autowired
    private CustCardRepository custCardRepo;
    
    private static Logger logger = LoggerFactory.getLogger(CardInquirer.class);

    /**
     * 根据客户ID查询银行卡列表
     */
    public List<CustCard> findByCustId(String custId) {
        logger.debug("查询客户银行卡，custId: {}", custId);
        List<CustCard> cards = custCardRepo.findByCustId(custId);
        if (cards == null || cards.isEmpty()) {
            logger.warn("客户银行卡不存在，custId: {}", custId);
        }
        return cards;
    }
    
    /**
     * 根据客户ID和支付方向查询银行卡
     */
    public CustCard findByCustIdAndPayDirection(String custId, String payDirection) {
        logger.debug("查询客户银行卡，custId: {}, payDirection: {}", custId, payDirection);
        List<CustCard> cards = custCardRepo.findByCustIdAndPayDirection(custId, payDirection);
        if (cards == null || cards.isEmpty()) {
            logger.warn("客户银行卡不存在，custId: {}, payDirection: {}", custId, payDirection);
            return null;
        }
        return cards.get(0); // 返回第一张卡
    }
}
