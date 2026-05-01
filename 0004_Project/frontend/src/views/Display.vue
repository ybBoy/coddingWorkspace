<template>
  <div class="display-container">
    <div class="display-header">
      <div class="park-name">
        <el-icon :size="40"><MapLocation /></el-icon>
        <span>美丽风景景区</span>
      </div>
      <div class="current-time">
        {{ currentTime }}
      </div>
    </div>

    <div class="display-main">
      <div class="left-section">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
            <el-icon :size="50"><User /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ overview.totalVisitors || 0 }}</div>
            <div class="stat-label">当前在园游客</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);">
            <el-icon :size="50"><TrendCharts /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ (overview.overallRatio * 100).toFixed(1) }}%</div>
            <div class="stat-label">景区拥挤率</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
            <el-icon :size="50"><Warning /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ overview.busyAreas || 0 }}</div>
            <div class="stat-label">拥挤区域</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
            <el-icon :size="50"><CircleCheck /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ overview.emptyAreas || 0 }}</div>
            <div class="stat-label">舒适区域</div>
          </div>
        </div>

        <div class="map-section">
          <div class="section-title">
            <el-icon><MapLocation /></el-icon> 区域分布地图
          </div>
          <div class="map-container">
            <div 
              v-for="area in areas" 
              :key="area.id"
              class="map-area"
              :style="getAreaStyle(area)"
            >
              <div class="area-name">{{ area.name }}</div>
              <div class="area-count">{{ area.currentVisitors }}人</div>
              <div class="area-level" :style="{ background: getCrowdColor(area.crowdLevel) }">
                {{ getCrowdDescription(area.crowdLevel) }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="center-section">
        <div v-if="activeGuidance.length > 0" class="guidance-banner">
          <div class="banner-title">
            <el-icon :size="30"><Promotion /></el-icon>
            <span>游客引导信息</span>
          </div>
          <div class="banner-content marquee-container">
            <div class="marquee-content" :ref="el => marqueeRef = el">
              <span v-for="(msg, index) in activeGuidance" :key="msg.id" class="guidance-item">
                <el-icon><Bell /></el-icon>
                {{ msg.content }}
              </span>
            </div>
          </div>
        </div>

        <div class="chart-section">
          <div class="section-title">
            <el-icon><BarChart /></el-icon> 各区域游客分布
          </div>
          <div ref="barChart" class="chart-container"></div>
        </div>

        <div class="chart-section" style="margin-top: 20px;">
          <div class="section-title">
            <el-icon><PieChart /></el-icon> 拥挤程度分布
          </div>
          <div ref="pieChart" class="chart-container"></div>
        </div>
      </div>

      <div class="right-section">
        <div class="hotspots-section">
          <div class="section-title">
            <el-icon><Fire /></el-icon> 热门区域
          </div>
          <div class="hotspots-list">
            <div 
              v-for="(area, index) in hotspots" 
              :key="area.id"
              class="hotspot-item"
              :class="{ 'top-3': index < 3 }"
            >
              <div class="hotspot-rank" :style="{ background: getRankColor(index) }">
                {{ index + 1 }}
              </div>
              <div class="hotspot-info">
                <div class="hotspot-name">{{ area.name }}</div>
                <div class="hotspot-detail">
                  {{ area.currentVisitors }}人 / 容量{{ area.maxCapacity }}
                </div>
              </div>
              <div class="hotspot-progress">
                <el-progress 
                  :percentage="Math.round((area.currentVisitors / area.maxCapacity) * 100)"
                  :color="getCrowdColor(area.crowdLevel)"
                  :stroke-width="8"
                  :show-text="false"
                />
              </div>
            </div>
          </div>
        </div>

        <div class="suggestions-section">
          <div class="section-title">
            <el-icon><ChatDotRound /></el-icon> 游览建议
          </div>
          <div v-if="suggestions.length > 0" class="suggestions-list">
            <div 
              v-for="suggestion in suggestions" 
              :key="suggestion.id"
              class="suggestion-item"
            >
              <div class="suggestion-icon" :style="{ background: getSuggestionColor(suggestion.priority) }">
                <el-icon v-if="suggestion.priority === 1"><WarningFilled /></el-icon>
                <el-icon v-else><InfoFilled /></el-icon>
              </div>
              <div class="suggestion-content">
                <div class="suggestion-warning">{{ suggestion.message }}</div>
                <div class="suggestion-text">{{ suggestion.suggestion }}</div>
              </div>
            </div>
          </div>
          <div v-else class="no-suggestion">
            <el-icon :size="60" color="#4CAF50"><CircleCheck /></el-icon>
            <div>景区运行正常</div>
            <div style="font-size: 14px; color: #95a5a6;">暂无拥挤预警</div>
          </div>
        </div>

        <div class="recommendations-section">
          <div class="section-title">
            <el-icon><Star /></el-icon> 推荐游览区域
          </div>
          <div class="recommendations-list">
            <div 
              v-for="area in recommendedAreas" 
              :key="area.id"
              class="recommendation-item"
            >
              <div class="rec-icon">
                <el-icon><Sunny /></el-icon>
              </div>
              <div class="rec-info">
                <div class="rec-name">{{ area.name }}</div>
                <div class="rec-detail">
                  仅{{ area.currentVisitors }}人，拥挤率{{ (area.currentVisitors / area.maxCapacity * 100).toFixed(0) }}%
                </div>
              </div>
              <el-tag type="success" effect="dark">推荐</el-tag>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'
import webSocket from '../utils/websocket'

const areas = ref([])
const overview = ref({})
const suggestions = ref([])
const activeGuidance = ref([])
const currentTime = ref('')
const barChart = ref(null)
const pieChart = ref(null)
const marqueeRef = ref(null)
let barChartInstance = null
let pieChartInstance = null
let timeInterval = null

const crowdLevelMap = {
  EMPTY: { description: '空闲', color: '#4CAF50' },
  COMFORTABLE: { description: '舒适', color: '#8BC34A' },
  MODERATE: { description: '适中', color: '#FFC107' },
  BUSY: { description: '拥挤', color: '#FF9800' },
  OVERCROWDED: { description: '超负荷', color: '#F44336' }
}

const hotspots = computed(() => {
  return [...areas.value]
    .sort((a, b) => b.currentVisitors - a.currentVisitors)
    .slice(0, 5)
})

const recommendedAreas = computed(() => {
  return [...areas.value]
    .filter(a => {
      const ratio = a.currentVisitors / a.maxCapacity
      return ratio < 0.5
    })
    .sort((a, b) => (a.currentVisitors / a.maxCapacity) - (b.currentVisitors / b.maxCapacity))
    .slice(0, 3)
})

const getCrowdColor = (level) => {
  if (!level) return '#95a5a6'
  return crowdLevelMap[level]?.color || '#95a5a6'
}

const getCrowdDescription = (level) => {
  if (!level) return '未知'
  return crowdLevelMap[level]?.description || '未知'
}

const getRankColor = (index) => {
  if (index === 0) return '#FFD700'
  if (index === 1) return '#C0C0C0'
  if (index === 2) return '#CD7F32'
  return '#95a5a6'
}

const getSuggestionColor = (priority) => {
  if (priority === 1) return '#F44336'
  if (priority === 2) return '#FF9800'
  return '#2196F3'
}

const getAreaStyle = (area) => {
  const positions = [
    { left: '5%', top: '5%' },
    { left: '30%', top: '10%' },
    { left: '55%', top: '5%' },
    { left: '20%', top: '45%' },
    { left: '50%', top: '50%' },
    { left: '75%', top: '40%' }
  ]
  const idx = (area.id - 1) % positions.length
  return {
    left: positions[idx].left,
    top: positions[idx].top,
    borderColor: getCrowdColor(area.crowdLevel),
    background: `${getCrowdColor(area.crowdLevel)}20`
  }
}

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    weekday: 'long'
  })
}

