<template>
  <div class="tech-bg">
    <!-- 顶部导航栏 -->
    <header class="navbar">
      <div class="logo-area">
        <button v-if="store.state.token" @click="store.toggleSidebar" class="menu-toggle">☰</button>
        <div class="logo">🚀 Distributed Pay</div>
      </div>
      <div class="user-profile" v-if="store.state.token">
        <span class="username">{{ store.state.username }}</span>
        <button @click="handleLogout" class="btn-logout">退出</button>
      </div>
      <div class="user-profile" v-else>
        <span class="guest-text">未登录</span>
      </div>
    </header>

    <div class="main-layout">
      <!-- 侧边栏 -->
      <Sidebar v-if="store.state.token" :is-open="store.state.sidebarOpen" @menu-change="handleMenuChange"/>

      <!-- 主内容区域 -->
      <main class="content-area">
        <!-- Toast -->
        <transition name="fade">
          <div v-if="store.state.toast.show" :class="['toast', store.state.toast.type]">
            {{ store.state.toast.message }}
          </div>
        </transition>

        <!-- 登录视图 -->
        <LoginView v-if="!store.state.token" />

        <!-- 订单管理视图 -->
        <DashboardView v-else-if="currentView === 'orders'" @open-create="openModal('create')" 
          @open-edit="openModal('edit', $event)"/>

        <!-- 提款管理视图 -->
        <WithdrawView v-else-if="currentView === 'withdraw'"/>

        <!-- 还款管理视图 -->
        <RepaymentView v-else-if="currentView === 'repayment'"/>

        <!-- 对账管理视图 -->
        <ReconciliationView v-else-if="currentView === 'reconciliation'"/>

        <!-- 系统设置视图 -->
        <SettingsView v-else-if="currentView === 'settings'"/>
      </main>
    </div>

    <!-- 弹窗 Modal (保留在 App 中，因为它是全局覆盖层) -->
    <div v-if="modal.visible" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <h3>{{ modal.type === 'create' ? '创建支付订单' : '修改订单' }}</h3>
        <div class="form-group">
          <label>订单号</label>
          <input v-model='modalForm.orderNo' :disabled="modal.type === 'edit'" class="input-field" placeholder="例如: ORD-2026-001" />
        </div>
        <div class="form-group">
          <label>金额</label>
          <input v-model.number='modalForm.amount' type="number" step="0.01" class="input-field" placeholder="0.00" />
        </div>
        <div class="modal-actions">
          <button @click="closeModal" class="btn btn-secondary">取消</button>
          <button @click="handleSubmit" :disabled="store.state.loading" class="btn btn-primary">
            {{ store.state.loading ? '提交中...' : '确认' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted, ref } from 'vue'
import Sidebar from './components/Sidebar.vue'
import LoginView from './views/LoginView.vue'
import DashboardView from './views/DashboardView.vue'
import WithdrawView from './views/WithdrawView.vue'
import RepaymentView from './views/RepaymentView.vue'
import ReconciliationView from './views/ReconciliationView.vue'
import SettingsView from './views/SettingsView.vue'
import { usePaymentStore } from './stores/usePaymentStore'
import { usePaymentLogic } from './composables/usePaymentLogic'

const store = usePaymentStore()
const { fetchPayments, submitPayment } = usePaymentLogic()

// 当前视图状态
const currentView = ref('orders')

// 弹窗状态
const modal = reactive({ visible: false, type: 'create' })
const modalForm = reactive({ id: null, orderNo: '', amount: 0 })

onMounted(() => {
  if (store.state.token) {
    fetchPayments()
  }
  // 移动端默认关闭侧边栏
  if (window.innerWidth < 768) {
    store.state.sidebarOpen = false
  }
})

const handleLogout = () => {
  store.logout()
  store.showToast('已退出')
}

// 处理菜单切换
const handleMenuChange = (menu) => {
  currentView.value = menu
  // 如果切换到订单管理，刷新数据
  if (menu === 'orders') {
    fetchPayments()
  }
}

const openModal = (type, item = null) => {
  modal.type = type
  if (type === 'edit' && item) {
    Object.assign(modalForm, item)
  } else {
    Object.assign(modalForm, { id: null, orderNo: 'ORD-' + Date.now(), amount: 0 })
  }
  modal.visible = true
}

const closeModal = () => {
  modal.visible = false
}

const handleSubmit = async () => {
  const success = await submitPayment(modalForm, modal.type)
  if (success) {
    closeModal()
  }
}
</script>


<style scoped>
/* 全局容器：浅灰背景，深色文字 */
.tech-bg {
  min-height: 100vh; 
  background-color: #f0f2f5; /* 浅灰背景，衬托白色卡片 */
  color: #333333; /* 全局深色文字 */
  font-family: 'Inter', sans-serif; 
  position: relative; 
  overflow-x: hidden;
  display: flex; 
  flex-direction: column;
}

/* 顶部导航栏：白色背景，底部阴影，专业感 */
.navbar {
  display: flex; 
  justify-content: space-between; 
  align-items: center;
  padding: 0 2rem; 
  background: #ffffff; /* 纯白背景 */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05); /* 轻微阴影 */
  border-bottom: 1px solid #e8e8e8;
  position: sticky; 
  top: 0; 
  z-index: 100; 
  height: 60px;
}

.logo-area { display: flex; align-items: center; gap: 1rem; }

/* 菜单按钮：深色图标 */
.menu-toggle { 
  background: none; 
  border: none; 
  color: #333333; 
  font-size: 1.5rem; 
  cursor: pointer; 
  display: none; 
}

/* Logo：保持品牌渐变蓝，但在白底上更清晰 */
.logo { 
  font-size: 1.5rem; 
  font-weight: 700; 
  background: linear-gradient(to right, #1890ff, #094781); 
  -webkit-background-clip: text; 
  background-clip: text; 
  color: transparent; 
}

.user-profile { display: flex; align-items: center; gap: 1rem; }

/* 用户名：深灰色 */
.username { 
  font-weight: 500; 
  color: #666666; 
}

/* 退出按钮：危险操作，保留红色但调整样式 */
.btn-logout { 
  background: transparent; 
  border: 1px solid #ff4d4f; 
  color: #ff4d4f; 
  padding: 0.4rem 1rem; 
  border-radius: 4px; 
  cursor: pointer; 
  transition: all 0.3s; 
  font-size: 14px;
}

.btn-logout:hover { 
  background: #ff4d4f; 
  color: white; 
}

/* 主布局：确保内容区有适当间距 */
.main-layout { 
  display: flex; 
  flex: 1; 
  width: 100%; 
  max-width: 1400px; 
  margin: 0 auto; 
  padding: 1.5rem; 
  gap: 1.5rem; 
  box-sizing: border-box; 
}

.content-area { 
  flex: 1; 
  min-width: 0; 
}

/* 模态框覆盖层 */
.modal-overlay { 
  position: fixed; 
  top: 0; 
  left: 0; 
  right: 0; 
  bottom: 0; 
  background: rgba(0, 0, 0, 0.45); /* 半透明黑 */
  backdrop-filter: blur(2px); 
  display: flex; 
  justify-content: center; 
  align-items: center; 
  z-index: 1000; 
}

/* 模态框内容：白色卡片风格 */
.modal-content { 
  width: 400px; 
  max-width: 90%; 
  background: #ffffff; /* 纯白背景 */
  border: 1px solid #e8e8e8;
  border-radius: 8px; 
  padding: 2rem; 
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  animation: slideUp 0.3s ease-out; 
  color: #333333;
}

.modal-content h3 {
  margin-top: 0;
  color: #094781;
  font-weight: 600;
}

.form-group { 
  margin-bottom: 1.5rem; 
  display: flex; 
  flex-direction: column; 
  gap: 0.5rem; 
}

/* 模态框内的标签 */
.form-group label {
  font-size: 14px;
  color: #666666;
  font-weight: 500;
}

/* 输入框：白底灰边，聚焦蓝 */
.input-field { 
  background: #ffffff; 
  border: 1px solid #dcdcdc; 
  color: #333333; 
  padding: 0.8rem; 
  border-radius: 4px; 
  outline: none; 
  transition: all 0.3s;
}

.input-field:focus {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.modal-actions { 
  display: flex; 
  gap: 1rem; 
  margin-top: 1.5rem; 
}

/* 按钮基础样式 */
.btn { 
  padding: 0.8rem 1.5rem; 
  border: none; 
  border-radius: 4px; 
  font-weight: 500; 
  cursor: pointer; 
  transition: all 0.3s;
  font-size: 14px;
}

.btn:disabled { 
  opacity: 0.6; 
  cursor: not-allowed; 
}

/* 主按钮：品牌蓝 */
.btn-primary { 
  background: #1890ff; 
  color: white; 
}

.btn-primary:hover:not(:disabled) {
  background: #40a9ff;
}

/* 次要按钮：白底蓝边 */
.btn-secondary { 
  background: #ffffff; 
  border: 1px solid #d9d9d9; 
  color: #666666; 
}

.btn-secondary:hover:not(:disabled) {
  border-color: #1890ff;
  color: #1890ff;
}

/* Toast 提示 */
.toast { 
  position: fixed; 
  top: 20px; 
  right: 20px; 
  padding: 1rem 2rem; 
  border-radius: 4px; 
  color: white; 
  font-weight: 500; 
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15); 
  z-index: 2000; 
}

.toast.success { background: #52c41a; } /* 绿色 */
.toast.error { background: #ff4d4f; }   /* 红色 */

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

@keyframes slideUp { 
  from { transform: translateY(20px); opacity: 0; } 
  to { transform: translateY(0); opacity: 1; } 
}

@media (max-width: 768px) {
  .menu-toggle { display: block; }
  .main-layout { padding: 0.5rem; gap: 0.5rem; }
  .navbar { padding: 0 1rem; }
}
</style>