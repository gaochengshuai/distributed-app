package com.example.payment.repository;
import com.example.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment,Long> {
    // 假设创建时间字段名为 createTime，按降序排列（最新的在前面）
    List<Payment> findAllByOrderByCreatedAtDesc();
}
