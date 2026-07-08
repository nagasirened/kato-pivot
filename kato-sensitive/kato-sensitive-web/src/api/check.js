import axios from 'axios'

const checkApi = axios.create({
  baseURL: 'http://localhost:8087',
  timeout: 10000
})

checkApi.interceptors.response.use(
  response => response.data,
  error => {
    console.error('Check API Error:', error)
    return Promise.reject(error)
  }
)

export default {
  // 检测敏感词
  check(data) {
    return checkApi.post('/sensitive/v1/check', data)
  },

  // 精确检测
  exactCheck(data) {
    return checkApi.post('/sensitive/v1/check/exact', data)
  },

  // 模糊检测
  fuzzyCheck(data) {
    return checkApi.post('/sensitive/v1/check/fuzzy', data)
  }
}
