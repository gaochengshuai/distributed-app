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