<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
              <el-icon :size="32"><OfficeBuilding /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.departments }}</div>
              <div class="stat-label">部门总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
              <el-icon :size="32"><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.employees }}</div>
              <div class="stat-label">员工总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
              <el-icon :size="32"><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pendingLeaves }}</div>
              <div class="stat-label">待审批请假</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)">
              <el-icon :size="32"><Check /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.onLeave }}</div>
              <div class="stat-label">请假中员工</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>快捷操作</span>
            </div>
          </template>
          <div class="quick-actions">
            <router-link to="/leave-apply" class="action-item">
              <el-icon :size="36" style="color: #409EFF"><Edit /></el-icon>
              <span>申请请假</span>
            </router-link>
            <router-link to="/my-leaves" class="action-item">
              <el-icon :size="36" style="color: #67c23a"><Document /></el-icon>
              <span>我的请假</span>
            </router-link>
            <router-link v-if="isAdmin" to="/leave-approval" class="action-item">
              <el-icon :size="36" style="color: #e6a23c"><Check /></el-icon>
              <span>请假审批</span>
            </router-link>
            <router-link v-if="isAdmin" to="/employees" class="action-item">
              <el-icon :size="36" style="color: #f56c6c"><Avatar /></el-icon>
              <span>用户管理</span>
            </router-link>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>个人信息</span>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="姓名">{{ user.name }}</el-descriptions-item>
            <el-descriptions-item label="用户名">{{ user.username }}</el-descriptions-item>
            <el-descriptions-item label="部门">{{ user.departmentName }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ user.roleName }}</el-descriptions-item>
            <el-descriptions-item label="当前状态" :span="2">
              <el-tag :type="user.status === 'ON_LEAVE' ? 'warning' : 'success'">
                {{ user.status === 'ON_LEAVE' ? '请假中' : '正常' }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getDepartments, getEmployees, getPendingLeaves } from '../api'

const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const isAdmin = computed(() => user.value.roleId === 1)

const stats = ref({
  departments: 0,
  employees: 0,
  pendingLeaves: 0,
  onLeave: 0
})

const loadStats = async () => {
  try {
    const deptRes = await getDepartments()
    stats.value.departments = deptRes.data?.length || 0
  } catch (e) {}

  try {
    const empRes = await getEmployees()
    stats.value.employees = empRes.data?.length || 0
    stats.value.onLeave = empRes.data?.filter(e => e.status === 'ON_LEAVE').length || 0
  } catch (e) {}

  if (isAdmin.value) {
    try {
      const leaveRes = await getPendingLeaves()
      stats.value.pendingLeaves = leaveRes.data?.length || 0
    } catch (e) {}
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.stat-card {
  margin-bottom: 20px;
}

.stat-content {
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 70px;
  height: 70px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-info {
  margin-left: 20px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 100px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  text-decoration: none;
  color: #606266;
  transition: all 0.3s;
}

.action-item:hover {
  border-color: #409EFF;
  background-color: #ecf5ff;
}

.action-item span {
  margin-top: 10px;
  font-size: 14px;
}
</style>