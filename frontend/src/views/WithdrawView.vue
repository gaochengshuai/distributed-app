<template>
  <div class="withdraw-view">
    <div class="page-header">
      <h2>💰 提款管理</h2>
      <button @click="showWithdrawModal = true" class="btn btn-primary">+ 发起提款</button>
    </div>

    <!-- 页签导航 -->
    <div class="tabs-container">
      <div 
        :class="['tab-item', { active: activeTab === 'pending' }]"
        @click="activeTab = 'pending'"
      >
        待审核提款
        <span v-if="pendingWithdrawals.length > 0" class="badge">{{ pendingWithdrawals.length }}</span>
      </div>
      <div 
        :class="['tab-item', { active: activeTab === 'failed' }]"
        @click="activeTab = 'failed'"
      >
        支付失败订单
        <span v-if="failedOrders.length > 0" class="badge">{{ failedOrders.length }}</span>
      </div>
    </div>

    <!-- 待审核列表 -->
    <div v-if="activeTab === 'pending'" class="section">
      <div class="table-container">
        <table>
          <thead>
            <tr>
              <th>贷款登记ID</th>
              <th>客户ID</th>
              <th>合同号</th>
              <th>提款金额</th>
              <th>申请时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in pendingWithdrawals" :key="item.loanRegId">
              <td>{{ item.loanRegId }}</td>
              <td>{{ item.custId }}</td>
              <td>{{ item.contrNo }}</td>
              <td class="amount">¥ {{ item.withdrawAmt }}</td>
              <td>{{ formatDate(item.createTime) }}</td>
              <td>
                <button @click="handleApprove(item.loanRegId)" class="btn-link success">审核通过</button>
              </td>
            </tr>
            <tr v-if="pendingWithdrawals.length === 0">
              <td colspan="6" class="empty-state-cell">
                <div class="empty-state">
                  <p>暂无待审核的提款申请</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 失败订单列表 -->
    <div v-if="activeTab === 'failed'" class="section">
      <div class="table-container">
        <table>
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
            <tr v-if="failedOrders.length === 0">
              <td colspan="6" class="empty-state-cell">
                <div class="empty-state">
                  <p>暂无失败的订单</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 发起提款弹窗 -->
    <div v-if="showWithdrawModal" class="modal-overlay" @click.self="showWithdrawModal = false">
      <div class="modal-content">
        <h3>发起提款申请</h3>
        <div class="form-group">
          <label>外部业务订单号</label>
          <input v-model="withdrawForm.exBizOrderId" class="input-field" placeholder="例如: EXT_20260101_001" />
        </div>
        <div class="form-group">
          <label>提款金额</label>
          <input v-model.number="withdrawForm.withdrawAmt" type="number" step="0.01" class="input-field" placeholder="0.00" />
        </div>
        <div class="form-group">
          <label>手续费率</label>
          <input v-model.number="withdrawForm.withdrawFee" type="number" step="0.0001" class="input-field" placeholder="0.005" />
        </div>
        <div class="form-group">
          <label>贷款本金</label>
          <input v-model.number="withdrawForm.loanPrin" type="number" step="0.01" class="input-field" placeholder="0.00" />
        </div>
        <div class="form-group">
          <label>贷款期数</label>
          <input v-model.number="withdrawForm.loanTerm" type="number" class="input-field" placeholder="12" />
        </div>
        <div class="form-group">
          <label>还款方式</label>
          <select v-model="withdrawForm.repayType" class="input-field">
            <option value="AT">等额本息</option>
            <option value="AC">等额本金</option>
            <option value="IO">先息后本</option>
          </select>
        </div>
        <div class="form-group">
          <label>申请人ID</label>
          <input v-model="withdrawForm.applyUserId" class="input-field" placeholder="例如: CUST001" />
        </div>
        <div class="form-group">
          <label>申请人姓名</label>
          <input v-model="withdrawForm.applyUserName" class="input-field" placeholder="例如: 张三" />
        </div>
        <div class="modal-actions">
          <button @click="showWithdrawModal = false" class="btn btn-secondary">取消</button>
          <button @click="submitWithdraw" :disabled="store.state.loading" class="btn btn-primary">
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

// 待审核提款列表
const pendingWithdrawals = ref([])

// 失败订单列表
const failedOrders = ref([])

// 显示提款弹窗
const showWithdrawModal = ref(false)

// 提款表单
const withdrawForm = reactive({
  exBizOrderId: '',
  withdrawAmt: 0,
  withdrawFee: 0.005,
  loanPrin: 0,
  loanTerm: 12,
  repayType: 'AT',
  applyUserId: '',
  applyUserName: ''
})

