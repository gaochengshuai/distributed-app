<template>
  <div class="repayment-view">
    <div class="page-header">
      <h2>💳 还款管理</h2>
      <button @click="showRepayModal = true" class="btn btn-primary">+ 发起还款</button>
    </div>

    <!-- 还款记录列表 -->
    <div class="section">
      <h3>还款记录</h3>
      <div class="table-container">
        <table v-if="repayRecords.length > 0">
          <thead>
            <tr>
              <th>订单号</th>
              <th>借据号</th>
              <th>还款金额</th>
              <th>还款类型</th>
              <th>支付状态</th>
              <th>还款时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="record in repayRecords" :key="record.orderId">
              <td>{{ record.orderId }}</td>
              <td>{{ record.billNo }}</td>
              <td class="amount">¥ {{ record.repayAmt }}</td>
              <td>{{ getRepayTypeText(record.repayType) }}</td>
              <td>
                <span :class="['status-badge', record.payStatus.toLowerCase()]">
                  {{ getPayStatusText(record.payStatus) }}
                </span>
              </td>
              <td>{{ formatDate(record.payTime) }}</td>
              <td>
                <button 
                  v-if="record.payStatus === 'F'" 
                  @click="handleRetry(record.orderId)" 
                  class="btn-link"
                >
                  重试
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-state">
          <p>暂无还款记录</p>
        </div>
      </div>
    </div>

    <!-- 失败订单列表 -->
    <div class="section">
      <h3>支付失败订单（可重试）</h3>
      <div class="table-container">
        <table v-if="failedOrders.length > 0">
          <thead>
            <tr>
              <th>订单号</th>
              <th>借据号</th>
              <th>订单金额</th>
              <th>失败原因</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in failedOrders" :key="order.orderId">
              <td>{{ order.orderId }}</td>
              <td>{{ order.billNo || '-' }}</td>
              <td class="amount">¥ {{ order.orderAmt }}</td>
              <td>{{ order.failReason || '未知' }}</td>
              <td>{{ formatDate(order.createTime) }}</td>
              <td>
                <button @click="handleRetry(order.orderId)" class="btn-link">重试</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-state">
          <p>暂无失败的订单</p>
        </div>
      </div>
    </div>

    <!-- 发起还款弹窗 -->
    <div v-if="showRepayModal" class="modal-overlay" @click.self="showRepayModal = false">
      <div class="modal-content">
        <h3>发起还款</h3>
        <div class="form-group">
          <label>借据号</label>
          <input v-model="repayForm.billNo" class="input-field" placeholder="例如: BILL1704067200000" />
        </div>
        <div class="form-group">
          <label>还款金额</label>
          <input v-model.number="repayForm.amount" type="number" step="0.01" class="input-field" placeholder="0.00" />
        </div>
        <div class="form-group">
          <label>还款类型</label>
          <select v-model="repayForm.repayType" class="input-field">
            <option value="NORMAL">正常还款</option>
            <option value="EARLY">提前还款</option>
            <option value="OVERDUE">逾期还款</option>
          </select>
        </div>
        <div class="modal-actions">
          <button @click="showRepayModal = false" class="btn btn-secondary">取消</button>
          <button @click="submitRepay" :disabled="store.state.loading" class="btn btn-primary">
            {{ store.state.loading ? '提交中...' : '提交' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { usePaymentStore } from '@/stores/usePaymentStore'
import { loanApi, paymentApi } from '@/api/paymentApi'

const store = usePaymentStore()

// 还款记录列表
const repayRecords = ref([])

// 失败订单列表
const failedOrders = ref([])

// 显示还款弹窗
const showRepayModal = ref(false)

// 还款表单
const repayForm = reactive({
  billNo: '',
  amount: 0,
  repayType: 'NORMAL'
})

// 加载还款记录
const loadRepayRecords = async () => {
  try {
    // TODO: 实现获取还款记录的API
    const res = await loanApi.getRepayRecords()
    repayRecords.value = res.data
  } catch (e) {
    console.error('加载还款记录失败', e)
  }
}

// 加载失败订单列表
const loadFailedOrders = async () => {
  try {
    const res = await paymentApi.getPayments()
    failedOrders.value = res.data.filter(order => 
      (order.status === 'F' || order.orderStatus === 'F') && order.orderType === 'REPAY'
    )
  } catch (e) {
    console.error('加载失败订单失败', e)
  }
}

// 重试还款
const handleRetry = async (orderId) => {
  if (!confirm('确认重试此还款订单？')) return
  
  try {
    const res = await loanApi.retryRepay(orderId)
    if (res.data.success) {
      store.showToast('订单已重置，可以重新支付')
      loadFailedOrders()
      loadRepayRecords()
    } else {
      store.showToast(res.data.message, 'error')
    }
  } catch (e) {
    store.showToast('重试失败: ' + e.message, 'error')
  }
}

// 提交还款申请
const submitRepay = async () => {
  if (!repayForm.billNo || !repayForm.amount) {
    store.showToast('请填写完整信息', 'error')
    return
  }
  
  store.setLoading(true)
  try {
    const params = {
      billNo: repayForm.billNo,
      amount: repayForm.amount.toString(),
      repayType: repayForm.repayType
    }
    
    const res = await loanApi.repay(params)
    if (res.data.success) {
      store.showToast('还款处理中，订单号: ' + res.data.orderId)
      showRepayModal.value = false
      // 重置表单
      Object.assign(repayForm, {
        billNo: '',
        amount: 0,
        repayType: 'NORMAL'
      })
      loadRepayRecords()
    } else {
      store.showToast(res.data.message, 'error')
    }
  } catch (e) {
    store.showToast('提交失败: ' + e.message, 'error')
  } finally {
    store.setLoading(false)
  }
}

// 获取还款类型文本
const getRepayTypeText = (type) => {
  const map = {
    'NORMAL': '正常还款',
    'EARLY': '提前还款',
    'OVERDUE': '逾期还款'
  }
  return map[type] || type
}

// 获取支付状态文本
const getPayStatusText = (status) => {
  const map = {
    'S': '成功',
    'F': '失败',
    'P': '处理中'
  }
  return map[status] || status
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

onMounted(() => {
  loadRepayRecords()
  loadFailedOrders()
})
</script>

<style scoped>
.repayment-view {
  width: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.table-container {
  background: #ffffff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  color: #333333;
  min-width: 600px;
}

th, td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

th {
  background: #fafafa;
  font-weight: 600;
  color: #666666;
  font-size: 14px;
}

td {
  font-size: 14px;
}

.amount {
  font-family: 'Courier New', monospace;
  font-weight: 700;
  color: #1890ff;
}

.status-badge {
  padding: 0.25rem 0.6rem;
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: 500;
}

.status-badge.s {
  background: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.status-badge.f {
  background: #fff1f0;
  color: #ff4d4f;
  border: 1px solid #ffa39e;
}

.status-badge.p {
  background: #e6f7ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
}

.empty-state {
  text-align: center;
  padding: 2rem;
  color: #999999;
}

.btn-link {
  background: none;
  border: none;
  color: #1890ff;
  cursor: pointer;
  font-size: 14px;
  padding: 0.3rem 0.6rem;
  border-radius: 4px;
  transition: all 0.3s;
}

.btn-link:hover {
  background: #e6f7ff;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(2px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  width: 450px;
  max-width: 90%;
  background: #ffffff;
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
  margin-bottom: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-group label {
  font-size: 14px;
  color: #666666;
  font-weight: 500;
}

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

.btn-primary {
  background: #1890ff;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #40a9ff;
}

.btn-secondary {
  background: #ffffff;
  border: 1px solid #d9d9d9;
  color: #666666;
}

.btn-secondary:hover:not(:disabled) {
  border-color: #1890ff;
  color: #1890ff;
}

@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
</style>