const initCharts = () => {
  if (barChart.value && !barChartInstance) {
    barChartInstance = echarts.init(barChart.value)
  }
  if (pieChart.value && !pieChartInstance) {
    pieChartInstance = echarts.init(pieChart.value)
  }
  updateCharts()
}

const updateCharts = () => {
  if (!barChartInstance || !pieChartInstance) return
  if (areas.value.length === 0) return

  const sortedAreas = [...areas.value].sort((a, b) => b.currentVisitors - a.currentVisitors)
  
  const barOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: sortedAreas.map(a => a.name),
      axisLabel: {
        color: '#ecf0f1',
        rotate: 30
      },
      axisLine: {
        lineStyle: {
          color: '#34495e'
        }
      }
    },
    yAxis: {
      type: 'value',
      name: '人数',
      nameTextStyle: {
        color: '#ecf0f1'
      },
      axisLabel: {
        color: '#ecf0f1'
      },
      splitLine: {
        lineStyle: {
          color: '#34495e'
        }
      }
    },
    series: [
      {
        type: 'bar',
        barWidth: '50%',
        data: sortedAreas.map(area => ({
          value: area.currentVisitors,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: getCrowdColor(area.crowdLevel) },
              { offset: 1, color: getCrowdColor(area.crowdLevel) + '80' }
            ]),
            borderRadius: [4, 4, 0, 0]
          }
        }))
      }
    ]
  }

  const crowdCounts = {
    空闲: 0,
    舒适: 0,
    适中: 0,
    拥挤: 0,
    超负荷: 0
  }

  areas.value.forEach(area => {
    const desc = getCrowdDescription(area.crowdLevel)
    if (crowdCounts.hasOwnProperty(desc)) {
      crowdCounts[desc]++
    }
  })

  const pieData = Object.entries(crowdCounts)
    .filter(([_, count]) => count > 0)
    .map(([name, count]) => ({ name, value: count }))

  const pieOption = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}个区域 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: {
        color: '#ecf0f1'
      }
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['35%', '50%'],
        data: pieData,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        },
        itemStyle: {
          borderRadius: 5,
          borderColor: '#1a1a2e',
          borderWidth: 2
        },
        label: {
          color: '#ecf0f1'
        }
      }
    ],
    color: ['#4CAF50', '#8BC34A', '#FFC107', '#FF9800', '#F44336']
  }

  barChartInstance.setOption(barOption, true)
  pieChartInstance.setOption(pieOption, true)
}

