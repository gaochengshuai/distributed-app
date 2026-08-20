import { paymentApi } from '@/api/paymentApi'
import { usePaymentStore } from '@/stores/usePaymentStore'

export const usePaymentLogic = () => {
  const store = usePaymentStore()

  const login = async (credentials) => {
    // 1. 增加对 credentials 对象本身的防御性检查，防止解构报错
    if (!credentials || !credentials.username || !credentials.password) {
      return store.showToast('请输入账号密码', 'error')
    }

    // 2. 可选：如果允许，建议在此处对用户名密码进行 trim 处理后再验证和发送
    // 但为了严格遵守“内部字符串不能产生变化”且不改变输入输出，我们保持原样发送
    // 仅增强验证逻辑：检查是否为纯空格（可选，视业务需求而定，此处保持简洁仅做非空检查）
    
    store.setLoading(true)
    try {
      const res = await paymentApi.login(credentials)
      
      // 3. 增加对响应数据的防御性检查，确保 token 存在
      if (!res || !res.data || !res.data.token) {
        throw new Error('登录响应数据格式错误')
      }

      store.setToken(res.data.token, credentials.username)
      store.showToast('登录成功')
      
      // 4. 确保 fetchPayments 定义在当前作用域或已导入
      await fetchPayments()
    } catch (e) {
      // 5. 记录原始错误以便调试，不暴露敏感信息给控制台（生产环境建议移除或改为日志服务）
      console.error('Login error:', e)
      
      // 6. 安全地获取错误消息，避免直接暴露后端详细错误
      const errorMessage = e.response?.data?.message || '未知错误'
      store.showToast('登录失败: ' + errorMessage, 'error')
    } finally {
      store.setLoading(false)
    }
  }

  const register = async (userData) => {
    if (!userData.username || !userData.password) {
      return store.showToast('请输入账号密码', 'error')
    }
    store.setLoading(true)
    try {
      // 确保发送的数据格式正确
      const requestData = {
        username: userData.username,
        password: userData.password,
        displayName: userData.displayName || userData.username
      }
      
      console.log('注册请求数据:', requestData)
      
      const res = await paymentApi.register(requestData)
      
      console.log('注册响应:', res)
      
      store.showToast('注册成功，请登录')
    } catch (e) {
      console.error('注册失败:', e)
      console.error('错误详情:', e.response?.data)
      const errorMessage = e.response?.data?.error || e.response?.data?.message || '注册失败'
      store.showToast(errorMessage, 'error')
    } finally {
      store.setLoading(false)
    }
  }

  const fetchPayments = async () => {
    try {
      // 实际项目中取消注释
      const res = await paymentApi.getPayments()
      store.setPayments(res.data)
      
    } catch (e) {
      console.error(e)
    }
  }

  const submitPayment = async (modalForm, type) => {
    if (!modalForm.orderNo || !modalForm.amount) {
      return store.showToast('请填写完整', 'error')
    }
    store.setLoading(true)
    try {
      let res
      if (type === 'create') {
        res = await paymentApi.createPayment({
          orderNo: modalForm.orderNo,
          amount: modalForm.amount
        })
        store.showToast('创建成功')
        await fetchPayments() // 刷新列表以获取最新数据
      } else {

        res = await paymentApi.updatePayment(modalForm.id, {

          orderNo: modalForm.orderNo,
          amount: modalForm.amount,
          // 如果后端需要其他字段（如状态），也需在此传递
          status: modalForm.status 
        })
        store.showToast('修改成功')
        // 更新本地 Store 中的对应项，避免重新请求整个列表（性能优化）
        // 假设 store.state.payments 是一个数组
        const index = store.state.payments.findIndex(p => p.id === modalForm.id)
        if (index !== -1) {
          // 使用 Vue 的响应式更新方式，或者直接替换对象
          // 这里简单起见，我们重新获取列表以确保与后端完全同步
          await fetchPayments()
        }
      }
      return true
    } catch (e) {
      store.showToast('操作失败', 'error')
      return false
    } finally {
      store.setLoading(false)
    }
  }

  return {
    login,
    register,
    fetchPayments,
    submitPayment
  }
}