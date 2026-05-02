<template>
  <div class="page-container">
    <div class="page-title">
      <i class="el-icon-warning" style="margin-right: 10px;"></i>需补货列表
    </div>
    
    <div class="alert-section" v-if="parts.length > 0">
      <el-alert
        title="库存预警"
        :type="parts.length > 5 ? 'error' : 'warning'"
        :closable="false"
        show-icon
      >
        <template slot="default">
          当前有 <strong style="color: #f56c6c; font-size: 18px;">{{ parts.length }}</strong> 种零件库存低于警戒线，请及时补货！
        </template>
      </el-alert>
    </div>

    <div class="toolbar">
      <el-button type="primary" icon="el-icon-refresh" @click="loadParts" :loading="loading">刷新</el-button>
      <el-button type="success" icon="el-icon-circle-plus" @click="handleBatchStockIn" :disabled="selectedParts.length === 0">
        批量入库 ({{ selectedParts.length }})
      </el-button>
    </div>

    <el-table
      :data="parts"
      v-loading="loading"
      style="width: 100%"
      class="table-container"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55"></el-table-column>
      <el-table-column prop="id" label="零件编号" width="120">
        <template slot-scope="scope">
          <el-tag type="danger">{{ scope.row.id }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="零件名称" width="150">
        <template slot-scope="scope">
          <span class="danger-text">{{ scope.row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="category" label="分类" width="100"></el-table-column>
      <el-table-column prop="specification" label="规格"></el-table-column>
      <el-table-column label="库存状况" width="200">
        <template slot-scope="scope">
          <div class="stock-status">
            <div class="stock-bar">
              <div class="stock-bar-current" :style="{ width: getStockPercentage(scope.row) + '%' }"></div>
              <div class="stock-bar-warning" :style="{ left: (scope.row.minStock / (scope.row.minStock * 3 || 100)) * 100 + '%' }"></div>
            </div>
            <div class="stock-text">
              当前: <span class="danger-text">{{ scope.row.quantity }}</span>
              / 警戒: <span class="warning-text">{{ scope.row.minStock }}</span>
              {{ scope.row.unit }}
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="补货建议" width="120">
        <template slot-scope="scope">
          <span class="suggestion">
            建议补货 <strong>{{ getSuggestedQuantity(scope.row) }}</strong> {{ scope.row.unit }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="handleStockIn(scope.row)">
            <i class="el-icon-circle-plus"></i> 立即入库
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && parts.length === 0" description="太棒了！所有零件库存正常">
      <el-button type="primary" icon="el-icon-s-grid" @click="$router.push('/parts')">查看全部库存</el-button>
    </el-empty>

    <el-dialog title="入库操作" :visible.sync="stockInDialogVisible" width="500px">
      <el-table :data="selectedParts" style="width: 100%; margin-bottom: 20px;" size="small">
        <el-table-column prop="id" label="编号" width="100"></el-table-column>
        <el-table-column prop="name" label="名称" width="150"></el-table-column>
        <el-table-column prop="specification" label="规格"></el-table-column>
        <el-table-column prop="quantity" label="当前库存" width="100">
          <template slot-scope="scope">
            <span class="danger-text">{{ scope.row.quantity }} {{ scope.row.unit }}</span>
          </template>
        </el-table-column>
      </el-table>
      
      <el-form label-width="100px">
        <el-form-item label="入库数量">
          <el-input-number v-model="stockInQuantity" :min="1" style="width: 200px"></el-input-number>
          <span style="margin-left: 10px;">单位</span>
        </el-form-item>
      </el-form>
      
      <span slot="footer" class="dialog-footer">
        <el-button @click="stockInDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBatchStockIn">确认入库</el-button>
      </span>
    </el-dialog>

    <el-dialog title="单零件入库" :visible.sync="singleStockInDialogVisible" width="400px">
      <div style="margin-bottom: 15px;">
        <p><strong>零件：</strong>{{ currentPart.name }} ({{ currentPart.id }})</p>
        <p><strong>当前库存：</strong><span class="danger-text">{{ currentPart.quantity }} {{ currentPart.unit }}</span></p>
        <p><strong>警戒线：</strong>{{ currentPart.minStock }} {{ currentPart.unit }}</p>
        <p><strong>建议补货：</strong><span class="suggestion-highlight">{{ getSuggestedQuantity(currentPart) }} {{ currentPart.unit }}</span></p>
      </div>
      <el-form label-width="100px">
        <el-form-item label="入库数量">
          <el-input-number v-model="singleStockInQuantity" :min="1" style="width: 200px"></el-input-number>
          <span style="margin-left: 10px;">{{ currentPart.unit }}</span>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="singleStockInDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSingleStockIn">确认入库</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import partApi from '../api/partApi'
import { eventBus } from '../utils/eventBus'

export default {
  name: 'RestockList',
  data() {
    return {
      loading: false,
      parts: [],
      selectedParts: [],
      stockInDialogVisible: false,
      singleStockInDialogVisible: false,
      stockInQuantity: 10,
      singleStockInQuantity: 10,
      currentPart: {}
    }
  },
  created() {
    this.loadParts()
  },
  methods: {
    async loadParts() {
      this.loading = true
      try {
        const res = await partApi.getPartsNeedRestock()
        if (res.success) {
          this.parts = res.data || []
        } else {
          this.$message.error(res.message)
        }
      } catch (error) {
        this.$message.error('加载需补货列表失败: ' + error.message)
      } finally {
        this.loading = false
      }
    },

    getStockPercentage(row) {
      const maxValue = Math.max(row.minStock * 3, row.quantity, 10)
      return Math.min((row.quantity / maxValue) * 100, 100)
    },

    getSuggestedQuantity(row) {
      const suggested = row.minStock * 2 - row.quantity
      return suggested > 0 ? suggested : row.minStock
    },

    handleSelectionChange(selection) {
      this.selectedParts = selection
    },

    handleBatchStockIn() {
      if (this.selectedParts.length === 0) {
        this.$message.warning('请先选择要入库的零件')
        return
      }
      this.stockInQuantity = 10
      this.stockInDialogVisible = true
    },

    async confirmBatchStockIn() {
      this.loading = true
      let successCount = 0
      let failCount = 0

      for (const part of this.selectedParts) {
        try {
          const res = await partApi.stockIn(part.id, this.stockInQuantity)
          if (res.success) {
            successCount++
          } else {
            failCount++
          }
        } catch (error) {
          failCount++
        }
      }

      if (successCount > 0) {
        this.$message.success('批量入库成功：' + successCount + ' 种零件')
        eventBus.$emit('stock-changed')
      }
      if (failCount > 0) {
        this.$message.error('批量入库失败：' + failCount + ' 种零件')
      }

      this.stockInDialogVisible = false
      this.selectedParts = []
      this.loadParts()
    },

    handleStockIn(row) {
      this.currentPart = { ...row }
      this.singleStockInQuantity = this.getSuggestedQuantity(row)
      this.singleStockInDialogVisible = true
    },

    async confirmSingleStockIn() {
      try {
        const res = await partApi.stockIn(this.currentPart.id, this.singleStockInQuantity)
        if (res.success) {
          this.$message.success('入库成功，新增 ' + this.singleStockInQuantity + ' ' + this.currentPart.unit)
          eventBus.$emit('stock-changed')
          this.singleStockInDialogVisible = false
          this.loadParts()
        } else {
          this.$message.error(res.message)
        }
      } catch (error) {
        this.$message.error('入库失败: ' + error.message)
      }
    }
  }
}
</script>

<style scoped>
.alert-section {
  margin-bottom: 20px;
}

.toolbar {
  margin-bottom: 20px;
}

.danger-text {
  color: #f56c6c;
  font-weight: bold;
}

.warning-text {
  color: #e6a23c;
  font-weight: bold;
}

.stock-status {
  padding: 5px 0;
}

.stock-bar {
  position: relative;
  height: 10px;
  background-color: #ebeef5;
  border-radius: 5px;
  overflow: visible;
  margin-bottom: 8px;
}

.stock-bar-current {
  height: 100%;
  background: linear-gradient(90deg, #f56c6c, #e6a23c);
  border-radius: 5px;
  transition: width 0.3s;
}

.stock-bar-warning {
  position: absolute;
  top: -5px;
  width: 2px;
  height: 20px;
  background-color: #e6a23c;
}

.stock-text {
  font-size: 12px;
  color: #909399;
}

.suggestion {
  font-size: 12px;
  color: #606266;
}

.suggestion-highlight {
  color: #409EFF;
  font-size: 16px;
}

.dialog-footer {
  text-align: right;
}
</style>
