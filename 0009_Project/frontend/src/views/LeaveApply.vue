<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <span>请假申请</span>
      </template>
      
      <div class="apply-form">
        <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" style="max-width: 600px">
          <el-form-item label="申请人">
            <el-input :value="user.name" disabled />
          </el-form-item>
          <el-form-item label="所属部门">
            <el-input :value="user.departmentName" disabled />
          </el-form-item>
          <el-form-item label="请假类型">
            <el-select v-model="form.leaveType" placeholder="请选择请假类型" style="width: 100%">
              <el-option label="事假" value="personal" />
              <el-option label="病假" value="sick" />
              <el-option label="年假" value="annual" />
              <el-option label="婚假" value="marriage" />
              <el-option label="产假" value="maternity" />
            </el-select>
          </el-form-item>
          <el-form-item label="开始日期" prop="startDate">
            <el-date-picker
              v-model="form.startDate"
              type="date"
              placeholder="选择开始日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
              :disabled-date="disabledDate"
            />
          </el-form-item>
          <el-form-item label="结束日期" prop="endDate">
            <el-date-picker
              v-model="form.endDate"
              type="date"
              placeholder="选择结束日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
              :disabled-date="disabledEndDate"
            />
          </el-form-item>
          <el-form-item label="请假天数">
            <el-input :value="leaveDays + ' 天'" disabled />
          </el-form-item>
          <el-form-item label="请假事由" prop="reason">
            <el-input
              v-model="form.reason"
              type="textarea"
              :rows="4"
              placeholder="请输入请假事由"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="handleSubmit" size="large">
              提交申请
            </el-button>
            <el-button @click="resetForm" size="large">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card style="margin-top: 20px">
      <template #header>
        <span>最近请假记录</span>
      </template>
      <el-table :data="recentLeaves" stripe v-if="recentLeaves.length > 0">
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column prop="reason" label="请假事由" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="180" />
      </el-table>
      <el-empty description="暂无请假记录" v-else />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { applyLeave, getMyLeaves } from '../api'

const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const formRef = ref(null)
const loading = ref(false)
const recentLeaves = ref([])

const form = reactive({
  employeeId: user.value.id,
  startDate: '',
  endDate: '',
  reason: '',
  leaveType: ''
})

const rules = {
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
  reason: [{ required: true, message: '请输入请假事由', trigger: 'blur' }]
}

const leaveDays = computed(() => {
  if (form.startDate && form.endDate) {
    const start = new Date(form.startDate)
    const end = new Date(form.endDate)
    const diff = Math.floor((end - start) / (1000 * 60 * 60 * 24)) + 1
    return diff >= 0 ? diff : 0
  }
  return 0
})

const disabledDate = (time) => {
  return time.getTime() < Date.now() - 8.64e7
}

const disabledEndDate = (time) => {
  if (form.startDate) {
    const start = new Date(form.startDate)
    return time.getTime() < start.getTime() - 8.64e7
  }
  return time.getTime() < Date.now() - 8.64e7
}

const getStatusType = (status) => {
  switch (status) {
    case 'PENDING': return 'warning'
    case 'APPROVED': return 'success'
    case 'REJECTED': return 'danger'
    default: return 'info'
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 'PENDING': return '待审批'
    case 'APPROVED': return '已通过'
    case 'REJECTED': return '已拒绝'
    default: return status
  }
}

const loadRecentLeaves = async () => {
  const res = await getMyLeaves(user.value.id)
  recentLeaves.value = (res.data || []).slice(0, 5)
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (form.startDate > form.endDate) {
    ElMessage.error('结束日期不能早于开始日期')
    return
  }

  loading.value = true
  try {
    const data = {
      employeeId: user.value.id,
      startDate: form.startDate,
      endDate: form.endDate,
      reason: form.reason
    }
    await applyLeave(data)
    ElMessage.success('请假申请已提交，您的状态已更新为"请假中"')
    resetForm()
    loadRecentLeaves()
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.startDate = ''
  form.endDate = ''
  form.reason = ''
  form.leaveType = ''
  formRef.value?.resetFields()
}

onMounted(() => {
  loadRecentLeaves()
})
</script>

<style scoped>
.page-container {
  padding: 0;
}

.apply-form {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}
</style>