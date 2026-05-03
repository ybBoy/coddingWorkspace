<template>
  <div class="page-container">
    <div class="page-title">
      <i class="el-icon-s-claim" style="margin-right: 10px;"></i>申请审核
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

    <el-table :data="requests" v-loading="loading" style="width: 100%;" border>
      <el-table-column type="selection" width="55">
      </el-table-column>
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
      <el-table-column prop="applicantIp" label="申请人IP" width="120">
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间" width="170">
        <template slot-scope="scope">
          {{ formatDate(scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="viewDetail(scope.row)">
            详情
          </el-button>
          <template v-if="scope.row.status === '待审核'">
            <el-button type="text" size="small" @click="handleApprove(scope.row)">
              通过
            </el-button>
            <el-button type="text" size="small" @click="handleReject(scope.row)">
              拒绝
            </el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && requests.length === 0" description="暂无申请记录"></el-empty>

    <el-dialog title="申请详情" :visible.sync="detailVisible" width="700px">
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
          <el-descriptions-item label="申请人IP">
            {{ currentRequest.applicantIp }}
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
          <el-table :data="currentRequest.items" style="width: 100%;" size="small" border>
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

        <div class="detail-section" v-if="!isPending(currentRequest)">
          <div class="section-title">审核信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="审核结果">
              <el-tag :type="isApproved(currentRequest) ? 'success' : 'danger'">
                {{ currentRequest.status }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="审核人IP">
              {{ currentRequest.reviewerIp || '-' }}
            </el-descriptions-item>
          </el-descriptions>
          <div style="margin-top: 15px;">
            <label style="font-weight: bold;">审核意见：</label>
            <el-input
              type="textarea"
              :rows="2"
              :value="currentRequest.reviewComment || '无'"
              disabled
            ></el-input>
          </div>
        </div>
      </div>
      <span slot="footer">
        <template v-if="currentRequest && currentRequest.status === '待审核'">
          <el-button type="success" @click="confirmApprove">通过</el-button>
          <el-button type="danger" @click="confirmReject">拒绝</el-button>
        </template>
        <el-button @click="detailVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import partApi from '../../api/partApi'
import { eventBus } from '../../utils/eventBus'

export default {
  name: 'AdminRequestsReview',
  data() {
    return {
      loading: false,
      reviewing: false,
      requests: [],
      selectedIds: [],
      filterForm: {
        type: '',
        status: '待审核'
      },
      detailVisible: false,
      currentRequest: null,
      reviewForm: {
        comment: ''
      }
    }
  },
  created() {
    this.loadRequests()
  },
  methods: {
    async loadRequests() {
      this.loading = true
      try {
        const res = await partApi.admin.getAllRequests(this.filterForm.type, this.filterForm.status)
        if (res.success) {
          this.requests = res.data || []
          this.selectedIds = []
        }
      } catch (error) {
        this.$message.error('加载失败')
      } finally {
        this.loading = false
      }
    },
    
    handleReset() {
      this.filterForm = { type: '', status: '待审核' }
      this.loadRequests()
    },
    
    handleSelectionChange(selection) {
      this.selectedIds = selection.map(function(r) { return r.id })
    },
    
    getStatusType(status) {
      if (status === '待审核') return 'warning'
      if (status === '已通过') return 'success'
      if (status === '已拒绝') return 'danger'
      return 'info'
    },
    
    getTypeTagType(type) {
      if (type === '退货入库申请') {
        return 'success'
      }
      return 'primary'
    },
    
    isPending(request) {
      return request && request.status === '待审核'
    },
    
    isApproved(request) {
      return request && request.status === '已通过'
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
    },
    
    viewDetail(row) {
      this.currentRequest = Object.assign({}, row)
      this.detailVisible = true
    },
    
    handleApprove(row) {
      this.currentRequest = Object.assign({}, row)
      this.detailVisible = true
    },
    
    handleReject(row) {
      this.$confirm('确定要拒绝该申请吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(function() {
        return partApi.admin.reviewRequest(row.id, false, '拒绝')
      }).then(function(res) {
        if (res.success) {
          this.$message.success('已拒绝')
          this.loadRequests()
          eventBus.$emit('request-reviewed')
        }
      }.bind(this)).catch(function() {
      })
    },
    
    async confirmApprove() {
      if (!this.currentRequest) return
      try {
        const res = await partApi.admin.reviewRequest(this.currentRequest.id, true, '')
        if (res.success) {
          this.$message.success('审核通过')
          this.detailVisible = false
          this.loadRequests()
          eventBus.$emit('request-reviewed')
        }
      } catch (error) {
        this.$message.error('操作失败')
      }
    },
    
    async confirmReject() {
      if (!this.currentRequest) return
      try {
        const res = await partApi.admin.reviewRequest(this.currentRequest.id, false, '拒绝')
        if (res.success) {
          this.$message.success('已拒绝')
          this.detailVisible = false
          this.loadRequests()
          eventBus.$emit('request-reviewed')
        }
      } catch (error) {
        this.$message.error('操作失败')
      }
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
