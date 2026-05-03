<template>
  <el-container style="height: 100vh">
    <el-aside width="220px" style="background-color: #304156; color: #fff">
      <div class="logo">
        <el-icon :size="28" style="vertical-align: middle; margin-right: 10px"><OfficeBuilding /></el-icon>
        <span style="font-size: 18px; font-weight: bold">OA管理系统</span>
      </div>
      <el-menu
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        :default-active="activeMenu"
        router
        style="border-right: none"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>
        
        <el-sub-menu index="system" v-if="isAdmin">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/departments">
            <el-icon><OfficeBuilding /></el-icon>
            <template #title>部门管理</template>
          </el-menu-item>
          <el-menu-item index="/roles">
            <el-icon><User /></el-icon>
            <template #title>角色管理</template>
          </el-menu-item>
          <el-menu-item index="/employees">
            <el-icon><Avatar /></el-icon>
            <template #title>用户管理</template>
          </el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/leave-apply">
          <el-icon><Edit /></el-icon>
          <template #title>请假申请</template>
        </el-menu-item>
        <el-menu-item index="/my-leaves">
          <el-icon><Document /></el-icon>
          <template #title>我的请假记录</template>
        </el-menu-item>
        
        <el-menu-item index="/leave-approval" v-if="isAdmin">
          <el-icon><Check /></el-icon>
          <template #title>请假审批</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header style="background-color: #fff; box-shadow: 0 1px 4px rgba(0,21,41,.08); display: flex; align-items: center; justify-content: space-between; padding: 0 20px">
        <div style="font-size: 16px; font-weight: 500">
          {{ currentPageTitle }}
        </div>
        <div class="user-info" style="display: flex; align-items: center; gap: 15px">
          <div class="status-badge" :class="statusClass">
            <el-avatar :size="40" style="background-color: #409EFF">
              <el-icon :size="24"><User /></el-icon>
            </el-avatar>
            <span class="status-dot"></span>
          </div>
          <div style="text-align: right">
            <div style="font-weight: 500">{{ user.name }}</div>
            <div style="font-size: 12px; color: #909399">
              {{ user.departmentName }} | {{ user.roleName }}
              <span :style="{ color: statusColor, marginLeft: '10px' }">
                [{{ statusText }}]
              </span>
            </div>
          </div>
          <el-button type="text" @click="logout" style="margin-left: 10px">
            <el-icon><SwitchButton /></el-icon>
            退出
          </el-button>
        </div>
      </el-header>

      <el-main style="background-color: #f5f7fa; padding: 20px">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getEmployee } from '../api'

const route = useRoute()
const router = useRouter()

const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const isAdmin = computed(() => user.value.roleId === 1)

const activeMenu = computed(() => route.path)

const currentPageTitle = computed(() => {
  if (route.meta && route.meta.title) {
    return route.meta.title
  }
  return '首页'
})

const statusClass = computed(() => {
  return user.value.status === 'ON_LEAVE' ? 'on-leave' : 'normal'
})

const statusColor = computed(() => {
  return user.value.status === 'ON_LEAVE' ? '#e6a23c' : '#67c23a'
})

const statusText = computed(() => {
  return user.value.status === 'ON_LEAVE' ? '请假中' : '正常'
})

const refreshUserInfo = async () => {
  const stored = JSON.parse(localStorage.getItem('user') || '{}')
  if (stored.id) {
    const res = await getEmployee(stored.id)
    if (res.code === 200) {
      user.value = res.data
      localStorage.setItem('user', JSON.stringify(res.data))
    }
  }
}

const logout = () => {
  localStorage.removeItem('user')
  router.push('/login')
}

watch(() => route.path, () => {
  refreshUserInfo()
}, { immediate: true })
</script>

<style scoped>
.logo {
  height: 60px;
  line-height: 60px;
  padding: 0 20px;
  background-color: #263445;
  color: #fff;
}

.user-info {
  display: flex;
  align-items: center;
}

.status-badge {
  position: relative;
  display: inline-block;
}

.status-dot {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid #fff;
}

.normal .status-dot {
  background-color: #67c23a;
}

.on-leave .status-dot {
  background-color: #e6a23c;
}
</style>