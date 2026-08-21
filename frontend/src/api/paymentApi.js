import axios from 'axios'

const GATEWAY_BASE_URL = 'http://localhost:8080'

const apiClient = axios.create({
  baseURL: GATEWAY_BASE_URL,
  timeout: 10000
})

// 请求拦截器：自动添加 Token
apiClient.interceptors.request.use(config => {
  const token = localStorage.getItem('jwt_token')
  if (token) {
    config.headers['Authorization'] = 'Bearer ' + token
  }
  return config
})

export const paymentApi = {
  login(credentials) {
    return apiClient.post('/api/auth/login', credentials)
  },
  register(userData) {
    return apiClient.post('/api/auth/register', userData)
  },
  /**
   * 获取支付列表信息
   * @returns {Promise} 返回包含支付数据的 Promise 对象
   */
  getPayments() {
    return apiClient.get('/api/payments')
  },
  /**
   * 创建支付记录
   * @param {Object} paymentData - 支付数据对象，包含创建支付所需的相关信息
   * @returns {Promise} 返回一个 Promise 对象，解析后包含服务器响应结果
   */
  createPayment(paymentData) {
    return apiClient.post('/api/payments', paymentData)
  },
  /**
   * 更新支付记录
   * @param {string} id - 要更新的支付记录的 ID
   * @param {Object} paymentData - 更新的支付数据对象
   * @returns {Promise} 返回一个 Promise 对象，解析后包含服务器响应结果
   */
  updatePayment(id, paymentData) {
    return apiClient.put(`/api/payments/${id}`, paymentData)
  }
}

// 贷款业务 API
export const loanApi = {
  /**
   * 发起提款申请
   */
  withdraw(data) {
    return apiClient.post('/api/loan/withdraw', data)
  },
  
  /**
   * 审核通过提款
   */
  approve(loanRegId) {
    return apiClient.post(`/api/loan/approve/${loanRegId}`)
  },
  
  /**
   * 重试提款
   */
  retryWithdraw(orderId) {
    return apiClient.post(`/api/loan/retry/${orderId}`)
  },
  
  /**
   * 发起还款
   */
  repay(params) {
    return apiClient.post('/api/loan/repay', null, { params })
  },
  
  /**
   * 重试还款
   */
  retryRepay(orderId) {
    return apiClient.post(`/api/loan/repay/retry/${orderId}`)
  },
  
  /**
   * 获取还款记录列表
   */
  getRepayRecords() {
    return apiClient.get('/api/loan/repay/records')
  },
  
  /**
   * 根据订单号查询还款记录
   * @param {string} orderId - 订单号
   */
  getRepayRecordByOrder(orderId) {
    return apiClient.get(`/api/loan/repay/records/${orderId}`)
  },
  
  /**
   * 手动触发对账
   */
  reconcile() {
    return apiClient.post('/api/loan/reconcile')
  },
  
  /**
   * 查询对账异常
   */
  getExceptions() {
    return apiClient.get('/api/loan/reconcile/exceptions')
  },
  
  /**
   * 处理对账异常
   */
  handleException(params) {
    return apiClient.post('/api/loan/reconcile/handle', null, { params })
  }
}

// 合同管理 API
export const contractApi = {
  /**
   * 获取所有合同
   */
  getAllContracts() {
    return apiClient.get('/api/contracts')
  },
  
  /**
   * 根据合同号查询合同
   * @param {string} contrNo - 合同号
   */
  getContractByNo(contrNo) {
    return apiClient.get(`/api/contracts/${contrNo}`)
  },
  
  /**
   * 根据客户ID查询所有合同
   * @param {string} custId - 客户ID
   */
  getContractsByCustomer(custId) {
    return apiClient.get(`/api/contracts/customer/${custId}`)
  },
  
  /**
   * 根据客户ID和状态查询合同
   * @param {string} custId - 客户ID
   * @param {string} status - 合同状态（A-有效，U-待审核，R-已拒绝，C-已关闭）
   */
  getContractsByStatus(custId, status) {
    return apiClient.get(`/api/contracts/customer/${custId}/status/${status}`)
  },
  
  /**
   * 获取客户的第一个有效合同
   * @param {string} custId - 客户ID
   */
  getFirstActiveContract(custId) {
    return apiClient.get(`/api/contracts/customer/${custId}/active`)
  },
  
  /**
   * 创建新合同
   * @param {Object} contractData - 合同数据 {contrNo, custId, productId, signAmt}
   */
  createContract(contractData) {
    return apiClient.post('/api/contracts', contractData)
  },
  
  /**
   * 更新合同状态
   * @param {string} contrNo - 合同号
   * @param {string} status - 新状态
   */
  updateContractStatus(contrNo, status) {
    return apiClient.put(`/api/contracts/${contrNo}/status`, { status })
  },
  
  /**
   * 激活合同
   * @param {string} contrNo - 合同号
   */
  activateContract(contrNo) {
    return apiClient.put(`/api/contracts/${contrNo}/activate`)
  },
  
  /**
   * 关闭合同
   * @param {string} contrNo - 合同号
   */
  closeContract(contrNo) {
    return apiClient.put(`/api/contracts/${contrNo}/close`)
  },
  
  /**
   * 删除合同
   * @param {string} contrNo - 合同号
   */
  deleteContract(contrNo) {
    return apiClient.delete(`/api/contracts/${contrNo}`)
  },
  
  /**
   * 检查合同是否存在
   * @param {string} contrNo - 合同号
   */
  checkContractExists(contrNo) {
    return apiClient.get(`/api/contracts/${contrNo}/exists`)
  },
  
  /**
   * 根据产品ID查询合同
   * @param {string} productId - 产品ID
   */
  getContractsByProduct(productId) {
    return apiClient.get(`/api/contracts/product/${productId}`)
  }
}
