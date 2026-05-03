<template>
  <div id="app">
    <el-container>
      <el-header class="header">
        <div class="logo">
          <i class="el-icon-office-building"></i>
          电动汽车库房管理系统
          <el-tag :type="currentRole === 'admin' ? 'danger' : 'primary'" size="medium" class="role-tag">
            {{ currentRole === 'admin' ? '管理员' : '普通用户' }}
          </el-tag>
        </div>
        <div class="role-switcher">
          <el-dropdown>
            <span class="switcher-text">
              <i class="el-icon-setting"></i> 切换视图
              <i class="el-icon-arrow-down el-icon--right"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item @click.native="switchToUser">
                <i class="el-icon-user"></i> 普通用户视图
              </el-dropdown-item>
              <el-dropdown-item @click.native="switchToAdmin">
                <i class="el-icon-s-operation"></i> 管理员视图
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </el-header>
      <el-container>
        <el-aside width="220px" class="aside">
          <el-menu
            :default-active="activeMenu"
            class="el-menu-vertical-demo"
            background-color="#545c64"
            text-color="#fff"
            active-text-color="#ffd04b"
            router
          >
            <template v-if="currentRole === 'user'">
              <el-menu-item index="/user/search">
                <i class="el-icon-search"></i>
                <span slot="title">零件搜索</span>
              </el-menu-item>
              <el-menu-item index="/user/request-submit">
                <i class="el-icon-s-order"></i>
                <span slot="title">提交出库申请</span>
              </el-menu-item>
              <el-menu-item index="/user/requests">
                <i class="el-icon-document"></i>
                <span slot="title">我的申请</span>
              </el-menu-item>
              <el-menu-item index="/user/records">
                <i class="el-icon-tickets"></i>
                <span slot="title">出入库记录</span>
              </el-menu-item>
            </template>
            
            <template v-else-if="currentRole === 'admin'">
              <el-menu-item index="/admin">
                <i class="el-icon-s-home"></i>
                <span slot="title">首页</span>
              </el-menu-item>
              <el-menu-item index="/admin/parts">
                <i class="el-icon-s-grid"></i>
                <span slot="title">库存管理</span>
              </el-menu-item>
              <el-menu-item index="/admin/search">
                <i class="el-icon-search"></i>
                <span slot="title">零件搜索</span>
              </el-menu-item>
              <el-menu-item index="/admin/stock">
                <i class="el-icon-upload2"></i>
                <span slot="title">入库/出库</span>
              </el-menu-item>
              <el-menu-item index="/admin/requests">
                <i class="el-icon-s-claim"></i>
                <span slot="title">申请审核</span>
                <el-badge :value="pendingRequestCount" :max="99" class="badge" v-if="pendingRequestCount > 0"></el-badge>
              </el-menu-item>
              <el-menu-item index="/admin/records">
                <i class="el-icon-document"></i>
                <span slot="title">出入库记录</span>
              </el-menu-item>
              <el-menu-item index="/admin/restock">
                <i class="el-icon-warning"></i>
                <span slot="title">需补货列表</span>
                <el-badge :value="adminRestockCount" :max="99" class="badge" v-if="adminRestockCount > 0"></el-badge>
              </el-menu-item>
            </template>
            
            <template v-else>
              <el-menu-item index="/">
                <i class="el-icon-s-home"></i>
                <span slot="title">首页</span>
              </el-menu-item>
              <el-menu-item index="/parts">
                <i class="el-icon-s-grid"></i>
                <span slot="title">库存列表</span>
              </el-menu-item>
              <el-menu-item index="/search">
                <i class="el-icon-search"></i>
                <span slot="title">零件搜索</span>
              </el-menu-item>
              <el-menu-item index="/stock">
                <i class="el-icon-upload2"></i>
                <span slot="title">入库/出库</span>
              </el-menu-item>
              <el-menu-item index="/records">
                <i class="el-icon-document"></i>
                <span slot="title">出入库记录</span>
              </el-menu-item>
              <el-menu-item index="/restock">
                <i class="el-icon-warning"></i>
                <span slot="title">需补货列表</span>
                <el-badge :value="restockCount" :max="99" class="badge" v-if="restockCount > 0"></el-badge>
              </el-menu-item>
            </template>
          </el-menu>
        </el-aside>
        <el-main class="main">
          <router-view/>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import partApi from './api/partApi'
import { eventBus } from './utils/eventBus'

export default {
  name: 'App',
  data() {
    return {
      restockCount: 0,
      adminRestockCount: 0,
      pendingRequestCount: 0
    }
  },
  computed: {
    currentRole() {
      const path = this.$route.path
      if (path.startsWith('/admin')) {
        return 'admin'
      } else if (path.startsWith('/user')) {
        return 'user'
      }
      return 'default'
    },
    activeMenu() {
      return this.$route.path
    }
  },
  created() {
    this.loadRestockCounts()
    this.loadPendingRequestCount()
    eventBus.$on('stock-changed', () => {
      this.loadRestockCounts()
    })
    eventBus.$on('visibility-changed', () => {
      this.loadRestockCounts()
    })
  },
  beforeDestroy() {
    eventBus.$off('stock-changed')
    eventBus.$off('visibility-changed')
  },
  methods: {
    async loadRestockCounts() {
      try {
        const adminRes = await partApi.admin.getPartsNeedRestock()
        if (adminRes.success) {
          this.adminRestockCount = adminRes.data ? adminRes.data.length : 0
          this.restockCount = this.adminRestockCount
        }
      } catch (error) {
        console.error('加载需补货数量失败:', error)
      }
    },
    async loadPendingRequestCount() {
      try {
        const res = await partApi.admin.getPendingRequestCount()
        if (res.success) {
          this.pendingRequestCount = res.data || 0
        }
      } catch (error) {
        console.error('加载待审核申请数量失败:', error)
      }
    },
    switchToUser() {
      this.$router.push('/user/search')
    },
    switchToAdmin() {
      this.$router.push('/admin')
    }
  },
  watch: {
    $route() {
      this.loadRestockCounts()
      this.loadPendingRequestCount()
    }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
}

html, body, #app {
  height: 100%;
}

.el-container {
  height: 100%;
}

.header {
  background-color: #409EFF;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 20px;
  font-weight: bold;
}

.logo {
  display: flex;
  align-items: center;
}

.logo i {
  margin-right: 10px;
  font-size: 28px;
}

.role-tag {
  margin-left: 15px;
}

.role-switcher {
  font-size: 14px;
  cursor: pointer;
}

.switcher-text {
  display: flex;
  align-items: center;
  font-weight: normal;
}

.aside {
  background-color: #545c64;
}

.main {
  background-color: #f5f7fa;
  padding: 20px;
}

.el-menu-vertical-demo {
  height: 100%;
  border-right: none;
}

.badge {
  margin-left: 10px;
}

.page-container {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
  min-height: 400px;
}

.page-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #303133;
  border-bottom: 2px solid #409EFF;
  padding-bottom: 10px;
}

.table-container {
  margin-top: 20px;
}
</style>
