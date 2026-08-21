<template>
  <div class="contract-view">
    <div class="page-header">
      <h2>📄 合同管理</h2>
      <button @click="openCreateModal" class="btn btn-primary">+ 新建合同</button>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="search-bar">
      <div class="search-item">
        <label>合同号：</label>
        <input 
          v-model="searchForm.contrNo" 
          type="text" 
          placeholder="输入合同号" 
          class="input-field"
        />
      </div>
      <div class="search-item">
        <label>客户ID：</label>
        <input 
          v-model="searchForm.custId" 
          type="text" 
          placeholder="输入客户ID" 
          class="input-field"
        />
      </div>
      <div class="search-item">
        <label>合同状态：</label>
        <select v-model="searchForm.status" class="input-field">
          <option value="">全部</option>
          <option value="A">有效</option>
          <option value="U">待审核</option>
          <option value="R">已拒绝</option>
          <option value="C">已关闭</option>
        </select>
      </div>
      <div class="search-actions">
        <button @click="handleSearch" class="btn btn-primary">搜索</button>
        <button @click="handleReset" class="btn btn-secondary">重置</button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 合同列表表格 -->
    <div v-else class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>合同号</th>
            <th>客户ID</th>
            <th>产品ID</th>
            <th>签署金额</th>
            <th>合同状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="contract in paginatedContracts" :key="contract.contrNo">
            <td><strong>{{ contract.contrNo }}</strong></td>
            <td>{{ contract.custId }}</td>
            <td>{{ contract.productId || '-' }}</td>
            <td class="amount">¥{{ formatAmount(contract.signAmt) }}</td>
            <td>
              <span :class="['status-badge', getStatusClass(contract.contrStatus)]">
                {{ getStatusText(contract.contrStatus) }}
              </span>
            </td>
            <td class="actions">
              <button 
                v-if="contract.contrStatus === 'A'" 
                @click="handleClose(contract)" 
                class="btn-icon btn-warning"
                title="关闭合同"
              >
                🔒
              </button>
              <button 
                v-if="contract.contrStatus !== 'A'" 
                @click="handleActivate(contract)" 
                class="btn-icon btn-success"
                title="激活合同"
              >
                ✅
              </button>
              <button 
                @click="handleDelete(contract)" 
                class="btn-icon btn-danger"
                title="删除合同"
              >
                🗑️
              </button>
            </td>
          </tr>
          <tr v-if="paginatedContracts.length === 0">
            <td colspan="6" class="empty-state-cell">
              <div class="empty-state">
                <p>暂无合同数据</p>
                <button @click="openCreateModal" class="btn btn-primary">创建第一个合同</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      
      <!-- 分页控件 -->
      <div v-if="filteredContracts && filteredContracts.length > 0" class="pagination">
        <div class="pagination-info">
          共 {{ filteredContracts.length }} 条记录，第 {{ currentPage }}/{{ totalPages }} 页
        </div>
        <div class="pagination-controls">
          <button 
            @click="goToPage(1)" 
            :disabled="currentPage === 1"
            class="pagination-btn"
            title="首页"
          >
            «
          </button>
          <button 
            @click="goToPage(currentPage - 1)" 
            :disabled="currentPage === 1"
            class="pagination-btn"
            title="上一页"
          >
            ‹
          </button>
          
          <template v-for="page in visiblePages" :key="page">
            <button 
              v-if="page === '...'" 
              class="pagination-btn pagination-ellipsis"
              disabled
            >
              ...
            </button>
            <button 
              v-else
              @click="goToPage(page)" 
              :class="['pagination-btn', { active: page === currentPage }]"
            >
              {{ page }}
            </button>
          </template>
          
          <button 
            @click="goToPage(currentPage + 1)" 
            :disabled="currentPage === totalPages"
            class="pagination-btn"
            title="下一页"
          >
            ›
          </button>
          <button 
            @click="goToPage(totalPages)" 
            :disabled="currentPage === totalPages"
            class="pagination-btn"
            title="末页"
          >
            »
          </button>
        </div>
        <div class="page-size-selector">
          <label>每页显示：</label>
          <select v-model="pageSize" @change="handlePageSizeChange" class="input-field page-size-select">
            <option value="10">10 条</option>
            <option value="20">20 条</option>
            <option value="50">50 条</option>
            <option value="100">100 条</option>
          </select>
        </div>
      </div>
    </div>

    <!-- 统计信息（基于筛选结果） -->
    <div class="statistics">
      <div class="stat-item">
        <span class="stat-label">总合同数：</span>
        <span class="stat-value">{{ filteredContracts.length }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">有效合同：</span>
        <span class="stat-value success">{{ filteredActiveCount }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">总金额：</span>
        <span class="stat-value amount">¥{{ formatAmount(filteredTotalAmount) }}</span>
      </div>
    </div>

    <!-- 创建/编辑合同弹窗 -->
    <div v-if="modal.visible" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <h3>{{ modal.isEdit ? '编辑合同' : '新建合同' }}</h3>
        
        <div class="form-group">
          <label>合同号 <span class="required">*</span></label>
          <input 
            v-model="modalForm.contrNo" 
            type="text" 
            :disabled="modal.isEdit"
            placeholder="例如: CONTR20260821001" 
            class="input-field" 
          />
        </div>
        
        <div class="form-group">
          <label>客户ID <span class="required">*</span></label>
          <input 
            v-model="modalForm.custId" 
            type="text" 
            placeholder="例如: CUST001" 
            class="input-field" 
          />
        </div>
        
        <div class="form-group">
          <label>产品ID</label>
          <input 
            v-model="modalForm.productId" 
            type="text" 
            placeholder="例如: PROD001" 
            class="input-field" 
          />
        </div>
        
        <div class="form-group">
          <label>签署金额 <span class="required">*</span></label>
          <input 
            v-model.number="modalForm.signAmt" 
            type="number" 
            step="0.01" 
            min="0"
            placeholder="0.00" 
            class="input-field" 
          />
        </div>
        
        <div class="modal-actions">
          <button @click="closeModal" class="btn btn-secondary">取消</button>
          <button @click="handleSubmit" :disabled="submitting" class="btn btn-primary">
            {{ submitting ? '提交中...' : '确认' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { contractApi } from '../api/paymentApi'

// 合同列表数据
const contracts = ref([])
const loading = ref(false)
const submitting = ref(false)

// 搜索表单
const searchForm = reactive({
  contrNo: '',
  custId: '',
  status: ''
})

// 弹窗状态
const modal = reactive({
  visible: false,
  isEdit: false
})

const modalForm = reactive({
  contrNo: '',
  custId: '',
  productId: '',
  signAmt: null
})

// 过滤后的合同列表
const filteredContracts = computed(() => {
  let result = contracts.value
  
  // 按合同号筛选
  if (searchForm.contrNo) {
    result = result.filter(c => c.contrNo.includes(searchForm.contrNo))
  }
  
  // 按客户ID筛选
  if (searchForm.custId) {
    result = result.filter(c => c.custId.includes(searchForm.custId))
  }
  
  // 按状态筛选
  if (searchForm.status) {
    result = result.filter(c => c.contrStatus === searchForm.status)
  }
  
  return result
})

// 基于筛选结果的统计数据
const filteredActiveCount = computed(() => {
  return filteredContracts.value.filter(c => c.contrStatus === 'A').length
})

const filteredTotalAmount = computed(() => {
  return filteredContracts.value.reduce((sum, c) => sum + (c.signAmt || 0), 0)
})

// 分页相关状态
const currentPage = ref(1)
const pageSize = ref(10)

// 计算总页数
const totalPages = computed(() => {
  return Math.ceil(filteredContracts.value.length / pageSize.value) || 1
})

// 当前页的合同列表
const paginatedContracts = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredContracts.value.slice(start, end)
})

// 可见的页码列表（智能显示）
const visiblePages = computed(() => {
  const pages = []
  const total = totalPages.value
  const current = currentPage.value
  
  // 防御性检查：如果总页数为0或无效，返回空数组
  if (!total || total <= 0) {
    return []
  }
  
  if (total <= 7) {
    // 总页数少于7页，全部显示
    for (let i = 1; i <= total; i++) {
      pages.push(i)
    }
  } else {
    // 总页数多于7页，智能显示
    if (current <= 4) {
      // 当前页在前面
      for (let i = 1; i <= 5; i++) {
        pages.push(i)
      }
      pages.push('...')
      pages.push(total)
    } else if (current >= total - 3) {
      // 当前页在后面
      pages.push(1)
      pages.push('...')
      for (let i = total - 4; i <= total; i++) {
        pages.push(i)
      }
    } else {
      // 当前页在中间
      pages.push(1)
      pages.push('...')
      for (let i = current - 1; i <= current + 1; i++) {
        pages.push(i)
      }
      pages.push('...')
      pages.push(total)
    }
  }
  
  return pages
})

// 跳转到指定页
const goToPage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
}

// 每页显示数量变化
const handlePageSizeChange = () => {
  currentPage.value = 1 // 重置到第一页
}

// 获取合同列表
const fetchContracts = async () => {
  loading.value = true
  try {
    const response = await contractApi.getAllContracts()
    if (response.data.success) {
      contracts.value = response.data.data || []
    } else {
      console.error('获取合同列表失败:', response.data.message)
    }
  } catch (error) {
    console.error('获取合同列表异常:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1 // 重置到第一页
}

// 重置搜索
const handleReset = () => {
  searchForm.contrNo = ''
  searchForm.custId = ''
  searchForm.status = ''
  currentPage.value = 1 // 重置到第一页
}

// 打开创建弹窗
const openCreateModal = () => {
  modal.isEdit = false
  Object.assign(modalForm, {
    contrNo: '',
    custId: '',
    productId: '',
    signAmt: null
  })
  modal.visible = true
}

// 关闭弹窗
const closeModal = () => {
  modal.visible = false
}

// 提交表单
const handleSubmit = async () => {
  // 表单验证
  if (!modalForm.contrNo || !modalForm.contrNo.trim()) {
    alert('请输入合同号')
    return
  }
  if (!modalForm.custId || !modalForm.custId.trim()) {
    alert('请输入客户ID')
    return
  }
  if (!modalForm.signAmt || modalForm.signAmt <= 0) {
    alert('请输入有效的签署金额')
    return
  }

  submitting.value = true
  try {
    const contractData = {
      contrNo: modalForm.contrNo.trim(),
      custId: modalForm.custId.trim(),
      productId: modalForm.productId?.trim() || null,
      signAmt: modalForm.signAmt
    }

    const response = await contractApi.createContract(contractData)
    
    if (response.data.success) {
      alert('合同创建成功')
      closeModal()
      await fetchContracts()
    } else {
      alert('创建失败: ' + response.data.message)
    }
  } catch (error) {
    console.error('创建合同异常:', error)
    alert('创建合同失败: ' + (error.response?.data?.message || error.message))
  } finally {
    submitting.value = false
  }
}

// 激活合同
const handleActivate = async (contract) => {
  if (!confirm(`确定要激活合同 ${contract.contrNo} 吗？`)) {
    return
  }

  try {
    const response = await contractApi.activateContract(contract.contrNo)
    if (response.data.success) {
      alert('合同已激活')
      await fetchContracts()
    } else {
      alert('激活失败: ' + response.data.message)
    }
  } catch (error) {
    console.error('激活合同异常:', error)
    alert('激活合同失败: ' + (error.response?.data?.message || error.message))
  }
}

// 关闭合同
const handleClose = async (contract) => {
  if (!confirm(`确定要关闭合同 ${contract.contrNo} 吗？关闭后无法恢复！`)) {
    return
  }

  try {
    const response = await contractApi.closeContract(contract.contrNo)
    if (response.data.success) {
      alert('合同已关闭')
      await fetchContracts()
    } else {
      alert('关闭失败: ' + response.data.message)
    }
  } catch (error) {
    console.error('关闭合同异常:', error)
    alert('关闭合同失败: ' + (error.response?.data?.message || error.message))
  }
}

// 删除合同
const handleDelete = async (contract) => {
  if (!confirm(`确定要删除合同 ${contract.contrNo} 吗？此操作不可恢复！`)) {
    return
  }

  try {
    const response = await contractApi.deleteContract(contract.contrNo)
    if (response.data.success) {
      alert('合同已删除')
      await fetchContracts()
    } else {
      alert('删除失败: ' + response.data.message)
    }
  } catch (error) {
    console.error('删除合同异常:', error)
    alert('删除合同失败: ' + (error.response?.data?.message || error.message))
  }
}

// 格式化金额
const formatAmount = (amount) => {
  if (amount === null || amount === undefined) return '0.00'
  return Number(amount).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

// 获取状态样式类
const getStatusClass = (status) => {
  const map = {
    'A': 'status-active',
    'U': 'status-pending',
    'R': 'status-rejected',
    'C': 'status-closed'
  }
  return map[status] || ''
}

// 获取状态文本
const getStatusText = (status) => {
  const map = {
    'A': '有效',
    'U': '待审核',
    'R': '已拒绝',
    'C': '已关闭'
  }
  return map[status] || status
}

// 组件挂载时加载数据
onMounted(() => {
  console.log('ContractView 组件已挂载')
  fetchContracts()
})
</script>

<style scoped>
.contract-view {
  background: #ffffff;
  border-radius: 8px;
  padding: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.page-header h2 {
  margin: 0;
  color: #094781;
  font-size: 1.5rem;
}

/* 搜索栏 */
.search-bar {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.5rem;
  padding: 1rem;
  background: #f5f5f5;
  border-radius: 4px;
  align-items: center;
  flex-wrap: wrap;
}

.search-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.search-item label {
  font-size: 14px;
  color: #666;
  white-space: nowrap;
}

.search-actions {
  display: flex;
  gap: 0.5rem;
  margin-left: auto;
}

/* 加载状态 */
.loading-container {
  text-align: center;
  padding: 3rem;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #1890ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 表格容器 */
.table-container {
  overflow-x: auto;
  margin-bottom: 1.5rem;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
}

.data-table thead {
  background: #fafafa;
}

.data-table th {
  padding: 1rem;
  text-align: left;
  font-weight: 600;
  color: #333;
  border-bottom: 2px solid #e8e8e8;
  white-space: nowrap;
}

.data-table td {
  padding: 1rem;
  border-bottom: 1px solid #e8e8e8;
  color: #666;
}

.data-table tbody tr:hover {
  background: #f5f5f5;
}

.amount {
  color: #1890ff;
  font-weight: 600;
}

/* 状态徽章 */
.status-badge {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-active {
  background: #e6f7ff;
  color: #1890ff;
}

.status-pending {
  background: #fff7e6;
  color: #fa8c16;
}

.status-rejected {
  background: #fff1f0;
  color: #ff4d4f;
}

.status-closed {
  background: #f5f5f5;
  color: #999;
}

/* 操作按钮 */
.actions {
  display: flex;
  gap: 0.5rem;
}

.btn-icon {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.4rem;
  border-radius: 4px;
  transition: all 0.3s;
  font-size: 16px;
}

.btn-icon:hover {
  background: #f0f0f0;
  transform: scale(1.1);
}

.btn-success:hover {
  background: #e6f7ff;
}

.btn-warning:hover {
  background: #fff7e6;
}

.btn-danger:hover {
  background: #fff1f0;
}

/* 空状态 */
.empty-state-cell {
  text-align: center;
  padding: 3rem;
}

.empty-state {
  color: #999;
}

.empty-state p {
  margin-bottom: 1rem;
}

/* 分页控件 */
.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  background: #fafafa;
  border-top: 1px solid #e8e8e8;
  flex-wrap: wrap;
  gap: 1rem;
}

.pagination-info {
  color: #666;
  font-size: 14px;
  white-space: nowrap;
}

.pagination-controls {
  display: flex;
  gap: 0.25rem;
  align-items: center;
}

.pagination-btn {
  min-width: 32px;
  height: 32px;
  padding: 0 0.5rem;
  border: 1px solid #d9d9d9;
  background: white;
  color: #333;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pagination-btn:hover:not(:disabled) {
  border-color: #1890ff;
  color: #1890ff;
}

.pagination-btn.active {
  background: #1890ff;
  border-color: #1890ff;
  color: white;
}

.pagination-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.pagination-btn.pagination-ellipsis {
  border: none;
  background: transparent;
  cursor: default;
  color: #999;
}

.page-size-selector {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #666;
  font-size: 14px;
}

.page-size-select {
  width: auto;
  padding: 0.4rem 0.8rem;
  font-size: 14px;
}

/* 统计信息 */
.statistics {
  display: flex;
  gap: 2rem;
  padding: 1.5rem;
  background: #fafafa;
  border-radius: 4px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.stat-label {
  color: #666;
  font-size: 14px;
}

.stat-value {
  font-size: 1.25rem;
  font-weight: 600;
  color: #333;
}

.stat-value.success {
  color: #52c41a;
}

.stat-value.amount {
  color: #1890ff;
}

/* 弹窗 */
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
  border-radius: 8px;
  padding: 2rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  animation: slideUp 0.3s ease-out;
}

.modal-content h3 {
  margin-top: 0;
  color: #094781;
  font-weight: 600;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.required {
  color: #ff4d4f;
}

.input-field {
  width: 100%;
  padding: 0.8rem;
  border: 1px solid #dcdcdc;
  border-radius: 4px;
  outline: none;
  transition: all 0.3s;
  font-size: 14px;
}

.input-field:focus {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.input-field:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.modal-actions {
  display: flex;
  gap: 1rem;
  margin-top: 2rem;
  justify-content: flex-end;
}

/* 按钮样式 */
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
  color: #666;
}

.btn-secondary:hover:not(:disabled) {
  border-color: #1890ff;
  color: #1890ff;
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .contract-view {
    padding: 1rem;
  }

  .page-header {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }

  .search-bar {
    flex-direction: column;
  }

  .search-actions {
    margin-left: 0;
    width: 100%;
    justify-content: flex-end;
  }

  .statistics {
    flex-direction: column;
    gap: 1rem;
  }

  .pagination {
    flex-direction: column;
    align-items: stretch;
  }

  .pagination-controls {
    justify-content: center;
    flex-wrap: wrap;
  }

  .page-size-selector {
    justify-content: center;
  }

  .data-table {
    font-size: 14px;
  }

  .data-table th,
  .data-table td {
    padding: 0.75rem;
  }
}
</style>
