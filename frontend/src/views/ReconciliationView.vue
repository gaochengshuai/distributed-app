<template>
  <div class="reconciliation-view">
    <div class="page-header">
      <h2>🔍 对账管理</h2>
      <button @click="handleReconcile" :disabled="store.state.loading" class="btn btn-primary">
        {{ store.state.loading ? '对账中...' : '手动触发对账' }}
      </button>
    </div>

    <!-- 对账异常列表 -->
    <div class="section">
      <h3>待处理对账异常</h3>
      <div class="table-container">
        <table>
          <thead>
            <tr>
              <th>异常ID</th>
              <th>订单号</th>
              <th>借据号</th>
              <th>异常类型</th>
              <th>支付金额</th>
              <th>核心金额</th>
              <th>差异金额</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="ex in exceptions" :key="ex.exceptionId">
              <td>{{ ex.exceptionId }}</td>
              <td>{{ ex.orderId }}</td>
              <td>{{ ex.billNo || '-' }}</td>
              <td>{{ getExceptionTypeText(ex.exceptionType) }}</td>
              <td class="amount">¥ {{ ex.payAmt }}</td>
              <td class="amount">¥ {{ ex.coreAmt }}</td>
              <td :class="['amount', ex.diffAmt > 0 ? 'positive' : 'negative']">
                ¥ {{ ex.diffAmt }}
              </td>
              <td>
                <span :class="['status-badge', ex.status.toLowerCase()]">
                  {{ getStatusText(ex.status) }}
                </span>
              </td>
              <td>{{ formatDate(ex.createTime) }}</td>
              <td>
                <button 
                  v-if="ex.status === 'P'" 
                  @click="showHandleModal(ex)" 
                  class="btn-link"
                >
                  处理
                </button>
              </td>
            </tr>
            <tr v-if="exceptions.length === 0">
              <td colspan="10" class="empty-state-cell">
                <div class="empty-state">
                  <p>暂无待处理的对账异常</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 处理异常弹窗 -->
    <div v-if="showHandleDialog" class="modal-overlay" @click.self="closeHandleModal">
      <div class="modal-content">
        <h3>处理对账异常</h3>
        <div class="info-row">
          <label>异常ID:</label>
          <span>{{ currentException.exceptionId }}</span>
        </div>
        <div class="info-row">
          <label>订单号:</label>
          <span>{{ currentException.orderId }}</span>
        </div>
        <div class="info-row">
          <label>异常类型:</label>
          <span>{{ getExceptionTypeText(currentException.exceptionType) }}</span>
        </div>
        <div class="form-group">
          <label>处理方式</label>
          <select v-model="handleForm.handleMethod" class="input-field">
            <option value="AUTO_RETRY">自动重试</option>
            <option value="MANUAL">人工处理</option>
            <option value="WRITE_OFF">核销</option>
            <option value="REVERSAL">冲正</option>
          </select>
        </div>
        <div class="form-group">
          <label>处理结果说明</label>
          <textarea 
            v-model="handleForm.result" 
            class="input-field textarea" 
            placeholder="请输入处理结果说明..."
            rows="4"
          ></textarea>
        </div>
        <div class="modal-actions">
          <button @click="closeHandleModal" class="btn btn-secondary">取消</button>
          <button @click="submitHandle" :disabled="store.state.loading" class="btn btn-primary">
            {{ store.state.loading ? '提交中...' : '确认' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { usePaymentStore } from '@/stores/usePaymentStore'
import { loanApi } from '@/api/paymentApi'

const store = usePaymentStore()

// 对账异常列表
const exceptions = ref([])

// 显示处理弹窗
const showHandleDialog = ref(false)

// 当前处理的异常
const currentException = reactive({})

// 处理表单
const handleForm = reactive({
  handleMethod: 'AUTO_RETRY',
  result: ''
})

// 加载对账异常
const loadExceptions = async () => {
  try {
    const res = await loanApi.getExceptions()
    if (res.data.success) {
      exceptions.value = res.data.data
    }
  } catch (e) {
    console.error('加载对账异常失败', e)
    store.showToast('加载对账异常失败', 'error')
  }
}

// 手动触发对账
const handleReconcile = async () => {
  if (!confirm('确认手动触发对账任务？')) return
  
  store.setLoading(true)
  try {
    const res = await loanApi.reconcile()
    if (res.data.success) {
      store.showToast('对账完成')
      loadExceptions()
    } else {
      store.showToast(res.data.message, 'error')
    }
  } catch (e) {
    store.showToast('对账失败: ' + e.message, 'error')
  } finally {
    store.setLoading(false)
  }
}

// 显示处理弹窗
const showHandleModal = (exception) => {
  Object.assign(currentException, exception)
  handleForm.handleMethod = 'AUTO_RETRY'
  handleForm.result = ''
  showHandleDialog.value = true
}

// 关闭处理弹窗
const closeHandleModal = () => {
  showHandleDialog.value = false
}

// 提交处理
const submitHandle = async () => {
  if (!handleForm.result) {
    store.showToast('请输入处理结果说明', 'error')
    return
  }
  
  store.setLoading(true)
  try {
    const params = {
      exceptionId: currentException.exceptionId,
      handleMethod: handleForm.handleMethod,
      result: handleForm.result
    }
    
    const res = await loanApi.handleException(params)
    if (res.data.success) {
      store.showToast('处理完成')
      closeHandleModal()
      loadExceptions()
    } else {
      store.showToast(res.data.message, 'error')
    }
  } catch (e) {
    store.showToast('处理失败: ' + e.message, 'error')
  } finally {
    store.setLoading(false)
  }
}

// 获取异常类型文本
const getExceptionTypeText = (type) => {
  const map = {
    'PAY_SUCCESS_CORE_FAIL': '支付成功核心未入账',
    'CORE_SUCCESS_PAY_FAIL': '核心已入账支付失败',
    'AMT_MISMATCH': '金额不一致',
    'BATCH_PARTIAL_FAIL': '批量扣款部分失败'
  }
  return map[type] || type
}

// 获取状态文本
const getStatusText = (status) => {
  const map = {
    'P': '待处理',
    'R': '已解决',
    'H': '人工处理'
  }
  return map[status] || status
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

onMounted(() => {
  loadExceptions()
})
</script>

<style scoped>
.reconciliation-view {
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
  min-width: 800px;
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
}

.amount.positive {
  color: #ff4d4f;
}

.amount.negative {
  color: #52c41a;
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

.status-badge {
  padding: 0.25rem 0.6rem;
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: 500;
}

.status-badge.p {
  background: #fff7e6;
  color: #faad14;
  border: 1px solid #ffd591;
}

.status-badge.r {
  background: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.status-badge.h {
  background: #e6f7ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
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

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 0.8rem 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-row label {
  font-weight: 500;
  color: #666666;
}

.info-row span {
  color: #333333;
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

.input-field.textarea {
  resize: vertical;
  font-family: inherit;
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
