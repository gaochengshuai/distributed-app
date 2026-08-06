package com.example.payment.service;

import com.example.payment.entity.CustInfo;
import com.example.payment.repository.CustInfoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CustInquirer {
    @PersistenceContext
    private EntityManager em;
    private CustInfoRepository custInfoRepo;
    private static Logger logger = LoggerFactory.getLogger(CustInquirer.class);
    public CustInfo findByCustId(String custId) {
        CustInfo custInfo = custInfoRepo.findByCustId(custId);
        em.persist(custInfo);
        return custInfo;
    };
}
