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
│   └── paymentApi.js
├── stores/             # 数据层：状态管理 (模拟 Pinia/Vuex)
│   └── usePaymentStore.js
├── composables/        # 逻辑层：组合式函数 (业务逻辑)
│   └── usePaymentLogic.js
├── views/              # 视图层：页面组件
│   ├── LoginView.vue
│   └── DashboardView.vue
├── components/         # 视图层：通用组件
│   ├── Sidebar.vue
│   ├── StatsCard.vue
│   └── PaymentTable.vue
└── App.vue             # 入口组件：布局容器

说明:
- 登录/注册: http://localhost:5173 页面通过 gateway 调用 system-service 的 /api/auth/login 和 /api/auth/register
- 支付 API 经过 gateway: POST http://localhost:8080/api/payments
- JWT secret 配置在 gateway 和 system-service 的 application.yml（示例中使用固定 secret），生产请更换并安全存储。

payment-service
   # 业务场景
   客户：张三（custId: CUST001）
   产品：现金贷（productCd: CASH_LOAN_001）
   提款金额：50,000元
   放款模式：L（放款到客户账户）
   是否需要审核：是（超过自动审批限额30,000元）
   # 一、第一阶段：预处理（WithdrawService）