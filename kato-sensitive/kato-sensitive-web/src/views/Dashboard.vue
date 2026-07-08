<template>
  <div class="page-container">
    <h2 class="page-title">数据概览</h2>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value text-info">{{ stats.total }}</div>
          <div class="stat-label">敏感词总数</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value text-danger">{{ stats.urgent }}</div>
          <div class="stat-label">紧急级别</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value text-warning">{{ stats.medium }}</div>
          <div class="stat-label">中等级别</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value text-success">{{ stats.normal }}</div>
          <div class="stat-label">普通级别</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="content-row">
      <el-col :span="16">
        <div class="card">
          <h3>分类分布</h3>
          <div class="category-chart">
            <div
              v-for="item in categoryStats"
              :key="item.category"
              class="category-item"
            >
              <span class="category-name">{{ item.category }}</span>
              <div class="category-bar">
                <div
                  class="category-fill"
                  :style="{ width: item.percent + '%' }"
                ></div>
              </div>
              <span class="category-count">{{ item.count }}</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="card">
          <h3>快捷入口</h3>
          <div class="quick-actions">
            <el-button type="primary" @click="$router.push('/sensitive-word/list')">
              敏感词管理
            </el-button>
            <el-button type="success" @click="$router.push('/sensitive-test')">
              敏感词测试
            </el-button>
            <el-button type="warning" @click="$router.push('/import-export')">
              导入导出
            </el-button>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row>
      <el-col :span="24">
        <div class="card">
          <h3>最近操作日志</h3>
          <el-table :data="recentLogs" stripe>
            <el-table-column prop="operationType" label="操作类型" width="120" />
            <el-table-column prop="operationContent" label="操作内容" />
            <el-table-column prop="operator" label="操作人" width="120" />
            <el-table-column prop="operationTime" label="操作时间" width="180" />
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import manager from '@/api/manager'

const stats = ref({ total: 0, urgent: 0, medium: 0, normal: 0 })
const categoryStats = ref([])
const recentLogs = ref([])

onMounted(async () => {
  try {
    const res = await manager.pageSensitiveWords({ current: 1, size: 100 })
    if (res.data?.records) {
      const records = res.data.records
      stats.value.total = records.length
      stats.value.urgent = records.filter(r => r.level === 'URGENT').length
      stats.value.medium = records.filter(r => r.level === 'MEDIUM').length
      stats.value.normal = records.filter(r => r.level === 'NORMAL').length

      const categoryMap = {}
      records.forEach(r => {
        if (r.category) {
          categoryMap[r.category] = (categoryMap[r.category] || 0) + 1
        }
      })
      const total = records.length || 1
      categoryStats.value = Object.entries(categoryMap)
        .map(([category, count]) => ({
          category,
          count,
          percent: Math.round((count / total) * 100)
        }))
        .sort((a, b) => b.count - a.count)
        .slice(0, 5)
    }
  } catch (e) {
    console.error('Failed to load stats:', e)
  }

  try {
    const logRes = await manager.pageOperationLogs({ current: 1, size: 5 })
    if (logRes.data?.records) {
      recentLogs.value = logRes.data.records
    }
  } catch (e) {
    console.error('Failed to load logs:', e)
  }
})
</script>

<style scoped lang="scss">
.page-title {
  margin-bottom: 24px;
  font-size: 20px;
  font-weight: 600;
}

.stats-row {
  margin-bottom: 20px;
}

.content-row {
  margin-bottom: 20px;
}

.category-chart {
  margin-top: 16px;
}

.category-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;

  .category-name {
    width: 80px;
    color: var(--text-secondary);
    font-size: 14px;
  }

  .category-bar {
    flex: 1;
    height: 8px;
    background: var(--bg-tertiary);
    border-radius: 4px;
    margin: 0 12px;
    overflow: hidden;

    .category-fill {
      height: 100%;
      background: var(--gradient-primary);
      border-radius: 4px;
      transition: width 0.3s ease;
    }
  }

  .category-count {
    width: 40px;
    text-align: right;
    color: var(--text-primary);
    font-weight: 500;
  }
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;

  .el-button {
    width: 100%;
  }
}

.card h3 {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 16px;
  color: var(--text-primary);
}
</style>
