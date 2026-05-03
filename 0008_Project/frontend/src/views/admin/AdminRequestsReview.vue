<template>
  <div class="page-container">
    <div class="page-title">
      出库申请审核
    </div>
    
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm">
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

    <el-table :data="requests" v-loading="loading">
      <el-table-column type="selection" width="55">
      </el-table-column>
      <el-table-column prop="id" label="申请编号" width="220">
        <template slot-scope="scope">
          <el-tag size="small" type="info">{{ scope.row.id }}</el-tag>
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
          <div v-for="(item, index) in scope.row.items" :key="index">
            <span>{{ item.partName }} x {{ item.quantity }} {{ item.unit }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="applicantIp" label="申请人IP" width="120">
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间" width="170">
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
      <div v-if="currentRequest">
        <p>申请编号: {{ currentRequest.id }}</p>
        <p>状态: {{ currentRequest.status }}</p>
        <p>申请人IP: {{ currentRequest.applicantIp }}</p>
        <p>申请时间: {{ currentRequest.createTime }}</p>
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

export default {
  name: 'AdminRequestsReview',
  data() {
    return {
      loading: false,
      reviewing: false,
      requests: [],
      selectedIds: [],
      filterForm: {
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
        const res = await partApi.admin.getAllRequests(this.filterForm.status)
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
      this.filterForm = { status: '待审核' }
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
    
    viewDetail(row) {
      this.currentRequest = row
      this.detailVisible = true
    },
    
    handleApprove(row) {
      this.currentRequest = row
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
</style>
