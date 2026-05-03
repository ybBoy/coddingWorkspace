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
  },

  getRecords(params) {
    return api.get('/records', { params: params })
  },

  getRecordsSummary() {
    return api.get('/records/summary')
  },

  user: {
    getVisibleParts() {
      return api.get('/user/parts')
    },

    getVisiblePartById(id) {
      return api.get('/user/parts/' + id)
    },

    searchVisibleParts(params) {
      return api.get('/user/parts/search', { params: params })
    },

    getVisiblePartsNeedRestock() {
      return api.get('/user/parts/need-restock')
    },

    stockIn(partId, quantity) {
      return api.post('/user/parts/stock-in', { partId, quantity })
    },

    stockOut(partId, quantity) {
      return api.post('/user/parts/stock-out', { partId, quantity })
    },

    getRecords(params) {
      return api.get('/user/records', { params: params })
    },

    getRecordsSummary() {
      return api.get('/user/records/summary')
    }
  },

  admin: {
    getAllParts() {
      return api.get('/admin/parts')
    },

    getPartById(id) {
      return api.get('/admin/parts/' + id)
    },

    addPart(part) {
      return api.post('/admin/parts', part)
    },

    updatePart(id, part) {
      return api.put('/admin/parts/' + id, part)
    },

    deletePart(id) {
      return api.delete('/admin/parts/' + id)
    },

    updateVisibility(id, visible) {
      return api.put('/admin/parts/' + id + '/visibility', { visible })
    },

    searchParts(params) {
      return api.get('/admin/parts/search', { params: params })
    },

    getPartsNeedRestock() {
      return api.get('/admin/parts/need-restock')
    },

    stockIn(partId, quantity) {
      return api.post('/user/parts/stock-in', { partId, quantity })
    },

    stockOut(partId, quantity) {
      return api.post('/user/parts/stock-out', { partId, quantity })
    },

    getRecords(params) {
      return api.get('/records', { params: params })
    },

    getRecordsSummary() {
      return api.get('/records/summary')
    }
  }
}

export default partApi
