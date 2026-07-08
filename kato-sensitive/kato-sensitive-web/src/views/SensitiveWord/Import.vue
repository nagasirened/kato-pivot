<template>
  <div class="page-container">
    <h2 class="page-title">导入导出</h2>

    <el-row :gutter="20">
      <el-col :span="12">
        <div class="card">
          <h3>导入敏感词</h3>
          <el-upload
            ref="uploadRef"
            class="upload-area"
            drag
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls"
            :on-change="handleFileChange"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处，或 <em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 .xlsx, .xls 格式</div>
            </template>
          </el-upload>
          <div class="upload-actions">
            <el-button type="primary" :disabled="!uploadFile" :loading="uploading" @click="handleImport">
              开始导入
            </el-button>
            <el-button @click="handleDownloadTemplate">下载模板</el-button>
          </div>
          <div v-if="importResult" class="import-result" :class="importResult.type">
            {{ importResult.message }}
          </div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="card">
          <h3>导出敏感词</h3>
          <el-form :model="exportParams" label-width="80px">
            <el-form-item label="等级">
              <el-select v-model="exportParams.level" placeholder="全部" clearable style="width: 100%">
                <el-option label="紧急" value="URGENT" />
                <el-option label="中等" value="MEDIUM" />
                <el-option label="普通" value="NORMAL" />
              </el-select>
            </el-form-item>
            <el-form-item label="分类">
              <el-select v-model="exportParams.category" placeholder="全部" clearable style="width: 100%">
                <el-option label="政治敏感" value="POLITICAL" />
                <el-option label="暴力恐怖" value="VIOLENCE" />
                <el-option label="色情低俗" value="PORN" />
                <el-option label="广告推广" value="AD" />
                <el-option label="其他" value="OTHER" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="exportParams.status" placeholder="全部" clearable style="width: 100%">
                <el-option label="启用" value="ENABLE" />
                <el-option label="禁用" value="DISABLE" />
              </el-select>
            </el-form-item>
          </el-form>
          <div class="export-actions">
            <el-button type="success" :loading="exporting" @click="handleExport">
              导出 Excel
            </el-button>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import manager from '@/api/manager'

const uploadRef = ref(null)
const uploadFile = ref(null)
const uploading = ref(false)
const exporting = ref(false)
const importResult = ref(null)

const exportParams = reactive({
  level: '',
  category: '',
  status: ''
})

const handleFileChange = (file) => {
  uploadFile.value = file.raw
}

const handleImport = async () => {
  if (!uploadFile.value) {
    ElMessage.warning('请选择要导入的文件')
    return
  }

  uploading.value = true
  importResult.value = null
  try {
    const res = await manager.importWords(uploadFile.value)
    importResult.value = { type: 'success', message: res.message || '导入成功' }
    uploadRef.value?.clearFiles()
    uploadFile.value = null
  } catch (e) {
    importResult.value = { type: 'error', message: e.response?.data?.message || '导入失败' }
  } finally {
    uploading.value = false
  }
}

const handleDownloadTemplate = async () => {
  try {
    const res = await manager.downloadTemplate()
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '敏感词导入模板.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('模板下载成功')
  } catch (e) {
    ElMessage.error('模板下载失败')
  }
}

const handleExport = async () => {
  exporting.value = true
  try {
    const res = await manager.exportWords(exportParams)
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '敏感词导出.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped lang="scss">
.page-title {
  margin-bottom: 24px;
  font-size: 20px;
  font-weight: 600;
}

.card h3 {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 20px;
  color: var(--text-primary);
}

.upload-area {
  margin-bottom: 20px;
}

.upload-actions, .export-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.import-result {
  margin-top: 16px;
  padding: 12px;
  border-radius: 6px;
  font-size: 14px;

  &.success {
    background: rgba(63, 185, 80, 0.1);
    border: 1px solid var(--accent-green);
    color: var(--accent-green);
  }

  &.error {
    background: rgba(248, 81, 73, 0.1);
    border: 1px solid var(--accent-red);
    color: var(--accent-red);
  }
}
</style>
