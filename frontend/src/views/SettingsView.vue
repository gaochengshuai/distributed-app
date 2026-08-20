<template>
  <div class="settings-view">
    <div class="page-header">
      <h2>⚙️ 系统设置</h2>
    </div>

    <!-- 基础配置 -->
    <div class="section">
      <h3>基础配置</h3>
      <div class="card">
        <div class="config-item">
          <label>系统名称</label>
          <input type="text" value="Distributed Pay" disabled class="input-field" />
        </div>
        <div class="config-item">
          <label>版本号</label>
          <input type="text" value="v1.0.0" disabled class="input-field" />
        </div>
        <div class="config-item">
          <label>运行环境</label>
          <input type="text" value="Development" disabled class="input-field" />
        </div>
      </div>
    </div>

    <!-- 业务配置 -->
    <div class="section">
      <h3>业务配置</h3>
      <div class="card">
        <div class="config-item">
          <label>自动审批阈值</label>
          <div class="config-value">
            <span>¥ 30,000.00</span>
            <span class="hint">超过此金额需要人工审核</span>
          </div>
        </div>
        <div class="config-item">
          <label>对账任务执行时间</label>
          <div class="config-value">
            <span>每天凌晨 2:00</span>
            <span class="hint">Cron: 0 0 2 * * ?</span>
          </div>
        </div>
        <div class="config-item">
          <label>小额差异容差</label>
          <div class="config-value">
            <span>¥ 1.00</span>
            <span class="hint">小于等于此金额的差异自动核销</span>
          </div>
        </div>
        <div class="config-item">
          <label>大额溢缴款阈值</label>
          <div class="config-value">
            <span>¥ 100.00</span>
            <span class="hint">超过此金额的溢缴款记入溢缴款账户</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 系统信息 -->
    <div class="section">
      <h3>系统信息</h3>
      <div class="card">
        <div class="info-item">
          <label>Java 版本</label>
          <span>Java 17</span>
        </div>
        <div class="info-item">
          <label>Spring Boot 版本</label>
          <span>3.5.14</span>
        </div>
        <div class="info-item">
          <label>数据库</label>
          <span>MySQL 8.4.0</span>
        </div>
        <div class="info-item">
          <label>消息队列</label>
          <span>RabbitMQ</span>
        </div>
      </div>
    </div>

    <!-- 关于 -->
    <div class="section">
      <h3>关于</h3>
      <div class="card about-card">
        <p><strong>Distributed Loan Payment System</strong></p>
        <p>分布式贷款支付系统 v1.0</p>
        <p class="copyright">© 2026 Alibaba Cloud Technical Team</p>
      </div>
    </div>
  </div>
</template>

<script setup>
// 系统设置视图 - 展示系统配置和信息
</script>

<style scoped>
.settings-view {
  width: 100%;
  max-width: 900px;
}

.page-header {
  margin-bottom: 1.5rem;
}

.page-header h2 {
  margin: 0;
  color: #094781;
  font-size: 1.5rem;
}

.section {
  margin-bottom: 2rem;
}

.section h3 {
  color: #333333;
  margin-bottom: 1rem;
  font-size: 1.1rem;
}

.card {
  background: #ffffff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
}

.config-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 0;
  border-bottom: 1px solid #f0f0f0;
}

.config-item:last-child {
  border-bottom: none;
}

.config-item label {
  font-weight: 500;
  color: #666666;
  min-width: 150px;
}

.input-field {
  background: #f5f5f5;
  border: 1px solid #dcdcdc;
  color: #999999;
  padding: 0.6rem 1rem;
  border-radius: 4px;
  outline: none;
  width: 200px;
}

.config-value {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.3rem;
}

.config-value span:first-child {
  color: #333333;
  font-weight: 500;
}

.hint {
  font-size: 12px;
  color: #999999;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 0.8rem 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item label {
  font-weight: 500;
  color: #666666;
}

.info-item span {
  color: #333333;
}

.about-card {
  text-align: center;
}

.about-card p {
  margin: 0.5rem 0;
  color: #666666;
}

.about-card p:first-child {
  font-size: 1.2rem;
  color: #094781;
}

.copyright {
  margin-top: 1rem;
  font-size: 12px;
  color: #999999;
}
</style>
