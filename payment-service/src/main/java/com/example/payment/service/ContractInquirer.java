package com.example.payment.service;

import com.example.payment.entity.ClsContract;
import com.example.payment.repository.ClsContractRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ContractInquirer {
    @PersistenceContext
    private EntityManager em;
    private ClsContractRepository clsContractRepo;
    private static Logger logger = LoggerFactory.getLogger(ContractInquirer.class);

    public ClsContract getClsContrInfo(String contrNo) {
        ClsContract contr = clsContractRepo.findByContrNo(contrNo);
        em.persist(contr);
        return contr;
    }
}
