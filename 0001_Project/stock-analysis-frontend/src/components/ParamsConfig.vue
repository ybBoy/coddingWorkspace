<template>
  <div class="params-config">
    <el-card>
      <div slot="header" class="card-header">
        <span>参数配置</span>
        <el-button type="text" @click="resetParams" style="float: right;">
          恢复默认
        </el-button>
      </div>
      
      <el-form :model="params" label-width="200px" style="max-width: 800px;">
        <el-divider content-position="left">建仓指标参数</el-divider>
        
        <el-form-item label="价格区间前百分比 (%)">
          <el-input-number 
            v-model="params.pricePercentile" 
            :min="1" 
            :max="100"
            :step="1"
            @change="handleParamsChange"
          />
          <span class="input-tip">默认30%，表示筛选价格排在前面30%的交易</span>
        </el-form-item>
        
        <el-form-item label="时间区间前百分比 (%)">
          <el-input-number 
            v-model="params.timePercentile" 
            :min="1" 
            :max="100"
            :step="1"
            @change="handleParamsChange"
          />
          <span class="input-tip">默认20%，表示筛选时间排在前面20%的交易</span>
        </el-form-item>
        
        <el-divider content-position="left">出货指标参数</el-divider>
        
        <el-form-item label="出货价格倍数">
          <el-input-number 
            v-model="params.sellPriceMultiple" 
            :min="1.0" 
            :max="100.0"
            :step="0.5"
            :precision="1"
            @change="handleParamsChange"
          />
          <span class="input-tip">默认3倍，表示出货价格需要是建仓价格的3倍以上</span>
        </el-form-item>
      </el-form>
      
      <div style="margin-top: 20px;">
        <el-button type="primary" size="large" @click="handleAnalyze">
          开始分析
        </el-button>
        <span style="margin-left: 10px; color: #909399;">
          调整参数后点击此按钮重新分析
        </span>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'ParamsConfig',
  props: {
    defaultParams: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      params: {
        pricePercentile: this.defaultParams.pricePercentile || 30,
        timePercentile: this.defaultParams.timePercentile || 20,
        sellPriceMultiple: this.defaultParams.sellPriceMultiple || 3.0
      }
    }
  },
  watch: {
    defaultParams: {
      handler(newVal) {
        this.params = { ...newVal }
      },
      immediate: true
    }
  },
  methods: {
    handleParamsChange() {
      this.$emit('params-change', { ...this.params })
    },
    resetParams() {
      this.params = {
        pricePercentile: this.defaultParams.pricePercentile || 30,
        timePercentile: this.defaultParams.timePercentile || 20,
        sellPriceMultiple: this.defaultParams.sellPriceMultiple || 3.0
      }
      this.$emit('params-change', { ...this.params })
      this.$message.info('已恢复默认参数')
    },
    handleAnalyze() {
      this.$emit('params-change', { ...this.params })
      this.$emit('analyze')
    }
  }
}
</script>

<style scoped>
.card-header {
  font-size: 18px;
  font-weight: bold;
}

.input-tip {
  margin-left: 15px;
  color: #909399;
  font-size: 13px;
}

.el-divider {
  margin: 20px 0;
}
</style>
