<template>
  <div>
    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="6">
        <el-card shadow="hover" style="text-align: center;">
          <div style="color: #667eea; font-size: 36px; font-weight: bold;">
            {{ overview.totalVisitors || 0 }}
          </div>
          <div style="color: #7f8c8d; margin-top: 5px;">当前总游客</div>
          <el-divider style="margin: 10px 0;" />
          <div style="font-size: 12px; color: #95a5a6;">
            <el-progress 
              :percentage="Math.round((overview.totalVisitors / (overview.totalCapacity || 1)) * 100)"
              :color="getOverallColor()"
              :stroke-width="8"
            />
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" style="text-align: center;">
          <div style="color: #11998e; font-size: 36px; font-weight: bold;">
            {{ overview.busyAreas || 0 }}
          </div>
          <div style="color: #7f8c8d; margin-top: 5px;">拥挤区域数</div>
          <el-divider style="margin: 10px 0;" />
          <div style="font-size: 12px; color: #95a5a6;">
            共 {{ totalAreas }} 个区域
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" style="text-align: center;">
          <div style="color: #f5576c; font-size: 36px; font-weight: bold;">
            {{ overview.emptyAreas || 0 }}
          </div>
          <div style="color: #7f8c8d; margin-top: 5px;">舒适区域数</div>
          <el-divider style="margin: 10px 0;" />
          <div style="font-size: 12px; color: #95a5a6;">
            建议优先引导游客前往
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" style="text-align: center;">
          <div style="color: #4facfe; font-size: 36px; font-weight: bold;">
            {{ safetyScore.toFixed(1) }}
          </div>
          <div style="color: #7f8c8d; margin-top: 5px;">安全指数</div>
          <el-divider style="margin: 10px 0;" />
          <div style="font-size: 12px;">
            <el-tag :type="getSafetyTagType()" effect="dark" size="small">
              {{ getSafetyLevel() }}
            </el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover" style="height: 100%;">
          <template #header>
            <span style="font-weight: bold; font-size: 16px;">
              <el-icon><PieChart /></el-icon> 区域游客分布
            </span>
          </template>
          <div ref="pieChart" style="width: 100%; height: 350px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" style="height: 100%;">
          <template #header>
            <span style="font-weight: bold; font-size: 16px;">
              <el-icon><TrendCharts /></el-icon> 区域拥挤率对比
            </span>
          </template>
          <div ref="barChart" style="width: 100%; height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <span style="font-weight: bold; font-size: 16px;">
              <el-icon><Warning /></el-icon> 安全风险评估
            </span>
          </template>
          
          <el-table :data="riskAssessment" style="width: 100%">
            <el-table-column prop="areaName" label="区域" width="150">
              <template #default="{ row }">
                <el-tag :type="getRiskTagType(row.riskLevel)" effect="dark">
                  {{ row.areaName }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="currentVisitors" label="当前游客" align="center" />
            <el-table-column prop="maxCapacity" label="最大容量" align="center" />
            <el-table-column prop="crowdRatio" label="拥挤率" width="180">
              <template #default="{ row }">
                <el-progress 
                  :percentage="Math.round(row.crowdRatio * 100)"
                  :color="getCrowdColor(row.crowdLevel)"
                  :stroke-width="15"
                />
              </template>
            </el-table-column>
            <el-table-column prop="riskLevel" label="风险等级" align="center">
              <template #default="{ row }">
                <el-tag :type="getRiskTagType(row.riskLevel)" effect="dark">
                  {{ getRiskLevelName(row.riskLevel) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="suggestion" label="建议措施" min-width="200">
              <template #default="{ row }">
                <el-text :type="getRiskTextType(row.riskLevel)" size="small">
                  {{ row.suggestion }}
                </el-text>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span style="font-weight: bold; font-size: 16px;">
              <el-icon><Promotion /></el-icon> 游客体验指标
            </span>
          </template>
          
          <div style="padding: 10px;">
            <div v-for="(indicator, index) in experienceIndicators" :key="index" style="margin-bottom: 20px;">
              <div style="display: flex; justify-content: space-between; margin-bottom: 5px;">
                <span style="font-weight: bold;">{{ indicator.name }}</span>
                <span :style="{ color: indicator.color }">{{ indicator.value }}{{ indicator.unit }}</span>
              </div>
              <el-progress 
                :percentage="indicator.percentage"
                :color="indicator.color"
                :stroke-width="12"
              />
              <div style="font-size: 12px; color: #95a5a6; margin-top: 3px;">
                {{ indicator.description }}
              </div>
            </div>
          </div>

          <el-divider />
          
          <div style="text-align: center; padding: 10px;">
            <div style="font-size: 14px; color: #7f8c8d; margin-bottom: 10px;">
              <el-icon><InfoFilled /></el-icon> 系统提示
            </div>
            <el-alert
              :title="systemTip.title"
              :type="systemTip.type"
              show-icon
              :closable="false"
            >
              {{ systemTip.content }}
            </el-alert>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'
import webSocket from '../utils/websocket'

const overview = ref({})
const areas = ref([])
const spots = ref([])
const pieChart = ref(null)
const barChart = ref(null)
let pieChartInstance = null
let barChartInstance = null

const crowdLevelMap = {
  EMPTY: { description: '空闲', color: '#4CAF50' },
  COMFORTABLE: { description: '舒适', color: '#8BC34A' },
  MODERATE: { description: '适中', color: '#FFC107' },
  BUSY: { description: '拥挤', color: '#FF9800' },
  OVERCROWDED: { description: '超负荷', color: '#F44336' }
}

const totalAreas = computed(() => areas.value.length)

const safetyScore = computed(() => {
  if (areas.value.length === 0) return 100
  
  let totalScore = 0
  areas.value.forEach(area => {
    const ratio = area.currentVisitors / area.maxCapacity
    if (ratio < 0.3) totalScore += 100
    else if (ratio < 0.5) totalScore += 80
    else if (ratio < 0.7) totalScore += 60
    else if (ratio < 0.9) totalScore += 30
    else totalScore += 10
  })
  
  return totalScore / areas.value.length
})

const riskAssessment = computed(() => {
  return areas.value.map(area => {
    const ratio = area.currentVisitors / area.maxCapacity
    let riskLevel = 0
    let suggestion = ''
    
    if (ratio < 0.3) {
      riskLevel = 1
      suggestion = '该区域空间充足，可引导游客前往'
    } else if (ratio < 0.5) {
      riskLevel = 2
      suggestion = '该区域运行正常，可正常接待'
    } else if (ratio < 0.7) {
      riskLevel = 3
      suggestion = '建议适当控制进入人数'
    } else if (ratio < 0.9) {
      riskLevel = 4
      suggestion = '拥挤风险增加，建议加强疏导'
    } else {
      riskLevel = 5
      suggestion = '已接近饱和，建议临时限流'
    }
    
    return {
      areaName: area.name,
      currentVisitors: area.currentVisitors,
      maxCapacity: area.maxCapacity,
      crowdRatio: ratio,
      crowdLevel: area.crowdLevel,
      riskLevel,
      suggestion
    }
  }).sort((a, b) => b.crowdRatio - a.crowdRatio)
})

const experienceIndicators = computed(() => {
  const comfortableAreas = areas.value.filter(a => {
    const ratio = a.currentVisitors / a.maxCapacity
    return ratio < 0.5
  }).length
  
  const avgRatio = areas.value.length > 0 
    ? areas.value.reduce((sum, a) => sum + (a.currentVisitors / a.maxCapacity), 0) / areas.value.length 
    : 0
  
  return [
    {
      name: '舒适度指数',
      value: (comfortableAreas / (areas.value.length || 1) * 100).toFixed(0),
      unit: '%',
      percentage: Math.round(comfortableAreas / (areas.value.length || 1) * 100),
      color: '#11998e',
      description: '舒适区域占总区域的比例'
    },
    {
      name: '空间利用率',
      value: (avgRatio * 100).toFixed(1),
      unit: '%',
      percentage: Math.round(avgRatio * 100),
      color: '#667eea',
      description: '景区整体空间使用情况'
    },
    {
      name: '安全缓冲区',
      value: Math.max(0, 100 - (overview.value.busyAreas || 0) * 20).toFixed(0),
      unit: '',
      percentage: Math.max(0, 100 - (overview.value.busyAreas || 0) * 20),
      color: '#4facfe',
      description: '基于拥挤区域数量的安全评估'
    }
  ]
})

const systemTip = computed(() => {
  const busyCount = overview.value.busyAreas || 0
  const totalVisitors = overview.value.totalVisitors || 0
  
  if (busyCount >= 3) {
    return {
      title: '高拥挤风险警告',
      type: 'warning',
      content: '当前有多个区域处于拥挤状态，建议加强游客引导，必要时采取限流措施。'
    }
  } else if (totalVisitors > 1000) {
    return {
      title: '游客量较大',
      type: 'info',
      content: '当前景区游客量较大，请密切关注各区域拥挤情况，做好疏导准备。'
    }
  } else {
    return {
      title: '运行正常',
      type: 'success',
      content: '当前景区运行平稳，各区域游客分布合理，游客体验良好。'
    }
  }
})

const getOverallColor = () => {
  const ratio = (overview.value.totalVisitors || 0) / (overview.value.totalCapacity || 1)
  if (ratio < 0.3) return '#4CAF50'
  if (ratio < 0.5) return '#8BC34A'
  if (ratio < 0.7) return '#FFC107'
  if (ratio < 0.9) return '#FF9800'
  return '#F44336'
}

const getSafetyTagType = () => {
  const score = safetyScore.value
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

const getSafetyLevel = () => {
  const score = safetyScore.value
  if (score >= 80) return '安全'
  if (score >= 60) return '一般'
  return '需注意'
}

const getCrowdColor = (level) => {
  if (!level) return '#95a5a6'
  return crowdLevelMap[level]?.color || '#95a5a6'
}

const getRiskTagType = (riskLevel) => {
  if (riskLevel <= 2) return 'success'
  if (riskLevel <= 3) return 'warning'
  return 'danger'
}

const getRiskTextType = (riskLevel) => {
  if (riskLevel <= 2) return 'success'
  if (riskLevel <= 3) return 'warning'
  return 'danger'
}

const getRiskLevelName = (riskLevel) => {
  const map = {
    1: '低风险',
    2: '正常',
    3: '中等',
    4: '较高',
    5: '高风险'
  }
  return map[riskLevel] || '未知'
}

const initCharts = () => {
  if (pieChart.value && !pieChartInstance) {
    pieChartInstance = echarts.init(pieChart.value)
  }
  if (barChart.value && !barChartInstance) {
    barChartInstance = echarts.init(barChart.value)
  }
  updateCharts()
}

const updateCharts = () => {
  if (!pieChartInstance || !barChartInstance) return
  if (areas.value.length === 0) return

  const sortedAreas = [...areas.value].sort((a, b) => b.currentVisitors - a.currentVisitors)
  
  const pieOption = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}人 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center'
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          }
        },
        data: sortedAreas.map(area => ({
          name: area.name,
          value: area.currentVisitors,
          itemStyle: {
            color: getCrowdColor(area.crowdLevel)
          }
        }))
      }
    ]
  }

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
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: sortedAreas.map(a => a.name),
      axisLabel: {
        rotate: 30,
        interval: 0
      }
    },
    yAxis: {
      type: 'value',
      name: '拥挤率 (%)',
      max: 100
    },
    series: [
      {
        type: 'bar',
        barWidth: '60%',
        data: sortedAreas.map(area => ({
          value: Math.round((area.currentVisitors / area.maxCapacity) * 100),
          itemStyle: {
            color: getCrowdColor(area.crowdLevel),
            borderRadius: [4, 4, 0, 0]
          }
        }))
      }
    ]
  }

  pieChartInstance.setOption(pieOption, true)
  barChartInstance.setOption(barOption, true)
}

const fetchInitialData = async () => {
  try {
    const [areasRes, overviewRes, spotsRes] = await Promise.all([
      axios.get('/api/areas'),
      axios.get('/api/overview'),
      axios.get('/api/spots')
    ])
    areas.value = areasRes.data
    overview.value = overviewRes.data
    spots.value = spotsRes.data
  } catch (e) {
    console.error('获取初始数据失败', e)
  }
}

const handleWebSocketData = (data) => {
  if (data.areas) {
    areas.value = data.areas
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
    updateCharts()
  })
}

const handleResize = () => {
  if (pieChartInstance) pieChartInstance.resize()
  if (barChartInstance) barChartInstance.resize()
}

onMounted(() => {
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
  if (pieChartInstance) pieChartInstance.dispose()
  if (barChartInstance) barChartInstance.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>