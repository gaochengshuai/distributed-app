<template>
  <div class="card auth-card">
    <h2>用户认证</h2>
    <div class="form-group">
      <input v-model='localForm.username' placeholder='用户名' class="input-field" />
      <input v-model='localForm.password' placeholder='密码' type='password' class="input-field" />
      <div class="button-group">
        <button @click='handleLogin' :disabled="loading" class="btn btn-primary">
          {{ loading ? '处理中...' : '登录' }}
        </button>
        <button @click='handleRegister' :disabled="loading" class="btn btn-secondary">
          {{ loading ? '处理中...' : '注册' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { usePaymentLogic } from '@/composables/usePaymentLogic'
import { usePaymentStore } from '@/stores/usePaymentStore'

const { login, register } = usePaymentLogic()
const store = usePaymentStore()

const localForm = reactive({ username: 'admin', password: '123456' })
const loading = ref(false) // 局部加载状态，或者直接使用 store.state.loading

const handleLogin = async () => {
  store.setLoading(true)
  await login(localForm)
  store.setLoading(false)
}

const handleRegister = async () => {
  store.setLoading(true)
  await register(localForm)
  store.setLoading(false)
}
</script>

<style scoped>
/* 复用之前的 auth-card 样式 */
.card {
  background: rgba(91, 123, 226, 0.9); backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 12px;
  padding: 2rem; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}
.auth-card { max-width: 400px; margin: 4rem auto; }
.form-group { margin-bottom: 1.5rem; display: flex; flex-direction: column; gap: 0.8rem; }
.input-field {
  background: rgba(15, 23, 42, 0.6); border: 1px solid #334155;
  color: #f1f5f9; padding: 0.8rem; border-radius: 6px; outline: none;
}
.button-group { display: flex; gap: 1rem; margin-top: 1rem; }
.btn { padding: 0.8rem 1.5rem; border: none; border-radius: 6px; font-weight: 600; cursor: pointer; }
.btn-primary { background: linear-gradient(135deg, #3b82f6, #2563eb); color: white; }
.btn-secondary { background: #334155; color: #cbd5e1; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
h2 { margin-top: 0; color: #f8fafc; font-weight: 600; }
</style>