<template>
  <div class="page-container">
    <div class="toolbar">
      <div class="search-bar">
        <el-input
          v-model="queryParams.word"
          placeholder="搜索敏感词"
          clearable
          @clear="handleSearch"
          style="width: 200px"
        />
        <el-select
          v-model="queryParams.level"
          placeholder="等级"
          clearable
          style="width: 120px; margin-left: 12px"
        >
          <el-option label="紧急" value="URGENT" />
          <el-option label="中等" value="MEDIUM" />
          <el-option label="普通" value="NORMAL" />
        </el-select>
        <el-select
          v-model="queryParams.status"
          placeholder="状态"
          clearable
          style="width: 120px; margin-left: 12px"
        >
          <el-option label="启用" value="ENABLE" />
          <el-option label="禁用" value="DISABLE" />
        </el-select>
        <el-button type="primary" @click="handleSearch" style="margin-left: 12px">
          搜索
        </el-button>
      </div>
      <div class="action-bar">
        <el-button type="primary" @click="handleAdd">新增</el-button>
        <el-button @click="handleBatchDelete">批量删除</el-button>
      </div>
    </div>

    <div class="card table-card">
      <el-table
        :data="tableData"
        @selection-change="handleSelectionChange"
        v-loading="loading"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="word" label="敏感词" min-width="150" />
        <el-table-column prop="level" label="等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.level)">
              {{ getLevelText(row.level) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-value="ENABLE"
              inactive-value="DISABLE"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryParams.current"
        v-model:page-size="queryParams.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </div>

    <EditDialog
      v-model="editDialogVisible"
      :data="currentRow"
      @success="handleEditSuccess"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import manager from '@/api/manager'
import EditDialog from './Edit.vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const editDialogVisible = ref(false)
const currentRow = ref(null)
const selectedRows = ref([])

const queryParams = reactive({
  current: 1,
  size: 10,
  word: '',
  level: '',
  status: ''
})

const getLevelType = (level) => {
  const map = { URGENT: 'danger', MEDIUM: 'warning', NORMAL: '' }
  return map[level] || ''
}

const getLevelText = (level) => {
  const map = { URGENT: '紧急', MEDIUM: '中等', NORMAL: '普通' }
  return map[level] || level
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await manager.pageSensitiveWords(queryParams)
    if (res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.current = 1
  loadData()
}

const handleAdd = () => {
  currentRow.value = null
  editDialogVisible.value = true
}

const handleEdit = (row) => {
  currentRow.value = { ...row }
  editDialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除敏感词 "${row.word}" 吗？`, '提示', {
      type: 'warning'
    })
    await manager.deleteSensitiveWord(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的数据')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 条数据吗？`, '提示', {
      type: 'warning'
    })
    await Promise.all(selectedRows.value.map(r => manager.deleteSensitiveWord(r.id)))
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleStatusChange = async (row) => {
  try {
    await manager.updateSensitiveWord(row)
    ElMessage.success('状态更新成功')
  } catch (e) {
    ElMessage.error('状态更新失败')
    loadData()
  }
}

const handleSelectionChange = (rows) => {
  selectedRows.value = rows
}

const handleEditSuccess = () => {
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}

.search-bar {
  display: flex;
  align-items: center;
}

.action-bar {
  display: flex;
  gap: 12px;
}

.table-card {
  padding: 0;
}
</style>
