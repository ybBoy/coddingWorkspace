import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

api.interceptors.response.use(
  response => response.data,
  error => {
    console.error('API请求错误:', error)
    let message = '网络错误，请稍后重试'
    if (error.response && error.response.data && error.response.data.message) {
      message = error.response.data.message
    }
    return Promise.reject(new Error(message))
  }
)

const partApi = {
  getAllParts() {
    return api.get('/parts')
  },

  getPartById(id) {
    return api.get('/parts/' + id)
  },

  addPart(part) {
    return api.post('/parts', part)
  },

  updatePart(id, part) {
    return api.put('/parts/' + id, part)
  },

  deletePart(id) {
    return api.delete('/parts/' + id)
  },

  stockIn(partId, quantity) {
    return api.post('/parts/stock-in', { partId, quantity })
  },

  stockOut(partId, quantity) {
    return api.post('/parts/stock-out', { partId, quantity })
  },

  searchParts(keyword) {
    return api.get('/parts/search', { params: { keyword } })
  },

  getPartsNeedRestock() {
    return api.get('/parts/need-restock')
  }
}

export default partApi
