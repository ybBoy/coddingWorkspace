<template>
  <el-container style="height: 100vh">
    <el-header style="background: linear-gradient(90deg, #2c3e50, #34495e); color: white; display: flex; align-items: center; justify-content: space-between; padding: 0 20px;">
      <div style="display: flex; align-items: center; gap: 15px;">
        <el-icon :size="32" color="#3498db"><Monitor /></el-icon>
        <span style="font-size: 24px; font-weight: bold;">景区游客活动范围管理系统</span>
      </div>
      <div style="display: flex; align-items: center; gap: 20px;">
        <el-tag :type="simulatorRunning ? 'success' : 'info'">
          <el-icon v-if="simulatorRunning"><VideoPlay /></el-icon>
          <el-icon v-else><VideoPause /></el-icon>
          模拟：{{ simulatorRunning ? '运行中' : '已停止' }}
        </el-tag>
        <el-tag :type="autoPublishEnabled ? 'success' : 'warning'">
          <el-icon v-if="autoPublishEnabled"><Promotion /></el-icon>
          <el-icon v-else><CircleClose /></el-icon>
          自动发布：{{ autoPublishEnabled ? '已启用' : '已禁用' }}
        </el-tag>
        <div style="color: #bdc3c7;">
          <el-icon><Clock /></el-icon>
          {{ currentTime }}
        </div>
      </div>
    </el-header>
    <el-container>
      <el-aside width="220px" style="background-color: #2c3e50;">
        <el-menu
          :default-active="activeMenu"
          router
          background-color="#2c3e50"
          text-color="#bdc3c7"
          active-text-color="#3498db"
          style="border-right: none;"
        >
          <el-menu-item index="/">
            <el-icon><DataAnalysis /></el-icon>
            <span>实时监控</span>
          </el-menu-item>
          <el-menu-item index="/guidance">
            <el-icon><ChatDotRound /></el-icon>
            <span>引导信息</span>
          </el-menu-item>
          <el-menu-item index="/statistics">
            <el-icon><TrendCharts /></el-icon>
            <span>统计分析</span>
          </el-menu-item>
          <el-menu-item index="/display">
            <el-icon><Monitor /></el-icon>
            <span>大屏展示</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main style="background-color: #f5f6fa; padding: 20px; overflow-y: auto;">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" :key="$route.path" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const currentTime = ref('')
const simulatorRunning = ref(true)
const autoPublishEnabled = ref(true)

const activeMenu = computed(() => route.path || '/')

let timeInterval = null

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const fetchStatus = async () => {
  try {
    const [simulatorRes, autoPublishRes] = await Promise.all([
      axios.get('/api/simulator/status'),
      axios.get('/api/auto-publish/status')
    ])
    simulatorRunning.value = simulatorRes.data.running
    autoPublishEnabled.value = autoPublishRes.data.enabled
  } catch (e) {
    console.error('获取状态失败', e)
  }
}

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)
  fetchStatus()
  
  const statusInterval = setInterval(fetchStatus, 5000)
  onUnmounted(() => {
    clearInterval(timeInterval)
    clearInterval(statusInterval)
  })
})

onUnmounted(() => {
  if (timeInterval) clearInterval(timeInterval)
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.el-main::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.el-main::-webkit-scrollbar-thumb {
  background-color: #95a5a6;
  border-radius: 3px;
}

.el-main::-webkit-scrollbar-track {
  background-color: #ecf0f1;
}
</style>