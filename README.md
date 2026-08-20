# Distributed App (Generated)

要求: Java 17+, Maven, Node 18+, Docker Desktop

生成后包名: distributed-app.zip

快速启动（PowerShell）:
1. 启动 MySQL & RabbitMQ:
   docker compose up -d

2. 在各模块目录分别启动（或用多个终端）：
   # system-service
   cd distributed-app/system-service
   mvn spring-boot:run

   # payment-service
   cd ../payment-service
   mvn spring-boot:run

   # message-service
   cd ../message-service
   mvn spring-boot:run

   # report-service
   cd ../report-service
   mvn spring-boot:run

   # gateway
   cd ../gateway
   mvn spring-boot:run

3. 前端:
   cd distributed-app/frontend
   npm install
   npm run dev

src/
├── api/                # 数据层：API 请求封装
│   └── paymentApi.js   # 包含支付和贷款业务API
├── stores/             # 数据层：状态管理 (模拟 Pinia/Vuex)
│   └── usePaymentStore.js
├── composables/        # 逻辑层：组合式函数 (业务逻辑)
│   └── usePaymentLogic.js
├── views/              # 视图层：页面组件
│   ├── LoginView.vue           # 登录页面
│   ├── DashboardView.vue       # 订单管理（原仪表盘）
│   ├── WithdrawView.vue        # 提款管理（新增）
│   ├── RepaymentView.vue       # 还款管理（新增）
│   ├── ReconciliationView.vue  # 对账管理（新增）
│   └── SettingsView.vue        # 系统设置（新增）
├── components/         # 视图层：通用组件
│   └── Sidebar.vue     # 侧边栏菜单（已完善）
└── App.vue             # 入口组件：布局容器

功能模块说明:
1. **订单管理** - 查看所有订单列表、创建和修改订单
2. **提款管理** - 发起提款申请、人工审核、重试失败订单
3. **还款管理** - 发起还款、重试还款、查看还款记录
4. **对账管理** - 手动触发对账、查看异常记录、人工处理异常
5. **系统设置** - 查看系统配置和业务参数

说明:
- 登录/注册: http://localhost:5173 页面通过 gateway 调用 system-service 的 /api/auth/login 和 /api/auth/register
- 支付 API 经过 gateway: POST http://localhost:8080/api/payments
- 贷款业务 API: http://localhost:8080/api/loan/*
  - 提款: POST /api/loan/withdraw
  - 审核: POST /api/loan/approve/{loanRegId}
  - 重试: POST /api/loan/retry/{orderId}
  - 还款: POST /api/loan/repay
  - 对账: POST /api/loan/reconcile
  - 异常查询: GET /api/loan/reconcile/exceptions
  - 异常处理: POST /api/loan/reconcile/handle
- JWT secret 配置在 gateway 和 system-service 的 application.yml（示例中使用固定 secret），生产请更换并安全存储。

payment-service
   # 业务场景
   客户：张三（custId: CUST001）
   产品：现金贷（productCd: CASH_LOAN_001）
   提款金额：50,000元
   放款模式：L（放款到客户账户）
   是否需要审核：是（超过自动审批限额30,000元）
   # 一、第一阶段：预处理（WithdrawService）