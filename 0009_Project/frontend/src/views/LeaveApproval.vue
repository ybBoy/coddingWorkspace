<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>请假审批</span>
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
        <el-table-column prop="employeeName" label="申请人" width="100" />
        <el-table-column prop="departmentName" label="所属部门" width="120" />
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column prop="reason" label="请假事由" min-width="150" />
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
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <template v-if="scope.row.status === 'PENDING'">
              <el-button type="success" link @click="handleApprove(scope.row, true)">
                通过
              </el-button>
              <el-button type="danger" link @click="handleApprove(scope.row, false)">
                拒绝
              </el-button>
            </template>
            <template v-else>
              <el-button type="info" link disabled>已处理</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <el-empty description="暂无请假申请" v-else />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isApproved ? '通过申请' : '拒绝申请'" width="500px">
      <el-form :model="approvalForm" label-width="80px">
        <el-form-item label="申请人">
          <el-input :value="currentLeave?.employeeName" disabled />
        </el-form-item>
        <el-form-item label="请假日期">
          <el-input :value="currentLeave?.startDate + ' 至 ' + currentLeave?.endDate" disabled />
        </el-form-item>
        <el-form-item label="请假事由">
          <el-input :value="currentLeave?.reason" type="textarea" :rows="2" disabled />
        </el-form-item>
        <el-form-item label="审批意见" prop="approvalComment">
          <el-input
            v-model="approvalForm.approvalComment"
            type="textarea"
            :rows="3"
            :placeholder="isApproved ? '请输入审批通过意见（可选）' : '请输入拒绝理由'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :type="isApproved ? 'success' : 'danger'" :loading="loading" @click="submitApproval">
          {{ isApproved ? '确认通过' : '确认拒绝' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getLeaves, approveLeave } from '../api'

const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const leaves = ref([])
const statusFilter = ref('')
const dialogVisible = ref(false)
const isApproved = ref(false)
const currentLeave = ref(null)
const loading = ref(false)

const approvalForm = reactive({
  approvalComment: ''
})

const filteredLeaves = computed(() => {
  let result = leaves.value
  if (statusFilter.value) {
    result = result.filter(leave => leave.status === statusFilter.value)
  }
  return result.sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
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
  const res = await getLeaves()
  leaves.value = res.data || []
}

const handleFilterChange = () => {
  // computed will handle it
}

const handleApprove = (row, approved) => {
  currentLeave.value = row
  isApproved.value = approved
  approvalForm.approvalComment = ''
  dialogVisible.value = true
}

const submitApproval = async () => {
  if (!isApproved.value && !approvalForm.approvalComment.trim()) {
    ElMessage.warning('请输入拒绝理由')
    return
  }

  loading.value = true
  try {
    const data = {
      leaveId: currentLeave.value.id,
      approverId: user.value.id,
      approvalComment: approvalForm.approvalComment || (isApproved.value ? '同意' : '不同意'),
      approved: isApproved.value
    }
    
    await approveLeave(data)
    ElMessage.success(isApproved.value ? '已通过申请' : '已拒绝申请')
    dialogVisible.value = false
    loadLeaves()
  } finally {
    loading.value = false
  }
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