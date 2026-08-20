<template>
  <aside :class="['sidebar', { 'sidebar-open': isOpen }]">
    <nav class="sidebar-nav">
      <!-- 订单管理 -->
      <div 
        :class="['nav-item', { active: activeMenu === 'orders' }]"
        @click="selectMenu('orders')"
      >
        <span class="icon">📋</span> 
        <span class="text">订单管理</span>
      </div>
      
      <!-- 提款管理 -->
      <div 
        :class="['nav-item', { active: activeMenu === 'withdraw' }]"
        @click="selectMenu('withdraw')"
      >
        <span class="icon">💰</span> 
        <span class="text">提款管理</span>
      </div>
      
      <!-- 还款管理 -->
      <div 
        :class="['nav-item', { active: activeMenu === 'repayment' }]"
        @click="selectMenu('repayment')"
      >
        <span class="icon">💳</span> 
        <span class="text">还款管理</span>
      </div>
      
      <!-- 对账管理 -->
      <div 
        :class="['nav-item', { active: activeMenu === 'reconciliation' }]"
        @click="selectMenu('reconciliation')"
      >
        <span class="icon">🔍</span> 
        <span class="text">对账管理</span>
      </div>
      
      <!-- 系统设置 -->
      <div 
        :class="['nav-item', { active: activeMenu === 'settings' }]"
        @click="selectMenu('settings')"
      >
        <span class="icon">⚙️</span> 
        <span class="text">系统设置</span>
      </div>
    </nav>
  </aside>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  isOpen: Boolean
})

const emit = defineEmits(['menu-change'])

// 当前激活的菜单项
const activeMenu = ref('orders')

// 选择菜单项
const selectMenu = (menu) => {
  activeMenu.value = menu
  emit('menu-change', menu)
}

// 监听外部传入的菜单变化（可选）
watch(() => props.isOpen, (newVal) => {
  // 可以在这里添加侧边栏打开/关闭时的逻辑
})
</script>

<style scoped>
/* 侧边栏容器：白色背景，圆角，阴影 */
.sidebar {
  width: 240px;
  background: #ffffff; /* 纯白背景 */
  border: 1px solid #e8e8e8; /* 浅灰边框 */
  border-radius: 8px;
  padding: 1.5rem 1rem;
  height: calc(100vh - 62px); /* 根据 navbar 高度调整 */
  position: sticky;
  top: 80px;
  transition: transform 0.3s ease, width 0.3s ease;
  overflow-y: auto;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05); /* 轻微阴影 */
}

.sidebar-nav { 
  display: flex; 
  flex-direction: column; 
  gap: 0.5rem; 
}

.nav-item {
  padding: 0.8rem 1rem; 
  border-radius: 4px; 
  cursor: pointer;
  transition: all 0.3s; 
  color: #666666; /* 默认中灰色文字 */
  display: flex; 
  align-items: center; 
  gap: 0.8rem;
  font-size: 16px;
  font-weight: 500;
}

/* 图标样式微调 */
.icon {
  font-size: 16px;
  width: 20px;
  text-align: center;
}

/* Hover 状态：浅灰背景 */
.nav-item:hover { 
  background: #f5f5f5; 
  color: #1890ff; /* Hover 时文字变蓝 */
}

/* 激活状态：品牌蓝左边框 + 浅蓝背景 */
.nav-item.active {
  background: #e6f7ff; /* 极浅蓝色背景 */
  color: #1890ff; /* 品牌蓝文字 */
  border-left: 3px solid #1890ff; /* 左侧指示条 */
  font-weight: 600;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed; 
    left: 0; 
    top: 60px; 
    height: calc(100vh - 60px);
    z-index: 90; 
    transform: translateX(-100%); 
    width: 240px;
    border-radius: 0; 
    border-right: 1px solid #e8e8e8;
    border-top: none;
    box-shadow: 2px 0 8px rgba(0,0,0,0.1);
  }
  
  .sidebar.sidebar-open { 
    transform: translateX(0); 
  }
}
</style>