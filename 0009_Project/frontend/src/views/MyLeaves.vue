<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>我的请假记录</span>
          <el-radio-group v-model="statusFilter" @change="handleFilterChange" size="small">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="PENDING">待审批</el-radio-button>
            <el-radio-button label="APPROVED">已通过</el-radio-button>
            <el-radio-button label="REJECTED">已拒绝</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      
      <el-table :data="filteredLeaves" stripe v-if="filteredLeaves.length > 0">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column prop="reason" label="请假事由" min-width="200" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="approvalComment" label="审批意见" min-width="150">
          <template #default="scope">
            {{ scope.row.approvalComment || '暂无' }}
          </template>
        </el-table-column>
        <el-table-column prop="approverName" label="审批人" width="100">
          <template #default="scope">
            {{ scope.row.approverName || '待审批' }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="180" />
      </el-table>
      <el-empty description="暂无请假记录" v-else />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getMyLeaves } from '../api'

const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const leaves = ref([])
const statusFilter = ref('')

const filteredLeaves = computed(() => {
  if (!statusFilter.value) {
    return leaves.value
  }
  return leaves.value.filter(leave => leave.status === statusFilter.value)
})

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

const loadLeaves = async () => {
  const res = await getMyLeaves(user.value.id)
  leaves.value = res.data || []
}

const handleFilterChange = () => {
  // computed will handle it
}

onMounted(() => {
  loadLeaves()
})
</script>

<style scoped>
.page-container {
  padding: 0;
}
</style>