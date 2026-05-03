<template>
  <div class="page-container">
    <div class="page-title">
      <i class="el-icon-document" style="margin-right: 10px;"></i>我的申请
    </div>
    
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="申请类型">
          <el-select v-model="filterForm.type" placeholder="全部类型" clearable @change="loadRequests">
            <el-option label="出库申请" value="出库申请"></el-option>
            <el-option label="退货入库申请" value="退货入库申请"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="申请状态">
          <el-select v-model="filterForm.status" placeholder="全部状态" clearable @change="loadRequests">
            <el-option label="待审核" value="待审核"></el-option>
            <el-option label="已通过" value="已通过"></el-option>
            <el-option label="已拒绝" value="已拒绝"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="loadRequests">查询</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="requests" v-loading="loading" style="width: 100%; margin-top: 20px;">
      <el-table-column prop="id" label="申请编号" width="220">
        <template slot-scope="scope">
          <el-tag size="small" type="info">{{ scope.row.id }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="申请类型" width="130">
        <template slot-scope="scope">
          <el-tag :type="getTypeTagType(scope.row.type)">
            {{ scope.row.type || '出库申请' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template slot-scope="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请明细" min-width="250">
        <template slot-scope="scope">
          <div v-for="(item, index) in scope.row.items" :key="index" class="request-item">
            <span class="item-name">{{ item.partName }}</span>
            <span class="item-quantity">x {{ item.quantity }} {{ item.unit }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="申请数量" width="100">
        <template slot-scope="scope">
          <strong>{{ getTotalQuantity(scope.row.items) }}</strong>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间" width="170">
        <template slot-scope="scope">
          {{ formatDate(scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="reviewTime" label="审核时间" width="170">
        <template slot-scope="scope">
          {{ formatDate(scope.row.reviewTime) || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="审核意见" width="150">
        <template slot-scope="scope">
          <span v-if="scope.row.reviewComment" class="review-comment">
            {{ scope.row.reviewComment }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="viewDetail(scope.row)">
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && requests.length === 0" description="暂无申请记录">
      <el-button type="primary" @click="$router.push('/user/request-submit')" style="margin-right: 10px;">
        提交出库申请
      </el-button>
      <el-button type="success" @click="$router.push('/user/return-request-submit')">
        提交退货入库申请
      </el-button>
    </el-empty>

    <el-dialog title="申请详情" :visible.sync="detailVisible" width="650px">
      <div v-if="currentRequest" class="request-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请编号">
            <el-tag size="small" type="info">{{ currentRequest.id }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请类型">
            <el-tag :type="getTypeTagType(currentRequest.type)">
              {{ currentRequest.type || '出库申请' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请状态">
            <el-tag :type="getStatusType(currentRequest.status)">
              {{ currentRequest.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请时间">
            {{ formatDate(currentRequest.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="审核时间">
            {{ formatDate(currentRequest.reviewTime) || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="detail-section">
          <div class="section-title">申请明细</div>
          <el-table :data="currentRequest.items" style="width: 100%;" size="small">
            <el-table-column prop="partId" label="零件编号" width="120"></el-table-column>
            <el-table-column prop="partName" label="零件名称"></el-table-column>
            <el-table-column prop="category" label="分类" width="80"></el-table-column>
            <el-table-column prop="currentStock" label="申请时库存" width="120">
              <template slot-scope="scope">
                {{ scope.row.currentStock }} {{ scope.row.unit }}
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="申请数量" width="100">
              <template slot-scope="scope">
                <strong>{{ scope.row.quantity }}</strong> {{ scope.row.unit }}
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="detail-section" v-if="currentRequest.remark">
          <div class="section-title">申请备注</div>
          <el-input type="textarea" :rows="2" :value="currentRequest.remark" disabled></el-input>
        </div>

        <div class="detail-section" v-if="!currentRequest.isPending()">
          <div class="section-title">审核信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="审核结果">
              <el-tag :type="currentRequest.isApproved() ? 'success' : 'danger'">
                {{ currentRequest.status }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="审核人IP">
              {{ currentRequest.reviewerIp || '-' }}
            </el-descriptions-item>
          </el-descriptions>
          <el-form-item label="审核意见" style="margin-top: 15px;">
            <el-input
              type="textarea"
              :rows="2"
              :value="currentRequest.reviewComment || '无'"
              disabled
            ></el-input>
          </el-form-item>
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="detailVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import partApi from '../../api/partApi'

export default {
  name: 'UserRequestsList',
  data() {
    return {
      loading: false,
      requests: [],
      filterForm: {
        type: '',
        status: ''
      },
      detailVisible: false,
      currentRequest: null
    }
  },
  created() {
    this.loadRequests()
  },
  methods: {
    async loadRequests() {
      this.loading = true
      try {
        const res = await partApi.user.getMyRequests(this.filterForm.type, this.filterForm.status)
        if (res.success) {
          this.requests = res.data || []
        } else {
          this.$message.error(res.message)
        }
      } catch (error) {
        this.$message.error('加载申请列表失败: ' + error.message)
      } finally {
        this.loading = false
      }
    },
    
    handleReset() {
      this.filterForm = {
        type: '',
        status: ''
      }
      this.loadRequests()
    },
    
    getStatusType(status) {
      switch (status) {
        case '待审核':
          return 'warning'
        case '已通过':
          return 'success'
        case '已拒绝':
          return 'danger'
        default:
          return 'info'
      }
    },
    
    getTypeTagType(type) {
      if (type === '退货入库申请') {
        return 'success'
      }
      return 'primary'
    },
    
    getTotalQuantity(items) {
      if (!items || items.length === 0) return 0
      return items.reduce((sum, item) => sum + item.quantity, 0)
    },
    
    viewDetail(row) {
      this.currentRequest = Object.assign({}, row)
      this.currentRequest.isPending = function() {
        return this.status === '待审核'
      }
      this.currentRequest.isApproved = function() {
        return this.status === '已通过'
      }
      this.currentRequest.isRejected = function() {
        return this.status === '已拒绝'
      }
      this.currentRequest.isOutbound = function() {
        return !this.type || this.type === '出库申请'
      }
      this.currentRequest.isReturn = function() {
        return this.type === '退货入库申请'
      }
      this.detailVisible = true
    },
    
    formatDate(dateStr) {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hour = String(date.getHours()).padStart(2, '0')
      const minute = String(date.getMinutes()).padStart(2, '0')
      const second = String(date.getSeconds()).padStart(2, '0')
      return year + '-' + month + '-' + day + ' ' + hour + ':' + minute + ':' + second
    }
  }
}
</script>

<style scoped>
.filter-card {
  padding: 15px;
}

.request-item {
  padding: 3px 0;
}

.item-name {
  color: #303133;
}

.item-quantity {
  color: #409EFF;
  margin-left: 10px;
}

.review-comment {
  color: #606266;
  font-size: 12px;
}

.request-detail {
  padding: 10px 0;
}

.detail-section {
  margin-top: 20px;
}

.section-title {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #303133;
  border-left: 3px solid #409EFF;
  padding-left: 8px;
}
</style>
