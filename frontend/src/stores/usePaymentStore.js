import { reactive, computed } from 'vue'

/**
 * 自定义 Hook，用于管理支付相关的状态、用户认证及 UI 交互逻辑。
 * 该 Hook 封装了状态管理方法，包括令牌设置、登出、支付记录管理、加载状态控制、
 * Toast 提示显示以及侧边栏切换，并提供了计算总金额的功能。
 *
 * @returns {Object} 返回包含状态对象及一系列操作方法的集合
 * @returns {Object} return.state - 当前应用的全局状态对象
 * @returns {Function} return.setToken - 设置用户认证令牌和用户名的方法
 * @returns {Function} return.logout - 执行用户登出操作，清除认证信息及支付数据
 * @returns {Function} return.setPayments - 批量设置支付记录列表的方法
 * @returns {Function} return.addPayment - 向支付记录列表中添加单条记录的方法
 * @returns {Function} return.setLoading - 设置应用加载状态的方法
 * @returns {Function} return.showToast - 显示临时提示消息的方法
 * @returns {Function} return.toggleSidebar - 切换侧边栏显示/隐藏状态的方法
 * @returns {ComputedRef<string>} return.totalAmount - 计算所有支付记录总金额的响应式属性，保留两位小数
 */

const state = reactive({
  token: localStorage.getItem('jwt_token') || null,
  username: localStorage.getItem('username') || '',
  payments: [],
  loading: false,
  toast: { show: false, message: '', type: 'success' },
  sidebarOpen: window.innerWidth >= 768
})

export const usePaymentStore = () => {
  const setToken = (token, username) => {
    state.token = token
    state.username = username
    localStorage.setItem('jwt_token', token)
    localStorage.setItem('username', username)
  }

  const logout = () => {
    state.token = null
    state.username = ''
    state.payments = []
    localStorage.removeItem('jwt_token')
    localStorage.removeItem('username')
  }

  const setPayments = (payments) => {
    state.payments = payments
  }

  const addPayment = (payment) => {
    state.payments.push(payment)
  }

  const setLoading = (isLoading) => {
    state.loading = isLoading
  }

  /**
   * 显示一个自动消失的 Toast 提示消息。
   * 消息将在显示 3 秒后自动隐藏。
   *
   * @param {string} message - 要显示的提示消息内容
   * @param {string} [type='success'] - 提示消息的类型，默认为 'success'
   */
  const showToast = (message, type = 'success') => {
    state.toast = { show: true, message, type }
    setTimeout(() => { state.toast.show = false }, 3000) // 延长显示时间以便阅读
  }

  /**
   * 切换侧边栏的打开或关闭状态。
   */
  const toggleSidebar = () => {
    state.sidebarOpen = !state.sidebarOpen
  }

  // 计算属性
  /**
   * 计算所有支付记录的总金额。
   * 对每条记录的金额进行解析并累加，结果保留两位小数。
   * 若金额无法解析则视为 0。
   *
   * @returns {string} 格式化后的总金额字符串
   */
  const totalAmount = computed(() => {
    return state.payments.reduce((sum, pay) => sum + (parseFloat(pay.amount) || 0), 0).toFixed(2)
  })

  return {
    state,
    setToken,
    logout,
    setPayments,
    addPayment,
    setLoading,
    showToast,
    toggleSidebar,
    totalAmount
  }
}