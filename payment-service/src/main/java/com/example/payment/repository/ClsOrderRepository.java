package com.example.payment.repository;

import com.example.payment.entity.ClsOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClsOrderRepository extends JpaRepository<ClsOrder, String> {
    
    /**
     * 根据外部订单号查询
     */
    Optional<ClsOrder> findByExtOrderId(String extOrderId);
    
    /**
     * 根据合同号和借据号查询
     */
    List<ClsOrder> findByContrNoAndBillNo(String contrNo, String billNo);
    
    /**
     * 根据订单状态查询
     */
    List<ClsOrder> findByOrderStatus(String orderStatus);
    
    /**
     * 查询支付中的订单（用于重试）
     */
    @Query("SELECT o FROM ClsOrder o WHERE o.orderStatus = 'J'")
    List<ClsOrder> findPayingOrders();
    
    /**
     * 查询失败的订单（用于重提）
     */
    @Query("SELECT o FROM ClsOrder o WHERE o.orderStatus = 'F'")
    List<ClsOrder> findFailedOrders();
}
