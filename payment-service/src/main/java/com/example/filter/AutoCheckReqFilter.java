package com.example.filter;

import com.example.payment.service.TxnContext;
import com.example.rpc.filter.AbstractProcessFilter;
import com.example.rpc.filter.ExecuteStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

public class AutoCheckReqFilter extends AbstractProcessFilter<TxnContext> {
    @Autowired
    private Validator validator;
    @Override
    public ExecuteStatus process(TxnContext context) throws Exception {
        Object reqInfo = context.getReqInfo();

        // 1. 执行 JSR-303 校验
        Set<ConstraintViolation<Object>> violations =
                validator.validate(reqInfo);

        if (!violations.isEmpty()) {
            // 2. 校验失败，获取第一个错误信息并中断流程
            ConstraintViolation<Object> violation = violations.iterator().next();
            String errorMsg = violation.getMessage();

            context.getRespInfo().setRespCode("E_001");
            context.getRespInfo().setRespDesc("参数校验失败: " + errorMsg);

            return ExecuteStatus.TERMINATE;
        }

        return ExecuteStatus.CONTINUE;
    }
}
