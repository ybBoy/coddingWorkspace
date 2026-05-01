<template>
  <div>
    <el-card shadow="hover" style="margin-bottom: 20px;">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: bold; font-size: 16px;">
            <el-icon><Setting /></el-icon> 引导信息设置
          </span>
          <el-switch
            v-model="autoPublishEnabled"
            active-text="自动发布已开启"
            inactive-text="自动发布已关闭"
            @change="toggleAutoPublish"
          />
        </div>
      </template>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <el-descriptions title="模拟器控制" :column="1" border>
            <el-descriptions-item label="状态">
              <el-tag :type="simulatorRunning ? 'success' : 'info'">
                {{ simulatorRunning ? '运行中' : '已停止' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="操作">
              <el-button-group>
                <el-button type="success" @click="startSimulator" :disabled="simulatorRunning">
                  <el-icon><VideoPlay /></el-icon> 启动
                </el-button>
                <el-button type="warning" @click="stopSimulator" :disabled="!simulatorRunning">
                  <el-icon><VideoPause /></el-icon> 停止
                </el-button>
                <el-button type="danger" @click="resetSimulator">
                  <el-icon><RefreshRight /></el-icon> 重置
                </el-button>
              </el-button-group>
            </el-descriptions-item>
          </el-descriptions>
        </el-col>
        <el-col :span="12">
          <el-descriptions title="自动发布规则" :column="1" border>
            <el-descriptions-item label="触发条件">
              <span>当区域拥挤程度为"拥挤"或"超负荷"时</span>
            </el-descriptions-item>
            <el-descriptions-item label="发布间隔">
              <span>每10秒检查一次，同一区域5分钟内不重复发布</span>
            </el-descriptions-item>
            <el-descriptions-item label="默认显示位置">
              <el-tag>入口大屏</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="hover" style="margin-bottom: 20px;">
      <template #header>
        <span style="font-weight: bold; font-size: 16px;">
          <el-icon><Edit /></el-icon> 发布新引导
        </span>
      </template>
      
      <el-form :model="form" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="标题">
              <el-input v-model="form.title" placeholder="请输入引导标题" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="类型">
              <el-select v-model="form.type" style="width: 100%;">
                <el-option label="拥挤预警" value="CROWD_WARNING" />
                <el-option label="引导建议" value="GUIDANCE_SUGGESTION" />
                <el-option label="紧急通知" value="EMERGENCY" />
                <el-option label="普通信息" value="INFO" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="有效时间">
              <el-select v-model="form.durationMinutes" style="width: 100%;">
                <el-option label="5分钟" :value="5" />
                <el-option label="10分钟" :value="10" />
                <el-option label="30分钟" :value="30" />
                <el-option label="1小时" :value="60" />
                <el-option label="2小时" :value="120" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="3"
            placeholder="请输入引导内容"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="publishGuidance" :loading="publishing">
            <el-icon><Promotion /></el-icon> 发布引导
          </el-button>
          <el-button @click="resetForm">
            <el-icon><RefreshRight /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: bold; font-size: 16px;">
            <el-icon><List /></el-icon> 引导信息历史
          </span>
          <el-radio-group v-model="filterType" size="small">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="active">进行中</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      
      <el-table :data="filteredMessages" style="width: 100%" stripe>
        <el-table-column prop="title" label="标题" width="200">
          <template #default="{ row }">
            <span style="font-weight: bold;">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="300">
          <template #default="{ row }">
            <el-text :type="getGuidanceTextType(row.type)" size="small">
              {{ row.content }}
            </el-text>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getGuidanceType(row.type)" size="small" effect="dark">
              {{ getGuidanceTypeName(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.source === 'AUTO' ? 'info' : 'primary'" size="small">
              {{ row.source === 'AUTO' ? '自动' : '手动' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="active" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'" size="small">
              {{ row.active ? '进行中' : '已结束' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.active"
              type="danger"
              link
              size="small"
              @click="deactivateMessage(row)"
            >
              结束
            </el-button>
            <span v-else style="color: #95a5a6;">-</span>
          </template>
        </el-table-column>
      </el-table>
      
      <el-empty v-if="filteredMessages.length === 0" description="暂无引导信息" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const autoPublishEnabled = ref(true)
const simulatorRunning = ref(true)
const publishing = ref(false)
const filterType = ref('all')
const messages = ref([])

const form = ref({
  title: '',
  content: '',
  type: 'INFO',
  durationMinutes: 30
})

const filteredMessages = computed(() => {
  if (filterType.value === 'active') {
    return messages.value.filter(m => m.active)
  }
  return messages.value
})

const getGuidanceType = (type) => {
  const map = {
    CROWD_WARNING: 'warning',
    GUIDANCE_SUGGESTION: 'info',
    EMERGENCY: 'danger',
    INFO: 'success'
  }
  return map[type] || 'info'
}

const getGuidanceTextType = (type) => {
  const map = {
    CROWD_WARNING: 'warning',
    GUIDANCE_SUGGESTION: 'primary',
    EMERGENCY: 'danger',
    INFO: ''
  }
  return map[type] || ''
}

const getGuidanceTypeName = (type) => {
  const map = {
    CROWD_WARNING: '拥挤预警',
    GUIDANCE_SUGGESTION: '引导建议',
    EMERGENCY: '紧急通知',
    INFO: '普通信息'
  }
  return map[type] || '未知'
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  return new Date(timeStr).toLocaleString('zh-CN')
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

const fetchMessages = async () => {
  try {
    const res = await axios.get('/api/guidance', { params: { activeOnly: false } })
    messages.value = res.data
  } catch (e) {
    console.error('获取引导信息失败', e)
  }
}

const toggleAutoPublish = async (val) => {
  try {
    if (val) {
      await axios.post('/api/auto-publish/enable')
      ElMessage.success('自动发布已启用')
    } else {
      await axios.post('/api/auto-publish/disable')
      ElMessage.warning('自动发布已禁用')
    }
  } catch (e) {
    ElMessage.error('操作失败')
    autoPublishEnabled.value = !val
  }
}

const startSimulator = async () => {
  try {
    await axios.post('/api/simulator/start')
    simulatorRunning.value = true
    ElMessage.success('模拟器已启动')
  } catch (e) {
    ElMessage.error('启动失败')
  }
}

const stopSimulator = async () => {
  try {
    await axios.post('/api/simulator/stop')
    simulatorRunning.value = false
    ElMessage.success('模拟器已停止')
  } catch (e) {
    ElMessage.error('停止失败')
  }
}

const resetSimulator = async () => {
  try {
    await axios.post('/api/simulator/reset')
    ElMessage.success('数据已重置')
  } catch (e) {
    ElMessage.error('重置失败')
  }
}

const publishGuidance = async () => {
  if (!form.value.title || !form.value.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }

  publishing.value = true
  try {
    await axios.post('/api/guidance', form.value)
    ElMessage.success('发布成功')
    resetForm()
    fetchMessages()
  } catch (e) {
    ElMessage.error('发布失败')
  } finally {
    publishing.value = false
  }
}

const resetForm = () => {
  form.value = {
    title: '',
    content: '',
    type: 'INFO',
    durationMinutes: 30
  }
}

const deactivateMessage = async (row) => {
  try {
    await axios.delete(`/api/guidance/${row.id}`)
    ElMessage.success('已结束')
    fetchMessages()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  fetchStatus()
  fetchMessages()
})
</script>