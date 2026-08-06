package com.example.filter;

import com.example.payment.entity.ClsContract;
import com.example.payment.entity.CustInfo;
import com.example.payment.entity.req.WithdrawReq;
import com.example.payment.service.ContractInquirer;
import com.example.payment.service.CustInquirer;
import com.example.payment.service.TxnContext;
import com.example.rpc.filter.AbstractProcessFilter;
import com.example.rpc.filter.ExecuteStatus;
import org.springframework.beans.factory.annotation.Autowired;

public class LoadContextFilter extends AbstractProcessFilter<TxnContext> {
    @Autowired
    private ContractInquirer contractInquirer; // 假设存在的查询组件

    @Autowired
    private CustInquirer custInquirer;         // 假设存在的查询组件

    @Override
    public ExecuteStatus process(TxnContext context) throws Exception {
        WithdrawReq req = (WithdrawReq) context.getReqInfo();

        // 1. 加载合同信息
        ClsContract contract = contractInquirer.getClsContrInfo(req.getExBizOrderId());
        if (contract == null) {
            context.getRespInfo().setRespCode("E_002");
            context.getRespInfo().setRespDesc("合同不存在: " + req.getExBizOrderId());
            return ExecuteStatus.TERMINATE;
        }
        context.setContInfo(contract);

        // 2. 加载客户信息
        CustInfo custInfo = custInquirer.findByCustId(contract.getCustId());
        if (custInfo == null) {
            context.getRespInfo().setRespCode("E_003");
            context.getRespInfo().setRespDesc("客户信息不存在");
            return ExecuteStatus.TERMINATE;
        }
        context.setCustInfo(custInfo);

        // 3. 可以在这里继续加载借据、订单等其他关联数据

        return ExecuteStatus.CONTINUE;
    }

}
