package com.example.payment.entity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String orderNo;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;

    public Long getId(){return id;} public void setId(Long i){this.id=i;}
    public String getOrderNo(){return orderNo;} public void setOrderNo(String o){this.orderNo=o;}
    public BigDecimal getAmount(){return amount;}
    public void setAmount(BigDecimal amount){this.amount = amount;}
    public String getStatus(){return status;} public void setStatus(String s){this.status=s;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime t){this.createdAt=t;}
}
