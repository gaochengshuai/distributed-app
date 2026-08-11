# Payment Service API 测试用例

## 基础URL
```
http://localhost:8081/api/loan
```

---

## 1. 发起提款申请

**请求：**
```http
POST /withdraw
Content-Type: application/json

{
  "exBizOrderId": "EXT_ORDER_20260101_001",
  "withdrawAmt": 25000.00,
  "withdrawFee": 0.005,
  "loanPrin": 25000.00,
  "loanTerm": 12,
  "repayType": "AT",
  "applyUserId": "CUST001",
  "applyUserName": "张三"
}
```

**预期响应：**
```json
{
  "success": true,
  "orderId": "ORD1704067200000ABCD1234",
  "message": "提款申请已提交"
}
```

---

## 2. 审核通过（如果需要审核）

**请求：**
```http
POST /approve/1
```

**预期响应：**
```json
{
  "success": true,
  "billNo": "BILL1704067200000",
  "message": "审核通过"
}
```

---

## 3. 重试提款（支付失败后）

**请求：**
```http
POST /retry/ORD1704067200000ABCD1234
```

**预期响应：**
```json
{
  "success": true,
  "orderId": "ORD1704067200000ABCD1234",
  "message": "订单已重置，可以重新支付"
}
```

---

## 4. 发起还款

**请求：**
```http
POST /repay?billNo=BILL1704067200000&amount=2100.00&repayType=NORMAL
```

**预期响应：**
```json
{
  "success": true,
  "orderId": "REP1704067200000EFGH5678",
  "message": "还款处理中"
}
```

---

## 5. 重试还款

**请求：**
```http
POST /repay/retry/REP1704067200000EFGH5678
```

**预期响应：**
```json
{
  "success": true,
  "message": "订单已重置，可以重新支付"
}
```

---

## 6. 手动触发对账

**请求：**
```http
POST /reconcile
```

**预期响应：**
```json
{
  "success": true,
  "message": "对账完成"
}
```

---

## 7. 查询待处理的对账异常

**请求：**
```http
GET /reconcile/exceptions
```

**预期响应：**
```json
{
  "success": true,
  "data": [
    {
      "exceptionId": 1,
      "orderId": "ORD123",
      "billNo": "BILL456",
      "exceptionType": "PAY_SUCCESS_CORE_FAIL",
      "payAmt": 25000.00,
      "coreAmt": 0.00,
      "diffAmt": 25000.00,
      "status": "P",
      "createTime": "2026-01-01T10:00:00"
    }
  ],
  "count": 1
}
```

---

## 8. 人工处理对账异常

**请求：**
```http
POST /reconcile/handle?exceptionId=1&handleMethod=MANUAL&result=已核实，手动补账完成
```

**预期响应：**
```json
{
  "success": true,
  "message": "处理完成"
}
```

---

## 测试场景

### 场景1：正常放款流程
1. 发起提款申请（金额<30000，自动审核通过）
2. 系统自动创建订单
3. 模拟支付成功
4. 生成借据和还款计划

### 场景2：需要人工审核的放款
1. 发起提款申请（金额>30000）
2. 系统创建贷款登记，状态为待审核
3. 调用审核接口
4. 审核通过后生成借据和还款计划

### 场景3：放款支付失败后重提
1. 发起提款申请
2. 模拟支付失败
3. 订单状态变为F
4. 调用重试接口
5. 订单状态重置为U，可重新支付

### 场景4：正常还款流程
1. 发起还款请求
2. 模拟支付成功
3. 执行冲销逻辑（费-息-本）
4. 更新还款计划状态

### 场景5：还款支付失败
1. 发起还款请求
2. 模拟支付失败
3. 订单状态变为F
4. 不生成账务数据

### 场景6：对账-支付成功核心未入账
1. 模拟支付成功但核心未保存
2. 手动触发对账
3. 系统检测到异常
4. 自动或人工补账

### 场景7：对账-金额不一致
1. 模拟支付金额和记账金额不一致
2. 手动触发对账
3. 小额差异自动核销
4. 大额差异标记为人工处理
