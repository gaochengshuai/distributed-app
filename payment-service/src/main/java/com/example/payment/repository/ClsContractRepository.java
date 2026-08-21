package com.example.payment.repository;

import com.example.payment.entity.ClsContract;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClsContractRepository extends JpaRepository<ClsContract, String> {
    /**
     * 根据合同号查询合同
     */
    ClsContract findByContrNo(String contrNo);
    
    /**
     * 根据客户ID和合同状态查询合同列表
     * @param custId 客户ID
     * @param contrStatus 合同状态（A-有效）
     * @return 合同列表
     */
    List<ClsContract> findByCustIdAndContrStatus(String custId, String contrStatus);
    
    /**
     * 根据客户ID查询所有合同
     * @param custId 客户ID
     * @return 合同列表
     */
    List<ClsContract> findByCustId(String custId);
    
    /**
     * 根据产品ID查询合同列表
     * @param productId 产品ID
     * @return 合同列表
     */
    List<ClsContract> findByProductId(String productId);
}
