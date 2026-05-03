<template>
  <div class="page-container">
    <div class="page-title">
      <i class="el-icon-document" style="margin-right: 10px;"></i>出入库记录（管理员）
    </div>
    
    <el-card class="summary-card" v-if="summary">
      <div slot="header">
        <span>出入库汇总（按分类统计）</span>
      </div>
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="summary-item">
            <div class="summary-label">总记录数</div>
            <div class="summary-value">{{ summary.totalRecords || 0 }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="summary-item">
            <div class="summary-label">总入库数量</div>
            <div class="summary-value success">{{ summary.totalStockIn || 0 }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="summary-item">
            <div class="summary-label">总出库数量</div>
            <div class="summary-value danger">{{ summary.totalStockOut || 0 }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="summary-item">
            <div class="summary-label">净变化</div>
            <div class="summary-value" :class="{'success': (summary.totalStockIn || 0) - (summary.totalStockOut || 0) >= 0, 'danger': (summary.totalStockIn || 0) - (summary.totalStockOut || 0) < 0}">
              {{ (summary.totalStockIn || 0) - (summary.totalStockOut || 0) }}
            </div>
          </div>
        </el-col>
      </el-row>
      
      <el-divider></el-divider>
      
      <el-table :data="summary.categorySummary || []" style="width: 100%" size="small">
        <el-table-column prop="category" label="分类" width="120"></el-table-column>
        <el-table-column prop="stockInQuantity" label="入库数量" width="120">
          <template slot-scope="scope">
            <span class="success">{{ scope.row.stockInQuantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stockOutQuantity" label="出库数量" width="120">
          <template slot-scope="scope">
            <span class="danger">{{ scope.row.stockOutQuantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="balance" label="净变化" width="120">
          <template slot-scope="scope">
            <span :class="{'success': scope.row.balance >= 0, 'danger': scope.row.balance < 0}">
              {{ scope.row.balance }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="filter-card" style="margin-top: 20px;">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="操作类型">
          <el-select v-model="filterForm.type" placeholder="全部类型" clearable style="width: 120px;">
            <el-option label="入库" value="入库"></el-option>
            <el-option label="出库" value="出库"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="filterForm.category" placeholder="全部分类" clearable style="width: 120px;">
            <el-option label="电池" value="电池"></el-option>
            <el-option label="电机" value="电机"></el-option>
            <el-option label="轮胎" value="轮胎"></el-option>
            <el-option label="控制器" value="控制器"></el-option>
            <el-option label="玻璃" value="玻璃"></el-option>
            <el-option label="机油" value="机油"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="IP地址">
          <el-input
            v-model="filterForm.ipAddress"
            placeholder="按IP地址筛选"
            clearable
            style="width: 150px;"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="loadRecords">查询</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="filteredRecords" v-loading="loading" style="width: 100%; margin-top: 20px;">
      <el-table-column prop="id" label="记录编号" width="200">
        <template slot-scope="scope">
          <el-tag size="mini" type="info">{{ scope.row.id }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="操作类型" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.type === '入库' ? 'success' : 'danger'">
            {{ scope.row.type }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="partId" label="零件编号" width="100"></el-table-column>
      <el-table-column prop="partName" label="零件名称" width="120"></el-table-column>
      <el-table-column prop="category" label="分类" width="80"></el-table-column>
      <el-table-column prop="quantity" label="数量" width="80">
        <template slot-scope="scope">
          <span :class="{'success': scope.row.type === '入库', 'danger': scope.row.type === '出库'}">
            {{ scope.row.type === '入库' ? '+' : '-' }}{{ scope.row.quantity }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="unit" label="单位" width="60"></el-table-column>
      <el-table-column label="库存变化" width="150">
        <template slot-scope="scope">
          <span>{{ scope.row.beforeQuantity }} → {{ scope.row.afterQuantity }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="ipAddress" label="操作IP地址" width="150">
        <template slot-scope="scope">
          <el-tag size="mini" :type="getIpTagType(scope.row.ipAddress)">
            {{ scope.row.ipAddress || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="操作时间" width="180">
        <template slot-scope="scope">
          {{ formatDate(scope.row.createTime) }}
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && filteredRecords.length === 0" description="暂无出入库记录"></el-empty>
  </div>
</template>

<script>
import partApi from '../../api/partApi'

export default {
  name: 'AdminRecordsList',
  data() {
    return {
      loading: false,
      records: [],
      summary: null,
      filterForm: {
        type: '',
        category: '',
        ipAddress: ''
      }
    }
  },
  computed: {
    filteredRecords() {
      if (!this.filterForm.ipAddress) {
        return this.records
      }
      const ip = this.filterForm.ipAddress.toLowerCase().trim()
      return this.records.filter(r => 
        r.ipAddress && r.ipAddress.toLowerCase().includes(ip)
      )
    }
  },
  created() {
    this.loadSummary()
    this.loadRecords()
  },
  methods: {
    async loadSummary() {
      try {
        const res = await partApi.admin.getRecordsSummary()
        if (res.success) {
          this.summary = res.data || {}
        }
      } catch (error) {
        console.error('加载汇总数据失败:', error)
      }
    },

    async loadRecords() {
      this.loading = true
      try {
        const params = {}
        if (this.filterForm.type) {
          params.type = this.filterForm.type
        }
        if (this.filterForm.category) {
          params.category = this.filterForm.category
        }
        
        const res = await partApi.admin.getRecords(params)
        if (res.success) {
          this.records = res.data || []
        } else {
          this.$message.error(res.message)
        }
      } catch (error) {
        this.$message.error('加载记录失败: ' + error.message)
      } finally {
        this.loading = false
      }
    },

    handleReset() {
      this.filterForm = {
        type: '',
        category: '',
        ipAddress: ''
      }
      this.loadRecords()
    },

    getIpTagType(ipAddress) {
      if (!ipAddress || ipAddress === 'unknown') {
        return 'info'
      }
      if (ipAddress.startsWith('192.168') || ipAddress.startsWith('10.') || ipAddress.startsWith('172.')) {
        return 'primary'
      }
      return 'warning'
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
.summary-card {
  margin-bottom: 20px;
}

.summary-item {
  text-align: center;
  padding: 10px;
}

.summary-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.summary-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.summary-value.success {
  color: #67c23a;
}

.summary-value.danger {
  color: #f56c6c;
}

.success {
  color: #67c23a;
  font-weight: bold;
}

.danger {
  color: #f56c6c;
  font-weight: bold;
}

.filter-card {
  padding: 15px;
}
</style>
