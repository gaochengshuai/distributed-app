<template>
  <div class="dashboard">
    <div class="action-bar">
      <h2>订单管理中心</h2>
      <button @click="$emit('open-create')" class="btn btn-primary">+ 创建新订单</button>
    </div>

    <!-- 统计卡片 (保留原有结构，更新配色) -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-title">总订单数</div>
        <div class="stat-value">{{ payments.length }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-title">今日交易金额</div>
        <div class="stat-value amount">¥ {{ totalAmount }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-title">待处理订单</div>
        <div class="stat-value warning">0</div>
      </div>
    </div>

    <!-- 订单列表 -->
    <div class="table-container">
      <table v-if="paginatedPayments.length > 0">
        <thead>
          <tr>
            <th>订单号</th><th>金额 (元)</th><th>状态</th><th>创建时间</th><th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="pay in paginatedPayments" :key="pay.id">
            <td>{{ pay.orderNo }}</td>
            <td class="amount">¥ {{ pay.amount }}</td>
            <td><span :class="['status-badge', pay.status.toLowerCase()]">{{ pay.status }}</span></td>
            <td>{{ formatDate(pay.createdAt) }}</td>
            <td><button @click="$emit('open-edit', pay)" class="btn-link">修改</button></td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-state">
        <div class="empty-icon">📭</div>
        <p>暂无订单数据，请点击右上角创建</p>
      </div>

      <!-- 分页控件 -->
      <div class="pagination" v-if="totalPages > 1">
        <button @click="currentPage--" :disabled="currentPage === 1" class="page-btn">
          &lt; 上一页
        </button>
        <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
        <button @click="currentPage++" :disabled="currentPage === totalPages" class="page-btn">
          下一页 &gt;
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { usePaymentStore } from '@/stores/usePaymentStore'

const store = usePaymentStore()
const payments = computed(() => store.state.payments)
const totalAmount = computed(() => store.totalAmount)

// 分页逻辑
const currentPage = ref(1)
const pageSize = 10

const totalPages = computed(() => Math.ceil(payments.value.length / pageSize))

const paginatedPayments = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  const end = start + pageSize
  return payments.value.slice(start, end)
})

// 监听数据变化，如果删除了数据导致当前页超出范围，自动回退到最后一页
// 注意：这里简单处理，实际项目中可能需要更复杂的 watch
// 当 payments 长度变化时，检查 currentPage 是否越界
import { watch } from 'vue'
watch(payments, (newVal) => {
  const newTotalPages = Math.ceil(newVal.length / pageSize)
  if (currentPage.value > newTotalPages && newTotalPages > 0) {
    currentPage.value = newTotalPages
  } else if (newTotalPages === 0) {
    currentPage.value = 1
  }
})

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

defineEmits(['open-create', 'open-edit'])
</script>

<style scoped>
/* 仪表盘容器：透明背景，因为 App.vue 已经设置了浅灰背景 */
.dashboard {width: 100%; padding: 0; /* 移除内边距，让卡片直接贴合或通过 gap 控制 */ }
.action-bar {display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 1rem;}
.action-bar h2 {margin: 0; color: #094781; /* 深蓝色标题 */ font-size: 1.5rem;}
/* 统计卡片网格 */
.stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));gap: 1.5rem; margin-bottom: 2rem;}
/* 统计卡片：白色背景，阴影 */
.stat-card { 
  background: #ffffff; border: 1px solid #e8e8e8; border-radius: 8px; padding: 1.5rem; 
  text-align: center; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02); transition: transform 0.2s;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 8px rgba(0, 0, 0, 0.05);}
.stat-title { font-size: 0.9rem; color: #666666; margin-bottom: 0.5rem;}
.stat-value { font-size: 1.8rem; font-weight: 700;  color: #333333;}
.stat-value.amount { color: #1890ff; /* 品牌蓝 */ font-weight: 700;}
.stat-value.warning { color: #faad14; /* 警告橙 */ }
/* 表格容器：白色卡片 */
.table-container { 
  background: #ffffff; border: 1px solid #e8e8e8; border-radius: 8px; padding: 1.5rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02); overflow-x: auto; }
table { width: 100%; border-collapse: collapse; color: #333333; min-width: 600px;}
th, td { padding: 1rem; text-align: left; border-bottom: 1px solid #f0f0f0;}
th { background: #fafafa; font-weight: 600; color: #666666;font-size: 14px;}
td { font-size: 14px;}
.amount { 
  font-family: 'Courier New', monospace; 
  /* color: #1890ff;  */
  font-weight: 700;
}

/* 状态徽章 */
.status-badge { 
  padding: 0.25rem 0.6rem; 
  border-radius: 4px; 
  font-size: 0.8rem; 
  font-weight: 500; 
}

/* 根据不同状态设置颜色 */
.status-badge.created { 
  background: #e6f7ff; 
  color: #1890ff; 
  border: 1px solid #91d5ff;
}

.status-badge.success, .status-badge.paid { 
  background: #f6ffed; 
  color: #52c41a; 
  border: 1px solid #b7eb8f;
}

.status-badge.failed, .status-badge.cancelled { 
  background: #fff1f0; 
  color: #ff4d4f; 
  border: 1px solid #ffa39e;
}

/* 链接按钮 */
.btn-link { 
  background: none; 
  border: none; 
  color: #1890ff; 
  cursor: pointer; 
  font-size: 14px;
}

.btn-link:hover {
  text-decoration: underline;
}

/* 空状态 */
.empty-state { 
  text-align: center; 
  padding: 3rem; 
  color: #999999; 
}

.empty-icon { 
  font-size: 3rem; 
  margin-bottom: 1rem; 
  opacity: 0.5; 
}

/* 主要按钮 (创建订单) */
.btn-primary {
  background: #1890ff;
  color: white;
  padding: 0.6rem 1.2rem;
  border: none;
  border-radius: 4px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.3s;
}

.btn-primary:hover {
  background: #40a9ff;
}

/* 分页控件 */
.pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 1.5rem;
  gap: 1rem;
}

.page-btn {
  padding: 0.5rem 1rem;
  border: 1px solid #d9d9d9;
  background: #ffffff;
  border-radius: 4px;
  cursor: pointer;
  color: #333333;
  transition: all 0.3s;
}

.page-btn:hover:not(:disabled) {
  border-color: #1890ff;
  color: #1890ff;
}

.page-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  background: #f5f5f5;
}

.page-info {
  font-size: 14px;
  color: #666666;
}
</style>