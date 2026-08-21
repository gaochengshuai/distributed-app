<template>
  <div v-if="total > 0" class="pagination-container">
    <div class="pagination-info">
      共 {{ total }} 条记录，第 {{ currentPage }}/{{ totalPages }} 页
    </div>
    <div class="pagination-controls">
      <!-- 首页 -->
      <button 
        @click="handlePageChange(1)" 
        :disabled="currentPage === 1"
        class="pagination-btn"
        title="首页"
      >
        «
      </button>
      <!-- 上一页 -->
      <button 
        @click="handlePageChange(currentPage - 1)" 
        :disabled="currentPage === 1"
        class="pagination-btn"
        title="上一页"
      >
        ‹
      </button>
      
      <!-- 页码按钮 -->
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
          @click="handlePageChange(page)" 
          :class="['pagination-btn', { active: page === currentPage }]"
        >
          {{ page }}
        </button>
      </template>
      
      <!-- 下一页 -->
      <button 
        @click="handlePageChange(currentPage + 1)" 
        :disabled="currentPage === totalPages"
        class="pagination-btn"
        title="下一页"
      >
        ›
      </button>
      <!-- 末页 -->
      <button 
        @click="handlePageChange(totalPages)" 
        :disabled="currentPage === totalPages"
        class="pagination-btn"
        title="末页"
      >
        »
      </button>
    </div>
    <div class="page-size-selector">
      <label>每页显示：</label>
      <select 
        v-model="currentPageSize" 
        @change="handlePageSizeChange" 
        class="input-field page-size-select"
      >
        <option value="10">10 条</option>
        <option value="20">20 条</option>
        <option value="50">50 条</option>
        <option value="100">100 条</option>
      </select>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  // 总记录数
  total: {
    type: Number,
    required: true,
    default: 0
  },
  // 当前页码
  currentPage: {
    type: Number,
    required: true,
    default: 1
  },
  // 每页显示数量
  pageSize: {
    type: Number,
    required: true,
    default: 10
  }
})

const emit = defineEmits(['page-change', 'page-size-change'])

// 内部状态
const currentPageSize = ref(props.pageSize)

// 计算总页数
const totalPages = computed(() => {
  return Math.ceil(props.total / currentPageSize.value) || 1
})

// 可见的页码列表（智能显示）
const visiblePages = computed(() => {
  const pages = []
  const total = totalPages.value
  const current = props.currentPage
  
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

// 监听props.pageSize变化，同步到内部状态
watch(() => props.pageSize, (newVal) => {
  currentPageSize.value = newVal
})

// 处理页码变化
const handlePageChange = (page) => {
  if (page < 1 || page > totalPages.value) return
  emit('page-change', page)
}

// 处理每页显示数量变化
const handlePageSizeChange = () => {
  emit('page-size-change', currentPageSize.value)
}
</script>

<style scoped>
/* 分页容器 */
.pagination-container {
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

/* 响应式设计 */
@media (max-width: 768px) {
  .pagination-container {
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
}
</style>
