<template>
  <div id="app">
    <el-container>
      <el-header class="header">
        <h1>股票数据分析系统</h1>
      </el-header>
      <el-main class="main">
        <el-row :gutter="20">
          <el-col :span="24">
            <FileUpload @file-uploaded="handleFileUploaded" />
          </el-col>
        </el-row>
        
        <el-divider />
        
        <el-row :gutter="20" v-if="fileUploaded">
          <el-col :span="24">
            <ParamsConfig 
              :defaultParams="defaultParams"
              @params-change="handleParamsChange"
              @analyze="handleAnalyze"
            />
          </el-col>
        </el-row>
        
        <el-divider v-if="showResults" />
        
        <el-row :gutter="20" v-if="showResults">
          <el-col :span="24">
            <ResultsDisplay 
              :analysisResult="analysisResult"
            />
          </el-col>
        </el-row>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import FileUpload from './components/FileUpload.vue'
import ParamsConfig from './components/ParamsConfig.vue'
import ResultsDisplay from './components/ResultsDisplay.vue'

export default {
  name: 'App',
  components: {
    FileUpload,
    ParamsConfig,
    ResultsDisplay
  },
  data() {
    return {
      fileUploaded: false,
      showResults: false,
      defaultParams: {
        pricePercentile: 30,
        timePercentile: 20,
        sellPriceMultiple: 3.0
      },
      currentParams: {
        pricePercentile: 30,
        timePercentile: 20,
        sellPriceMultiple: 3.0
      },
      analysisResult: null
    }
  },
  methods: {
    handleFileUploaded() {
      this.fileUploaded = true
      this.showResults = false
      this.analysisResult = null
    },
    handleParamsChange(params) {
      this.currentParams = { ...params }
    },
    handleAnalyze() {
      this.$axios.post('/analyze', this.currentParams)
        .then(response => {
          if (response.data.success) {
            this.analysisResult = response.data.result
            this.showResults = true
            this.$message.success('分析完成')
          } else {
            this.$message.error(response.data.message || '分析失败')
          }
        })
        .catch(error => {
          console.error('分析错误:', error)
          this.$message.error('分析请求失败，请检查网络连接')
        })
    }
  }
}
</script>

<style>
#app {
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  min-height: 100vh;
  background-color: #f5f7fa;
}

.header {
  background-color: #409EFF;
  color: white;
  display: flex;
  align-items: center;
  padding: 0 20px;
}

.header h1 {
  margin: 0;
  font-size: 24px;
}

.main {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}
</style>