const fetchInitialData = async () => {
  try {
    const [areasRes, overviewRes, suggestionsRes, guidanceRes] = await Promise.all([
      axios.get('/api/areas'),
      axios.get('/api/overview'),
      axios.get('/api/suggestions'),
      axios.get('/api/guidance')
    ])
    areas.value = areasRes.data
    overview.value = overviewRes.data
    suggestions.value = suggestionsRes.data
    activeGuidance.value = guidanceRes.data
  } catch (e) {
    console.error('获取初始数据失败', e)
  }
}

const handleWebSocketData = (data) => {
  if (data.areas) {
    areas.value = data.areas
  }
  if (data.suggestions) {
    suggestions.value = data.suggestions
  }
  if (data.guidanceMessages) {
    activeGuidance.value = data.guidanceMessages
  }
  if (data.statistics) {
    const stats = data.statistics
    overview.value.totalVisitors = stats.totalVisitors
    overview.value.totalCapacity = stats.maxCapacity
    overview.value.overallRatio = stats.overallCrowdRatio
    overview.value.busyAreas = stats.busyAreasCount
    overview.value.emptyAreas = stats.emptyAreasCount
  }
  nextTick(() => {
    updateCharts()
  })
}

const handleResize = () => {
  if (barChartInstance) barChartInstance.resize()
  if (pieChartInstance) pieChartInstance.resize()
}

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)
  
  fetchInitialData()
  nextTick(() => {
    initCharts()
  })
  
  webSocket.connect().then(() => {
    webSocket.subscribe('/topic/visitor-data', handleWebSocketData)
  })
  
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (timeInterval) clearInterval(timeInterval)
  if (barChartInstance) barChartInstance.dispose()
  if (pieChartInstance) pieChartInstance.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.display-container {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  min-height: 100vh;
  padding: 20px;
  color: #ecf0f1;
}

