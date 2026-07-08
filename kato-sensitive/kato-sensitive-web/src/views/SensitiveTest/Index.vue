<template>
  <div class="page-container">
    <h2 class="page-title">敏感词检测</h2>

    <el-row :gutter="20">
      <el-col :span="12">
        <div class="card input-card">
          <h3>输入文本</h3>
          <el-input
            v-model="text"
            type="textarea"
            :rows="10"
            placeholder="请输入要检测的文本..."
            @input="handleTextChange"
          />
          <div class="input-actions">
            <el-button type="primary" :loading="checking" @click="handleCheck">
              检测
            </el-button>
            <el-button @click="handleClear">清空</el-button>
          </div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="card result-card">
          <h3>检测结果</h3>
          <div v-if="!result" class="empty-state">
            <el-icon :size="48"><Document /></el-icon>
            <p>输入文本后点击检测</p>
          </div>
          <div v-else class="result-content">
            <div class="result-header">
              <el-tag :type="result.containsSensitive ? 'danger' : 'success'" size="large">
                {{ result.containsSensitive ? '发现敏感词' : '未发现敏感词' }}
              </el-tag>
              <span class="hit-count" v-if="result.containsSensitive">
                命中 {{ result.hitCount }} 个
              </span>
            </div>

            <div v-if="result.containsSensitive" class="result-details">
              <div class="detail-item">
                <span class="label">最高等级：</span>
                <el-tag :type="getLevelType(result.maxLevel)">
                  {{ getLevelText(result.maxLevel) }}
                </el-tag>
              </div>
              <div class="detail-item">
                <span class="label">命中分类：</span>
                <el-tag v-for="cat in result.categories" :key="cat" size="small">
                  {{ cat }}
                </el-tag>
              </div>
              <div class="detail-item">
                <span class="label">命中词列表：</span>
                <div class="hit-words">
                  <el-tag
                    v-for="word in result.hitWords"
                    :key="word.word"
                    size="small"
                    :type="getLevelType(word.level)"
                  >
                    {{ word.word }}
                  </el-tag>
                </div>
              </div>
              <div class="sanitize-preview" v-if="showSanitize">
                <span class="label">脱敏预览：</span>
                <div class="sanitize-text" v-html="sanitizedText"></div>
              </div>
              <div class="sanitize-toggle">
                <el-checkbox v-model="showSanitize" @change="handleSanitizeToggle">
                  脱敏预览
                </el-checkbox>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import { debounce } from 'lodash-es'
import checkApi from '@/api/check'

const text = ref('')
const checking = ref(false)
const result = ref(null)
const showSanitize = ref(false)
const sanitizedText = ref('')

const getLevelType = (level) => {
  const map = { URGENT: 'danger', MEDIUM: 'warning', NORMAL: '' }
  return map[level] || ''
}

const getLevelText = (level) => {
  const map = { URGENT: '紧急', MEDIUM: '中等', NORMAL: '普通' }
  return map[level] || level
}

const handleCheck = async () => {
  if (!text.value.trim()) {
    ElMessage.warning('请输入要检测的文本')
    return
  }

  checking.value = true
  try {
    const res = await checkApi.check({
      text: text.value,
      wordType: '',
      fuzzyMatch: true,
      sanitize: showSanitize.value
    })
    result.value = res.data || res

    if (showSanitize.value && result.value.sanitizedText) {
      sanitizedText.value = highlightText(text.value, result.value.hitWords)
    }
  } catch (e) {
    ElMessage.error('检测失败')
  } finally {
    checking.value = false
  }
}

const handleClear = () => {
  text.value = ''
  result.value = null
  sanitizedText.value = ''
}

const handleTextChange = debounce(() => {
  if (text.value.trim().length > 10) {
    handleCheck()
  }
}, 300)

const handleSanitizeToggle = () => {
  if (showSanitize.value && result.value?.hitWords) {
    sanitizedText.value = highlightText(text.value, result.value.hitWords)
  }
}

const highlightText = (original, hitWords) => {
  let highlighted = original
  hitWords.forEach(({ word }) => {
    const regex = new RegExp(word.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g')
    highlighted = highlighted.replace(regex, `<mark style="background: var(--accent-red); color: white; padding: 0 2px;">${word}</mark>`)
  })
  return highlighted
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
  margin-bottom: 16px;
  color: var(--text-primary);
}

.input-card, .result-card {
  height: 100%;
}

.input-actions {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: var(--text-secondary);

  p {
    margin-top: 16px;
  }
}

.result-content {
  .result-header {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;

    .hit-count {
      color: var(--text-secondary);
    }
  }

  .result-details {
    .detail-item {
      margin-bottom: 16px;

      .label {
        color: var(--text-secondary);
        font-size: 14px;
        display: block;
        margin-bottom: 8px;
      }

      .hit-words {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
      }
    }

    .sanitize-preview {
      margin-top: 16px;

      .label {
        color: var(--text-secondary);
        font-size: 14px;
        display: block;
        margin-bottom: 8px;
      }

      .sanitize-text {
        padding: 12px;
        background: var(--bg-tertiary);
        border-radius: 6px;
        line-height: 1.6;
        word-break: break-all;
      }
    }

    .sanitize-toggle {
      margin-top: 16px;
    }
  }
}
</style>
