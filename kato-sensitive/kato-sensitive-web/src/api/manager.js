import axios from 'axios'

const managerApi = axios.create({
  baseURL: 'http://localhost:8086',
  timeout: 10000
})

managerApi.interceptors.response.use(
  response => response.data,
  error => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

export default {
  // 分页查询敏感词
  pageSensitiveWords(params) {
    return managerApi.get('/sensitive/v1/word/page', { params })
  },

  // 获取敏感词详情
  getSensitiveWord(id) {
    return managerApi.get(`/sensitive/v1/word/${id}`)
  },

  // 新增敏感词
  saveSensitiveWord(data) {
    return managerApi.post('/sensitive/v1/word', data)
  },

  // 更新敏感词
  updateSensitiveWord(data) {
    return managerApi.put('/sensitive/v1/word', data)
  },

  // 删除敏感词
  deleteSensitiveWord(id) {
    return managerApi.delete(`/sensitive/v1/word/${id}`)
  },

  // 导入敏感词
  importWords(file) {
    const formData = new FormData()
    formData.append('file', file)
    return managerApi.post('/sensitive/v1/word/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // 导出敏感词
  exportWords(params) {
    return managerApi.get('/sensitive/v1/word/export', {
      params,
      responseType: 'blob'
    })
  },

  // 下载模板
  downloadTemplate() {
    return managerApi.get('/sensitive/v1/word/template', {
      responseType: 'blob'
    })
  },

  // 操作日志分页
  pageOperationLogs(params) {
    return managerApi.get('/sensitive/v1/word/log/page', { params })
  }
}
