package com.example.filter;

import com.example.payment.entity.req.WithdrawReq;
import com.example.payment.service.TxnContext;
import com.example.rpc.filter.AbstractProcessFilter;
import com.example.rpc.filter.ExecuteStatus;

public class IdeFilter extends AbstractProcessFilter<TxnContext> {
    @Override
    public ExecuteStatus process(TxnContext context) throws Exception {
        WithdrawReq req = context.getReqInfo();

        // 检查外部订单号是否已存在
        if (isDuplicateOrder(req.getExBizOrderId())) {
            context.getRespInfo().setRespCode("E_001");
            context.getRespInfo().setRespDesc("重复的请求");
            return ExecuteStatus.TERMINATE; // 中断执行
        }

        return ExecuteStatus.CONTINUE; // 继续执行
    }

    private boolean isDuplicateOrder(String exBizOrderId) {
        // 查询数据库判断是否重复
        return false;
    }
}
