import axios from 'axios'

/**
 * API客户端配置
 * 基础路径：/api（通过vue.config.js代理到后端8080端口）
 * 超时时间：10秒
 */
const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

/**
 * 响应拦截器
 * 统一处理API响应，只返回response.data
 * 统一处理错误信息
 */
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

/**
 * 零件相关API封装
 * 包含通用API、用户API和管理员API三组接口
 */
const partApi = {
  // ==================== 通用API ====================

  /**
   * 获取所有零件列表
   * @returns {Promise} API响应
   */
  getAllParts() {
    return api.get('/parts')
  },

  /**
   * 根据编号获取零件详情
   * @param {string} id - 零件编号
   * @returns {Promise} API响应
   */
  getPartById(id) {
    return api.get('/parts/' + id)
  },

  /**
   * 添加新零件
   * @param {Object} part - 零件对象
   * @returns {Promise} API响应
   */
  addPart(part) {
    return api.post('/parts', part)
  },

  /**
   * 更新零件信息
   * @param {string} id - 零件编号
   * @param {Object} part - 零件对象
   * @returns {Promise} API响应
   */
  updatePart(id, part) {
    return api.put('/parts/' + id, part)
  },

  /**
   * 删除零件
   * @param {string} id - 零件编号
   * @returns {Promise} API响应
   */
  deletePart(id) {
    return api.delete('/parts/' + id)
  },

  /**
   * 零件入库操作
   * @param {string} partId - 零件编号
   * @param {number} quantity - 入库数量
   * @returns {Promise} API响应
   */
  stockIn(partId, quantity) {
    return api.post('/parts/stock-in', { partId, quantity })
  },

  /**
   * 零件出库操作
   * @param {string} partId - 零件编号
   * @param {number} quantity - 出库数量
   * @returns {Promise} API响应
   */
  stockOut(partId, quantity) {
    return api.post('/parts/stock-out', { partId, quantity })
  },

  /**
   * 搜索零件
   * @param {string} keyword - 搜索关键词
   * @returns {Promise} API响应
   */
  searchParts(keyword) {
    return api.get('/parts/search', { params: { keyword } })
  },

  /**
   * 获取需要补货的零件列表
   * @returns {Promise} API响应
   */
  getPartsNeedRestock() {
    return api.get('/parts/need-restock')
  },

  /**
   * 获取库存记录列表
   * @param {Object} params - 查询参数（type, category, partId）
   * @returns {Promise} API响应
   */
  getRecords(params) {
    return api.get('/records', { params: params })
  },

  /**
   * 获取库存记录统计汇总
   * @returns {Promise} API响应
   */
  getRecordsSummary() {
    return api.get('/records/summary')
  },

  // ==================== 普通用户API ====================
  /**
   * 普通用户专用API
   * 只能访问可见（visible=true）的零件
   */
  user: {
    /**
     * 获取可见零件列表
     * @returns {Promise} API响应
     */
    getVisibleParts() {
      return api.get('/user/parts')
    },

    /**
     * 根据编号获取可见零件详情
     * @param {string} id - 零件编号
     * @returns {Promise} API响应
     */
    getVisiblePartById(id) {
      return api.get('/user/parts/' + id)
    },

    /**
     * 搜索可见零件
     * @param {Object} params - 查询参数（keyword, category）
     * @returns {Promise} API响应
     */
    searchVisibleParts(params) {
      return api.get('/user/parts/search', { params: params })
    },

    /**
     * 获取需要补货的可见零件列表
     * @returns {Promise} API响应
     */
    getVisiblePartsNeedRestock() {
      return api.get('/user/parts/need-restock')
    },

    /**
     * 零件入库操作（用户视角）
     * @param {string} partId - 零件编号
     * @param {number} quantity - 入库数量
     * @returns {Promise} API响应
     */
    stockIn(partId, quantity) {
      return api.post('/user/parts/stock-in', { partId, quantity })
    },

    /**
     * 零件出库操作（用户视角）
     * @param {string} partId - 零件编号
     * @param {number} quantity - 出库数量
     * @returns {Promise} API响应
     */
    stockOut(partId, quantity) {
      return api.post('/user/parts/stock-out', { partId, quantity })
    },

    /**
     * 获取库存记录列表（用户视角）
     * @param {Object} params - 查询参数
     * @returns {Promise} API响应
     */
    getRecords(params) {
      return api.get('/user/records', { params: params })
    },

    /**
     * 获取库存记录统计汇总（用户视角）
     * @returns {Promise} API响应
     */
    getRecordsSummary() {
      return api.get('/user/records/summary')
    },

    // ==================== 出库申请API ====================
    /**
     * 提交出库申请
     * @param {Object} requestData - 申请数据
     * @param {Array} requestData.items - 申请明细列表
     * @param {string} requestData.remark - 申请备注
     * @returns {Promise} API响应
     */
    submitRequest(requestData) {
      return api.post('/user/requests', requestData)
    },

    /**
     * 获取我的申请列表
     * @param {string} status - 可选的状态筛选
     * @returns {Promise} API响应
     */
    getMyRequests(status) {
      const params = {}
      if (status) {
        params.status = status
      }
      return api.get('/user/requests', { params: params })
    },

    /**
     * 获取我的申请详情
     * @param {string} id - 申请编号
     * @returns {Promise} API响应
     */
    getMyRequestById(id) {
      return api.get('/user/requests/' + id)
    },

    /**
     * 获取我的待审核申请数量
     * @returns {Promise} API响应
     */
    getMyPendingCount() {
      return api.get('/user/requests/pending/count')
    }
  },

  // ==================== 管理员API ====================
  /**
   * 管理员专用API
   * 可访问所有零件，包含零件管理、可见性设置等权限
   */
  admin: {
    /**
     * 获取所有零件列表（管理员）
     * @returns {Promise} API响应
     */
    getAllParts() {
      return api.get('/admin/parts')
    },

    /**
     * 根据编号获取零件详情（管理员）
     * @param {string} id - 零件编号
     * @returns {Promise} API响应
     */
    getPartById(id) {
      return api.get('/admin/parts/' + id)
    },

    /**
     * 添加新零件（管理员）
     * @param {Object} part - 零件对象
     * @returns {Promise} API响应
     */
    addPart(part) {
      return api.post('/admin/parts', part)
    },

    /**
     * 更新零件信息（管理员）
     * @param {string} id - 零件编号
     * @param {Object} part - 零件对象
     * @returns {Promise} API响应
     */
    updatePart(id, part) {
      return api.put('/admin/parts/' + id, part)
    },

    /**
     * 删除零件（管理员）
     * @param {string} id - 零件编号
     * @returns {Promise} API响应
     */
    deletePart(id) {
      return api.delete('/admin/parts/' + id)
    },

    /**
     * 更新零件可见性（管理员）
     * @param {string} id - 零件编号
     * @param {boolean} visible - 是否对用户可见
     * @returns {Promise} API响应
     */
    updateVisibility(id, visible) {
      return api.put('/admin/parts/' + id + '/visibility', { visible })
    },

    /**
     * 搜索零件（管理员）
     * @param {Object} params - 查询参数（keyword, category）
     * @returns {Promise} API响应
     */
    searchParts(params) {
      return api.get('/admin/parts/search', { params: params })
    },

    /**
     * 获取需要补货的零件列表（管理员）
     * @returns {Promise} API响应
     */
    getPartsNeedRestock() {
      return api.get('/admin/parts/need-restock')
    },

    /**
     * 零件入库操作（管理员）
     * @param {string} partId - 零件编号
     * @param {number} quantity - 入库数量
     * @returns {Promise} API响应
     */
    stockIn(partId, quantity) {
      return api.post('/user/parts/stock-in', { partId, quantity })
    },

    /**
     * 零件出库操作（管理员）
     * @param {string} partId - 零件编号
     * @param {number} quantity - 出库数量
     * @returns {Promise} API响应
     */
    stockOut(partId, quantity) {
      return api.post('/user/parts/stock-out', { partId, quantity })
    },

    /**
     * 获取库存记录列表（管理员）
     * @param {Object} params - 查询参数
     * @returns {Promise} API响应
     */
    getRecords(params) {
      return api.get('/records', { params: params })
    },

    /**
     * 获取库存记录统计汇总（管理员）
     * @returns {Promise} API响应
     */
    getRecordsSummary() {
      return api.get('/records/summary')
    },

    // ==================== 出库申请API ====================
    /**
     * 获取所有申请列表
     * @param {string} status - 可选的状态筛选
     * @returns {Promise} API响应
     */
    getAllRequests(status) {
      const params = {}
      if (status) {
        params.status = status
      }
      return api.get('/admin/requests', { params: params })
    },

    /**
     * 获取待审核申请列表
     * @returns {Promise} API响应
     */
    getPendingRequests() {
      return api.get('/admin/requests/pending')
    },

    /**
     * 获取申请详情
     * @param {string} id - 申请编号
     * @returns {Promise} API响应
     */
    getRequestById(id) {
      return api.get('/admin/requests/' + id)
    },

    /**
     * 审核申请
     * @param {string} id - 申请编号
     * @param {boolean} approved - 是否通过
     * @param {string} comment - 审核意见
     * @returns {Promise} API响应
     */
    reviewRequest(id, approved, comment) {
      return api.put('/admin/requests/' + id + '/review', { approved, comment })
    },

    /**
     * 批量审核通过申请
     * @param {Array} ids - 申请编号数组
     * @returns {Promise} API响应
     */
    batchApprove(ids) {
      return api.post('/admin/requests/batch-approve', ids)
    },

    /**
     * 获取待审核申请数量
     * @returns {Promise} API响应
     */
    getPendingRequestCount() {
      return api.get('/admin/requests/pending/count')
    }
  }
}

export default partApi
