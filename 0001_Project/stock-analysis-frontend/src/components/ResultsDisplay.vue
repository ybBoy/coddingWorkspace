<template>
  <div class="results-display">
    <el-card v-if="analysisResult">
      <div slot="header" class="card-header">
        <span>分析结果</span>
      </div>
      
      <el-alert
        :title="`当前使用参数：价格前${analysisResult.params.pricePercentile}%、时间前${analysisResult.params.timePercentile}%、出货价格${analysisResult.params.sellPriceMultiple}倍`"
        type="info"
        :closable="false"
        style="margin-bottom: 20px;"
      />
      
      <el-tabs v-model="activeTab">
        <el-tab-pane label="建仓明细" name="position">
          <el-table 
            :data="analysisResult.positionResults" 
            border 
            stripe
            style="width: 100%;"
            v-loading="false"
          >
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="trader" label="建仓人员" min-width="100" />
            <el-table-column prop="positionTime" label="建仓时间" min-width="160">
              <template slot-scope="scope">
                {{ formatDateTime(scope.row.positionTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="positionPrice" label="建仓成本" min-width="100">
              <template slot-scope="scope">
                <span style="color: #f56c6c;">{{ scope.row.positionPrice.toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="positionQuantity" label="建仓数量" min-width="100">
              <template slot-scope="scope">
                {{ formatNumber(scope.row.positionQuantity) }}
              </template>
            </el-table-column>
            <el-table-column prop="buyAccount" label="买入账户" min-width="100" />
          </el-table>
          
          <div class="summary">
            <el-tag type="primary" size="large">
              建仓记录：{{ analysisResult.positionResults.length }} 条
            </el-tag>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="出货明细" name="sell">
          <el-table 
            :data="analysisResult.sellResults" 
            border 
            stripe
            style="width: 100%;"
            v-loading="false"
          >
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="seller" label="出货人员" min-width="100" />
            <el-table-column prop="sellTime" label="出货时间" min-width="160">
              <template slot-scope="scope">
                {{ formatDateTime(scope.row.sellTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="sellPrice" label="出货价格" min-width="100">
              <template slot-scope="scope">
                <span style="color: #67c23a;">{{ scope.row.sellPrice.toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="positionPrice" label="建仓价格" min-width="100">
              <template slot-scope="scope">
                {{ scope.row.positionPrice.toFixed(2) }}
              </template>
            </el-table-column>
            <el-table-column prop="sellQuantity" label="出货数量" min-width="100">
              <template slot-scope="scope">
                {{ formatNumber(scope.row.sellQuantity) }}
              </template>
            </el-table-column>
            <el-table-column prop="profit" label="获利金额" min-width="120">
              <template slot-scope="scope">
                <span :style="getProfitColor(scope.row.profit)">
                  {{ scope.row.profit >= 0 ? '+' : '' }}{{ formatNumber(scope.row.profit) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="profitRate" label="利润率" min-width="100">
              <template slot-scope="scope">
                <span :style="getProfitColor(scope.row.profitRate)">
                  {{ scope.row.profitRate >= 0 ? '+' : '' }}{{ scope.row.profitRate.toFixed(2) }}%
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="sellAccount" label="卖出账户" min-width="100" />
          </el-table>
          
          <div class="summary">
            <el-tag type="success" size="large">
              出货记录：{{ analysisResult.sellResults.length }} 条
            </el-tag>
            <el-tag type="warning" size="large" style="margin-left: 15px;">
              总获利：{{ calculateTotalProfit() }}
            </el-tag>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
    
    <el-card v-else>
      <el-empty description="暂无分析结果，请先上传文件并进行分析" />
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'ResultsDisplay',
  props: {
    analysisResult: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      activeTab: 'position'
    }
  },
  methods: {
    formatDateTime(dateTime) {
      if (!dateTime) return '-'
      const date = new Date(dateTime)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      const seconds = String(date.getSeconds()).padStart(2, '0')
      return `${year}${month}${day} ${hours}:${minutes}:${seconds}`
    },
    formatNumber(num) {
      if (num === null || num === undefined) return '-'
      if (typeof num === 'number') {
        return num.toLocaleString('zh-CN', { 
          minimumFractionDigits: num % 1 === 0 ? 0 : 2,
          maximumFractionDigits: 2
        })
      }
      return num
    },
    getProfitColor(profit) {
      if (profit > 0) {
        return 'color: #67c23a; font-weight: bold;'
      } else if (profit < 0) {
        return 'color: #f56c6c; font-weight: bold;'
      }
      return 'color: #909399;'
    },
    calculateTotalProfit() {
      if (!this.analysisResult || !this.analysisResult.sellResults) {
        return '0'
      }
      const total = this.analysisResult.sellResults.reduce((sum, item) => {
        return sum + (item.profit || 0)
      }, 0)
      return this.formatNumber(total)
    }
  }
}
</script>

<style scoped>
.card-header {
  font-size: 18px;
  font-weight: bold;
}

.summary {
  margin-top: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

::v-deep .el-tabs__header {
  margin-bottom: 20px;
}

::v-deep .el-table th {
  background-color: #f5f7fa !important;
  font-weight: bold;
}
</style>