// 当前激活的页签
const activeTab = ref('pending')

// 加载待审核列表
const loadPendingWithdrawals = async () => {
  try {
    // TODO: 实现获取待审核列表的API
    // const res = await axios.get('http://localhost:8080/api/loan/pending')
    // pendingWithdrawals.value = res.data
    pendingWithdrawals.value = []
  } catch (e) {
    console.error('加载待审核列表失败', e)
  }
}

// 加载失败订单列表
const loadFailedOrders = async () => {
  try {
    const res = await paymentApi.getPayments()
    failedOrders.value = res.data.filter(order => order.status === 'F' || order.orderStatus === 'F')
  } catch (e) {
    console.error('加载失败订单失败', e)
  }
}

// 审核通过
const handleApprove = async (loanRegId) => {
  if (!confirm('确认审核通过此提款申请？')) return
  
  try {
    const res = await loanApi.approve(loanRegId)
    if (res.data.success) {
      store.showToast('审核通过，借据号: ' + res.data.billNo)
      loadPendingWithdrawals()
    } else {
      store.showToast(res.data.message, 'error')
    }
  } catch (e) {
    store.showToast('审核失败: ' + e.message, 'error')
  }
}

// 重试订单
const handleRetry = async (orderId) => {
  if (!confirm('确认重试此订单？')) return
  
  try {
    const res = await loanApi.retryWithdraw(orderId)
    if (res.data.success) {
      store.showToast('订单已重置，可以重新支付')
      loadFailedOrders()
    } else {
      store.showToast(res.data.message, 'error')
    }
  } catch (e) {
    store.showToast('重试失败: ' + e.message, 'error')
  }
}

// 提交提款申请
const submitWithdraw = async () => {
  if (!withdrawForm.exBizOrderId || !withdrawForm.withdrawAmt || !withdrawForm.applyUserId) {
    store.showToast('请填写完整信息', 'error')
    return
  }
  
  store.setLoading(true)
  try {
    const res = await loanApi.withdraw(withdrawForm)
    if (res.data.success) {
      store.showToast('提款申请已提交，订单号: ' + res.data.orderId)
      showWithdrawModal.value = false
      // 重置表单
      Object.assign(withdrawForm, {
        exBizOrderId: '',
        withdrawAmt: 0,
        withdrawFee: 0.005,
        loanPrin: 0,
        loanTerm: 12,
        repayType: 'AT',
        applyUserId: '',
        applyUserName: ''
      })
      loadPendingWithdrawals()
    } else {
      store.showToast(res.data.message, 'error')
    }
  } catch (e) {
    store.showToast('提交失败: ' + e.message, 'error')
  } finally {
    store.setLoading(false)
  }
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

onMounted(() => {
  loadPendingWithdrawals()
  loadFailedOrders()
})
</script>

<style scoped>
.withdraw-view {
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

.tabs-container {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
  border-bottom: 2px solid #e8e8e8;
  padding-bottom: 0;
}

.tab-item {
  padding: 0.8rem 1.5rem;
  border: none;
  border-bottom: 3px solid transparent;
  border-radius: 4px 4px 0 0;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
  color: #666666;
  background: transparent;
  position: relative;
  margin-bottom: -2px;
}

.tab-item:hover {
  color: #1890ff;
  background: #f5f5f5;
}

.tab-item.active {
  color: #1890ff;
  border-bottom-color: #1890ff;
  background: #ffffff;
  font-weight: 600;
}

.badge {
  display: inline-block;
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
  white-space: nowrap;
  border-radius: 9px;
  background: #ff4d4f;
  color: white;
  margin-left: 6px;
  font-weight: 500;
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

.empty-state-cell {
  text-align: center;
  padding: 3rem 1rem !important;
}

.empty-state {
  text-align: center;
  padding: 2rem;
  color: #999999;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

.btn-link {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  padding: 0.3rem 0.6rem;
  border-radius: 4px;
  transition: all 0.3s;
}

.btn-link.success {
  color: #52c41a;
}

.btn-link.success:hover {
  background: #f6ffed;
}

.btn-link:not(.success) {
  color: #1890ff;
}

.btn-link:not(.success):hover {
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
  width: 500px;
  max-width: 90%;
  background: #ffffff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 2rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  animation: slideUp 0.3s ease-out;
  color: #333333;
  max-height: 90vh;
  overflow-y: auto;
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