.display-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 30px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  margin-bottom: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.park-name {
  display: flex;
  align-items: center;
  gap: 15px;
  font-size: 28px;
  font-weight: bold;
  background: linear-gradient(90deg, #4facfe, #00f2fe);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.current-time {
  font-size: 18px;
  color: #bdc3c7;
}

.display-main {
  display: grid;
  grid-template-columns: 280px 1fr 320px;
  gap: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 15px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  padding: 15px;
  margin-bottom: 15px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.stat-icon {
  width: 70px;
  height: 70px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #fff;
}

.stat-label {
  font-size: 12px;
  color: #95a5a6;
  margin-top: 5px;
}

.map-section {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  padding: 15px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 15px;
  color: #4facfe;
  display: flex;
  align-items: center;
  gap: 8px;
}

.map-container {
  position: relative;
  height: 280px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 8px;
}

.map-area {
  position: absolute;
  width: 110px;
  padding: 10px;
  border-radius: 8px;
  border: 2px solid;
  text-align: center;
  transition: all 0.3s ease;
}

.map-area:hover {
  transform: scale(1.05);
}

.area-name {
  font-weight: bold;
  font-size: 12px;
  margin-bottom: 3px;
}

.area-count {
  font-size: 14px;
  font-weight: bold;
}

.area-level {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 10px;
  color: white;
  display: inline-block;
  margin-top: 3px;
}

.guidance-banner {
  background: linear-gradient(90deg, #e74c3c, #c0392b);
  border-radius: 10px;
  padding: 20px;
  margin-bottom: 20px;
}

.banner-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 15px;
}

.banner-content {
  background: rgba(0, 0, 0, 0.3);
  border-radius: 8px;
  padding: 10px;
  overflow: hidden;
}

.marquee-container {
  overflow: hidden;
}

.guidance-item {
  display: inline-block;
  padding: 0 30px;
  font-size: 16px;
  white-space: nowrap;
}

.chart-section {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  padding: 15px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.chart-container {
  width: 100%;
  height: 220px;
}

.hotspots-section,
.suggestions-section,
.recommendations-section {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  padding: 15px;
  margin-bottom: 15px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.hotspots-list {
  max-height: 300px;
  overflow-y: auto;
}

.hotspot-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
  margin-bottom: 8px;
  transition: all 0.3s ease;
}

.hotspot-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

.hotspot-item.top-3 {
  background: rgba(255, 215, 0, 0.1);
  border: 1px solid rgba(255, 215, 0, 0.3);
}

.hotspot-rank {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  color: white;
  font-size: 14px;
}

.hotspot-info {
  flex: 1;
}

.hotspot-name {
  font-weight: bold;
  font-size: 14px;
}

.hotspot-detail {
  font-size: 11px;
  color: #95a5a6;
  margin-top: 3px;
}

.hotspot-progress {
  width: 60px;
}

.suggestion-item {
  display: flex;
  gap: 10px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
  margin-bottom: 8px;
  border-left: 3px solid #e74c3c;
}

.suggestion-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.suggestion-content {
  flex: 1;
}

.suggestion-warning {
  color: #e74c3c;
  font-weight: bold;
  font-size: 13px;
  margin-bottom: 5px;
}

.suggestion-text {
  color: #2ecc71;
  font-size: 12px;
}

.no-suggestion {
  text-align: center;
  padding: 30px;
  color: #4CAF50;
}

.recommendation-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: rgba(76, 175, 80, 0.1);
  border-radius: 8px;
  margin-bottom: 8px;
  border: 1px solid rgba(76, 175, 80, 0.3);
}

.rec-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4CAF50, #8BC34A);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
}

.rec-info {
  flex: 1;
}

.rec-name {
  font-weight: bold;
  font-size: 14px;
}

.rec-detail {
  font-size: 11px;
  color: #95a5a6;
  margin-top: 3px;
}

::-webkit-scrollbar {
  width: 4px;
}

::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 2px;
}

::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
}
</style>