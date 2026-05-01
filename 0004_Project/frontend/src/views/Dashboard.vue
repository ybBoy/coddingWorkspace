<template>
  <div>
    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="6">
        <el-card shadow="hover" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white;">
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <div>
              <div style="font-size: 14px; opacity: 0.9; margin-bottom: 8px;">景区总游客</div>
              <div style="font-size: 32px; font-weight: bold;">{{ overview.totalVisitors || 0 }}</div>
              <div style="font-size: 12px; opacity: 0.7; margin-top: 5px;">
                容量: {{ overview.totalCapacity || 0 }}
              </div>
            </div>
            <el-icon :size="50" style="opacity: 0.8;"><User /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" style="background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); color: white;">
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <div>
              <div style="font-size: 14px; opacity: 0.9; margin-bottom: 8px;">整体拥挤率</div>
              <div style="font-size: 32px; font-weight: bold;">{{ (overview.overallRatio * 100).toFixed(1) }}%</div>
              <div style="font-size: 12px; opacity: 0.7; margin-top: 5px;">
                状态: {{ getOverallStatus() }}
              </div>
            </div>
            <el-icon :size="50" style="opacity: 0.8;"><TrendCharts /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: white;">
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <div>
              <div style="font-size: 14px; opacity: 0.9; margin-bottom: 8px;">拥挤区域</div>
              <div style="font-size: 32px; font-weight: bold;">{{ overview.busyAreas || 0 }}</div>
              <div style="font-size: 12px; opacity: 0.7; margin-top: 5px;">
                舒适区域: {{ overview.emptyAreas || 0 }}
              </div>
            </div>
            <el-icon :size="50" style="opacity: 0.8;"><Warning /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); color: white;">
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <div>
              <div style="font-size: 14px; opacity: 0.9; margin-bottom: 8px;">活动建议</div>
              <div style="font-size: 32px; font-weight: bold;">{{ overview.activeSuggestions || 0 }}</div>
              <div style="font-size: 12px; opacity: 0.7; margin-top: 5px;">
                引导信息: {{ overview.activeGuidance || 0 }}
              </div>
            </div>
            <el-icon :size="50" style="opacity: 0.8;"><ChatDotRound /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span style="font-weight: bold; font-size: 16px;">
                <el-icon><MapLocation /></el-icon> 景区区域实时状态
              </span>
              <div>
                <el-tag v-for="(level, key) in crowdLevelMap" :key="key" :style="{ background: level.color, color: 'white', marginRight: '5px' }" size="small">
                  {{ level.description }}
                </el-tag>
              </div>
            </div>
          </template>
          
          <el-row :gutter="20">
            <el-col :span="24" style="margin-bottom: 20px;">
              <div ref="areaChart" style="width: 100%; height: 300px;"></div>
            </el-col>
          </el-row>

          <el-table :data="areas" style="width: 100%" stripe>
            <el-table-column prop="name" label="区域名称" width="150">
              <template #default="{ row }">
                <el-tag :style="{ background: getCrowdColor(row.crowdLevel), color: 'white' }">
                  {{ row.name }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="currentVisitors" label="当前游客" width="100" align="center">
              <template #default="{ row }">
                <span :style="{ color: getCrowdColor(row.crowdLevel), fontWeight: 'bold' }">
                  {{ row.currentVisitors }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="maxCapacity" label="最大容量" width="100" align="center" />
            <el-table-column label="拥挤率" width="180">
              <template #default="{ row }">
                <el-progress 
                  :percentage="Math.round((row.currentVisitors / row.maxCapacity) * 100)"
                  :color="getCrowdColor(row.crowdLevel)"
                  :stroke-width="20"
                />
              </template>
            </el-table-column>
            <el-table-column prop="crowdLevel.description" label="拥挤程度" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getCrowdTagType(row.crowdLevel)" effect="dark">
                  {{ row.crowdLevel?.description || '未知' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" align="center">
              <template #default="{ row }">
                <el-button type="primary" link @click="showAreaDetail(row)">
                  <el-icon><View /></el-icon> 详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover" style="margin-bottom: 20px;">
          <template #header>
            <span style="font-weight: bold; font-size: 16px;">
              <el-icon><Warning /></el-icon> 拥挤预警建议
            </span>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="(suggestion, index) in suggestions"
              :key="suggestion.id"
              :type="getSuggestionType(suggestion.priority)"
              :icon="getSuggestionIcon(suggestion.priority)"
            >
              <el-card shadow="hover" :type="getSuggestionCardType(suggestion.priority)">
                <div style="font-weight: bold; margin-bottom: 8px; color: #e74c3c;">
                  <el-icon><WarningFilled /></el-icon> {{ suggestion.message }}
                </div>
                <div style="color: #16a085; font-size: 13px;">
                  <el-icon><Right /></el-icon> {{ suggestion.suggestion }}
                </div>
                <div style="font-size: 11px; color: #95a5a6; margin-top: 8px;">
                  {{ formatTime(suggestion.createTime) }}
                </div>
              </el-card>
            </el-timeline-item>
            <el-timeline-item v-if="suggestions.length === 0" type="success">
              <el-card>
                <div style="text-align: center; color: #27ae60; padding: 20px;">
                  <el-icon :size="40"><CircleCheck /></el-icon>
                  <div style="margin-top: 10px;">所有区域运行正常，无拥挤预警</div>
                </div>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <el-card shadow="hover">
          <template #header>
            <span style="font-weight: bold; font-size: 16px;">
              <el-icon><ChatDotRound /></el-icon> 正在发布的引导
            </span>
          </template>
          <el-empty v-if="activeGuidance.length === 0" description="暂无引导信息" />
          <div v-else>
            <el-alert
              v-for="msg in activeGuidance"
              :key="msg.id"
              :title="msg.title"
              :type="getGuidanceType(msg.type)"
              style="margin-bottom: 10px;"
              show-icon
              closable
            >
              <template #default>
                {{ msg.content }}
                <div style="margin-top: 5px; font-size: 12px; color: #7f8c8d;">
                  发布源: {{ msg.source === 'AUTO' ? '自动' : '手动' }} | 
                  目标: {{ msg.targetDisplay }}
                </div>
              </template>
            </el-alert>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="detailVisible" title="区域详情" width="60%">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="区域名称">{{ selectedArea?.name }}</el-descriptions-item>
        <el-descriptions-item label="当前游客">{{ selectedArea?.currentVisitors }}</el-descriptions-item>
        <el-descriptions-item label="最大容量">{{ selectedArea?.maxCapacity }}</el-descriptions-item>
        <el-descriptions-item label="拥挤程度">
          <el-tag :type="getCrowdTagType(selectedArea?.crowdLevel)" effect="dark">
            {{ selectedArea?.crowdLevel?.description }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="拥挤率" :span="2">
          <el-progress 
            :percentage="Math.round((selectedArea?.currentVisitors / selectedArea?.maxCapacity) * 100)"
            :color="getCrowdColor(selectedArea?.crowdLevel)"
          />
        </el-descriptions-item>
      </el-descriptions>

      <el-divider>区域内景点</el-divider>
      
      <el-table :data="areaSpots" style="width: 100%; margin-top: 15px;" stripe>
        <el-table-column prop="name" label="景点名称" width="150" />
        <el-table-column prop="currentVisitors" label="当前游客" align="center" />
        <el-table-column prop="maxCapacity" label="最大容量" align="center" />
        <el-table-column label="拥挤率" width="200">
          <template #default="{ row }">
            <el-progress 
              :percentage="Math.round((row.currentVisitors / row.maxCapacity) * 100)"
              :color="getCrowdColor(row.crowdLevel)"
              :stroke-width="15"
            />
          </template>
        </el-table-column>
        <el-table-column prop="crowdLevel.description" label="拥挤程度" align="center">
          <template #default="{ row }">
            <el-tag :type="getCrowdTagType(row.crowdLevel)" size="small" effect="dark">
              {{ row.crowdLevel?.description }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'
import webSocket from '../utils/websocket'

const areas = ref([])
const spots = ref([])
const suggestions = ref([])
const activeGuidance = ref([])
const overview = ref({})
const detailVisible = ref(false)
const selectedArea = ref(null)
const areaSpots = ref([])
const areaChart = ref(null)
let chartInstance = null

const crowdLevelMap = {
  EMPTY: { description: '空闲', color: '#4CAF50' },
  COMFORTABLE: { description: '舒适', color: '#8BC34A' },
  MODERATE: { description: '适中', color: '#FFC107' },
  BUSY: { description: '拥挤', color: '#FF9800' },
  OVERCROWDED: { description: '超负荷', color: '#F44336' }
}

const getCrowdColor = (level) => {
  if (!level) return '#95a5a6'
  return crowdLevelMap[level]?.color || '#95a5a6'
}

const getCrowdTagType = (level) => {
  if (!level) return 'info'
  const map = {
    EMPTY: 'success',
    COMFORTABLE: 'success',
    MODERATE: 'warning',
    BUSY: 'warning',
    OVERCROWDED: 'danger'
  }
  return map[level] || 'info'
}

const getOverallStatus = () => {
  const ratio = overview.value.overallRatio || 0
  if (ratio < 0.3) return '舒适'
  if (ratio < 0.5) return '良好'
  if (ratio < 0.7) return '适中'
  if (ratio < 0.9) return '拥挤'
  return '超负荷'
}

const getSuggestionType = (priority) => {
  if (priority === 1) return 'danger'
  if (priority === 2) return 'warning'
  return 'info'
}

const getSuggestionIcon = (priority) => {
  if (priority === 1) return 'WarningFilled'
  if (priority === 2) return 'Warning'
  return 'InfoFilled'
}

const getSuggestionCardType = (priority) => {
  if (priority === 1) return 'danger'
  if (priority === 2) return 'warning'
  return 'info'
}

const getGuidanceType = (type) => {
  const map = {
    CROWD_WARNING: 'warning',
    GUIDANCE_SUGGESTION: 'info',
    EMERGENCY: 'danger',
    INFO: 'success'
  }
  return map[type] || 'info'
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  return new Date(timeStr).toLocaleString('zh-CN')
}

const showAreaDetail = (area) => {
  selectedArea.value = area
  areaSpots.value = spots.value.filter(s => s.areaId === area.id)
  detailVisible.value = true
}

const initChart = () => {
  if (areaChart.value) {
    chartInstance = echarts.init(areaChart.value)
    updateChart()
  }
}

const updateChart = () => {
  if (!chartInstance) return

  const sortedAreas = [...areas.value].sort((a, b) => b.currentVisitors - a.currentVisitors)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['当前游客', '剩余容量'],
      top: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: sortedAreas.map(a => a.name),
      axisLabel: {
        rotate: 30
      }
    },
    yAxis: {
      type: 'value',
      name: '人数'
    },
    series: [
      {
        name: '当前游客',
        type: 'bar',
        stack: 'total',
        itemStyle: {
          color: function(params) {
            const area = sortedAreas[params.dataIndex]
            return getCrowdColor(area?.crowdLevel)
          }
        },
        data: sortedAreas.map(a => a.currentVisitors)
      },
      {
        name: '剩余容量',
        type: 'bar',
        stack: 'total',
        itemStyle: {
          color: '#ecf0f1'
        },
        data: sortedAreas.map(a => a.maxCapacity - a.currentVisitors)
      }
    ]
  }

  chartInstance.setOption(option, true)
}

const fetchInitialData = async () => {
  try {
    const [areasRes, spotsRes, suggestionsRes, guidanceRes, overviewRes] = await Promise.all([
      axios.get('/api/areas'),
      axios.get('/api/spots'),
      axios.get('/api/suggestions'),
      axios.get('/api/guidance'),
      axios.get('/api/overview')
    ])
    areas.value = areasRes.data
    spots.value = spotsRes.data
    suggestions.value = suggestionsRes.data
    activeGuidance.value = guidanceRes.data
    overview.value = overviewRes.data
    
    await nextTick()
    updateChart()
  } catch (e) {
    console.error('获取初始数据失败', e)
  }
}

const handleWebSocketData = (data) => {
  if (data.areas) {
    areas.value = data.areas
  }
  if (data.spots) {
    spots.value = data.spots
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
    overview.value.moderateAreas = stats.moderateAreasCount
  }
  
  nextTick(() => {
    updateChart()
  })
}

const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

onMounted(() => {
  fetchInitialData()
  nextTick(() => {
    initChart()
  })
  
  webSocket.connect().then(() => {
    webSocket.subscribe('/topic/visitor-data', handleWebSocketData)
  })
  
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
  }
  window.removeEventListener('resize', handleResize)
})
</script>