package com.example.payment.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "cust_info")
public class CustInfo {
    @Id
    private String custId;
    private String custName;
    private String certNo;
    private String certType;
    private String mobile;
}