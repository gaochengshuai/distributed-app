# 统一分页组件使用指南

## 📦 组件位置
`frontend/src/components/Pagination.vue`

---

## ✨ 功能特性

- ✅ **智能页码显示**：最多显示7个页码按钮，超出部分用省略号表示
- ✅ **完整的导航控制**：首页、上一页、下一页、末页按钮
- ✅ **每页数量选择**：支持10/20/50/100条每页
- ✅ **响应式设计**：自动适配桌面和移动端
- ✅ **边界保护**：自动禁用不可用的按钮
- ✅ **事件驱动**：通过emit通知父组件页码变化

---

## 🔧 Props 参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `total` | Number | ✅ | 0 | 总记录数 |
| `currentPage` | Number | ✅ | 1 | 当前页码 |
| `pageSize` | Number | ✅ | 10 | 每页显示数量 |

---

## 📤 Events 事件

| 事件名 | 参数 | 说明 |
|--------|------|------|
| `page-change` | `(page: Number)` | 页码变化时触发，返回新页码 |
| `page-size-change` | `(pageSize: Number)` | 每页数量变化时触发，返回新的每页数量 |

---

## 💻 使用示例

### 基础用法

```vue
<template>
  <div>
    <!-- 数据表格 -->
    <table>
      <tr v-for="item in paginatedData" :key="item.id">
        <td>{{ item.name }}</td>
      </tr>
    </table>
    
    <!-- 分页组件 -->
    <Pagination 
      :total="totalItems"
      :current-page="currentPage"
      :page-size="pageSize"
      @page-change="handlePageChange"
      @page-size-change="handlePageSizeChange"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import Pagination from '../components/Pagination.vue'

// 数据状态
const allData = ref([...]) // 所有数据
const currentPage = ref(1)
const pageSize = ref(10)

// 计算总记录数
const totalItems = computed(() => allData.value.length)

// 计算当前页的数据
const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return allData.value.slice(start, end)
})

// 处理页码变化
const handlePageChange = (page) => {
  currentPage.value = page
}

// 处理每页数量变化
const handlePageSizeChange = (newSize) => {
  pageSize.value = newSize
  currentPage.value = 1 // 重置到第一页
}
</script>
```

---

### 带筛选条件的用法

```vue
<template>
  <div>
    <!-- 搜索栏 -->
    <div class="search-bar">
      <input v-model="searchKeyword" placeholder="搜索..." />
      <button @click="handleSearch">搜索</button>
    </div>
    
    <!-- 数据列表 -->
    <ul>
      <li v-for="item in filteredAndPaginatedData" :key="item.id">
        {{ item.name }}
      </li>
    </ul>
    
    <!-- 分页组件 -->
    <Pagination 
      v-if="filteredData.length > 0"
      :total="filteredData.length"
      :current-page="currentPage"
      :page-size="pageSize"
      @page-change="handlePageChange"
      @page-size-change="handlePageSizeChange"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import Pagination from '../components/Pagination.vue'

const allData = ref([...])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

// 筛选后的数据
const filteredData = computed(() => {
  if (!searchKeyword.value) return allData.value
  return allData.value.filter(item => 
    item.name.includes(searchKeyword.value)
  )
})

// 筛选并分页后的数据
const filteredAndPaginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredData.value.slice(start, end)
})

const handleSearch = () => {
  currentPage.value = 1 // 搜索时重置到第一页
}

const handlePageChange = (page) => {
  currentPage.value = page
}

const handlePageSizeChange = (newSize) => {
  pageSize.value = newSize
  currentPage.value = 1
}
</script>
```

---

### 后端分页用法（推荐大数据量）

```vue
<template>
  <div>
    <table>
      <tr v-for="item in tableData" :key="item.id">
        <td>{{ item.name }}</td>
      </tr>
    </table>
    
    <Pagination 
      :total="totalFromBackend"
      :current-page="currentPage"
      :page-size="pageSize"
      @page-change="fetchData"
      @page-size-change="handlePageSizeChange"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Pagination from '../components/Pagination.vue'
import { myApi } from '../api/myApi'

const tableData = ref([])
const totalFromBackend = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 从后端获取数据
const fetchData = async (page = currentPage.value) => {
  try {
    const response = await myApi.getList({
      page: page,
      pageSize: pageSize.value
    })
    
    tableData.value = response.data.items
    totalFromBackend.value = response.data.total
    currentPage.value = page
  } catch (error) {
    console.error('获取数据失败:', error)
  }
}

const handlePageSizeChange = (newSize) => {
  pageSize.value = newSize
  currentPage.value = 1
  fetchData(1) // 重新获取第一页数据
}

onMounted(() => {
  fetchData()
})
</script>
```

---

## 🎨 样式定制

如果需要自定义分页样式，可以覆盖以下CSS类：

```css
/* 分页容器 */
.pagination-container { }

/* 分页信息文本 */
.pagination-info { }

/* 页码按钮容器 */
.pagination-controls { }

/* 页码按钮 */
.pagination-btn { }
.pagination-btn.active { } /* 当前页 */
.pagination-btn:disabled { } /* 禁用状态 */
.pagination-btn.pagination-ellipsis { } /* 省略号 */

/* 每页数量选择器 */
.page-size-selector { }
.page-size-select { }
```

---

## 📱 响应式行为

- **桌面端 (> 768px)**：横向布局，三部分左右分布
- **移动端 (≤ 768px)**：纵向堆叠，居中对齐

---

## ⚠️ 注意事项

1. **必须提供三个Props**：`total`、`currentPage`、`pageSize` 都是必需的
2. **页码从1开始**：不是从0开始
3. **总记录数为0时不显示**：组件会自动隐藏
4. **父组件负责数据切片**：组件只负责UI展示和事件通知
5. **搜索/筛选时重置页码**：建议在父组件中处理

---

## 🔄 已集成的视图

以下视图已使用统一分页组件：

- ✅ [ContractView.vue](../src/views/ContractView.vue) - 合同管理
- ✅ [DashboardView.vue](../src/views/DashboardView.vue) - 订单管理

---

## 🚀 后续计划

计划将以下视图也迁移到统一分页组件：

- ⏳ WithdrawView.vue - 提款管理
- ⏳ RepaymentView.vue - 还款管理
- ⏳ ReconciliationView.vue - 对账管理

---

## 💡 最佳实践

1. **前端分页适用场景**：
   - 数据量 < 1000条
   - 需要快速筛选和排序
   - 用户体验优先

2. **后端分页适用场景**：
   - 数据量 ≥ 1000条
   - 性能要求高
   - 实时性要求高

3. **状态管理建议**：
   - 使用 `ref` 或 `reactive` 管理分页状态
   - 使用 `computed` 计算分页数据
   - 保持状态单一数据源

4. **错误处理**：
   - API请求失败时保持当前页码不变
   - 显示友好的错误提示
   - 提供重试机制

---

**维护人员**: Lingma (灵码)  
**最后更新**: 2026-08-21  
**版本**: v1.0.0
