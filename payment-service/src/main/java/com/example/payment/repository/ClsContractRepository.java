package com.example.payment.repository;

import com.example.payment.entity.ClsContract;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClsContractRepository extends JpaRepository<ClsContract, Long> {
    ClsContract findByContrNo(String contrNo);
    
    /**
     * 根据客户ID和合同状态查询合同列表
     * @param custId 客户ID
     * @param contrStatus 合同状态（A-有效）
     * @return 合同列表
     */
    List<ClsContract> findByCustIdAndContrStatus(String custId, String contrStatus);
}
