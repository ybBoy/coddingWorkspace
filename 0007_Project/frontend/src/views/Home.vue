<template>
  <div class="page-container">
    <div class="page-title">
      <i class="el-icon-s-home" style="margin-right: 10px;"></i>系统概览
    </div>
    
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <div class="stat-card total-parts">
          <div class="stat-icon">
            <i class="el-icon-s-grid"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalParts }}</div>
            <div class="stat-label">零件种类</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card need-restock">
          <div class="stat-icon">
            <i class="el-icon-warning"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.needRestock }}</div>
            <div class="stat-label">需补货种类</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card normal-stock">
          <div class="stat-icon">
            <i class="el-icon-success"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.normalStock }}</div>
            <div class="stat-label">库存正常</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card total-quantity">
          <div class="stat-icon">
            <i class="el-icon-s-goods"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalQuantity }}</div>
            <div class="stat-label">总库存量</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 30px;">
      <el-col :span="12">
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title">需补货提醒</span>
            <el-button type="text" @click="$router.push('/restock')">查看全部</el-button>
          </div>
          <el-table :data="restockParts" v-loading="loading" style="width: 100%">
            <el-table-column prop="id" label="零件编号" width="120"></el-table-column>
            <el-table-column prop="name" label="零件名称"></el-table-column>
            <el-table-column prop="specification" label="规格" width="150"></el-table-column>
            <el-table-column prop="quantity" label="当前库存" width="100">
              <template slot-scope="scope">
                <span class="danger-text">{{ scope.row.quantity }}</span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && restockParts.length === 0" description="暂无需补货零件"></el-empty>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title">快捷操作</span>
          </div>
          <div class="quick-actions">
            <el-card shadow="hover" class="action-card" @click.native="$router.push('/parts')">
              <div class="action-icon">
                <i class="el-icon-s-grid"></i>
              </div>
              <div class="action-text">库存管理</div>
            </el-card>
            <el-card shadow="hover" class="action-card" @click.native="$router.push('/stock')">
              <div class="action-icon">
                <i class="el-icon-upload2"></i>
              </div>
              <div class="action-text">入库/出库</div>
            </el-card>
            <el-card shadow="hover" class="action-card" @click.native="$router.push('/search')">
              <div class="action-icon">
                <i class="el-icon-search"></i>
              </div>
              <div class="action-text">零件搜索</div>
            </el-card>
            <el-card shadow="hover" class="action-card" @click.native="$router.push('/restock')">
              <div class="action-icon">
                <i class="el-icon-warning"></i>
              </div>
              <div class="action-text">补货管理</div>
            </el-card>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import partApi from '../api/partApi'

export default {
  name: 'Home',
  data() {
    return {
      loading: false,
      stats: {
        totalParts: 0,
        needRestock: 0,
        normalStock: 0,
        totalQuantity: 0
      },
      restockParts: []
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const [partsRes, restockRes] = await Promise.all([
          partApi.getAllParts(),
          partApi.getPartsNeedRestock()
        ])
        
        if (partsRes.success) {
          const parts = partsRes.data || []
          this.stats.totalParts = parts.length
          this.stats.totalQuantity = parts.reduce((sum, p) => sum + p.quantity, 0)
          
          const needRestock = parts.filter(p => p.quantity <= p.minStock)
          this.stats.needRestock = needRestock.length
          this.stats.normalStock = parts.length - needRestock.length
        }
        
        if (restockRes.success) {
          this.restockParts = (restockRes.data || []).slice(0, 5)
        }
      } catch (error) {
        this.$message.error('加载数据失败: ' + error.message)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 8px;
  color: #fff;
}

.stat-card.total-parts {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-card.need-restock {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-card.normal-stock {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-card.total-quantity {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-icon {
  font-size: 48px;
  margin-right: 20px;
  opacity: 0.8;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
  margin-top: 5px;
}

.panel {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.panel-header {
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.danger-text {
  color: #f56c6c;
  font-weight: bold;
}

.quick-actions {
  padding: 20px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.action-card {
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.action-card:hover {
  transform: translateY(-2px);
}

.action-icon {
  font-size: 36px;
  color: #409EFF;
  margin-bottom: 10px;
}

.action-text {
  font-size: 14px;
  color: #606266;
}
</style>
