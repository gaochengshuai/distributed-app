package com.example.payment.repository;

import com.example.payment.entity.CustCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustCardRepository extends JpaRepository<CustCard, String> {
    
    /**
     * 根据客户ID查询银行卡
     */
    List<CustCard> findByCustId(String custId);
    
    /**
     * 根据卡号查询
     */
    Optional<CustCard> findByCardNo(String cardNo);
    
    /**
     * 根据客户ID和支付方向查询
     */
    List<CustCard> findByCustIdAndPayDirection(String custId, String payDirection);
}
