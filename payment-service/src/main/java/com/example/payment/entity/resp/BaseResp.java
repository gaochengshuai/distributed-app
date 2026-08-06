package com.example.payment.entity.resp;

import lombok.Data;

@Data
public class BaseResp{
    /**
     * 响应码 (例如: "S_000" 成功, "E_001" 失败)
     */
    private String respCode;

    /**
     * 响应描述 (例如: "处理成功", "重复的请求")
     */
    private String respDesc;
}
