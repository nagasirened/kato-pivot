<template>
  <div class="app-container">
    <aside class="sidebar">
      <div class="logo">
        <h1>敏感词管理</h1>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>Dashboard</span>
        </el-menu-item>
        <el-menu-item index="/sensitive-word/list">
          <el-icon><Document /></el-icon>
          <span>敏感词管理</span>
        </el-menu-item>
        <el-menu-item index="/sensitive-test">
          <el-icon><Search /></el-icon>
          <span>敏感词测试</span>
        </el-menu-item>
        <el-menu-item index="/import-export">
          <el-icon><UploadFilled /></el-icon>
          <span>导入导出</span>
        </el-menu-item>
        <el-menu-item index="/operation-log">
          <el-icon><Clock /></el-icon>
          <span>操作日志</span>
        </el-menu-item>
      </el-menu>
    </aside>
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { DataBoard, Document, Search, UploadFilled, Clock } from '@element-plus/icons-vue'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped lang="scss">
.app-container {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 200px;
  background: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  flex-shrink: 0;

  .logo {
    padding: 20px;
    border-bottom: 1px solid var(--border-color);

    h1 {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-primary);
      background: var(--gradient-primary);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
  }

  .sidebar-menu {
    border-right: none;
  }
}

.main-content {
  flex: 1;
  background: var(--bg-primary);
  overflow-y: auto;
}
</style>
