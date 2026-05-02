<template>
  <div id="app">
    <el-container>
      <el-header class="header">
        <div class="logo">
          <i class="el-icon-office-building"></i>
          电动汽车库房管理系统
        </div>
      </el-header>
      <el-container>
        <el-aside width="200px" class="aside">
          <el-menu
            :default-active="activeMenu"
            class="el-menu-vertical-demo"
            background-color="#545c64"
            text-color="#fff"
            active-text-color="#ffd04b"
            router
          >
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
      restockCount: 0
    }
  },
  computed: {
    activeMenu() {
      return this.$route.path
    }
  },
  created() {
    this.loadRestockCount()
    eventBus.$on('stock-changed', () => {
      this.loadRestockCount()
    })
  },
  beforeDestroy() {
    eventBus.$off('stock-changed')
  },
  methods: {
    async loadRestockCount() {
      try {
        const res = await partApi.getPartsNeedRestock()
        if (res.success) {
          this.restockCount = res.data ? res.data.length : 0
        }
      } catch (error) {
        console.error('加载需补货数量失败:', error)
      }
    }
  },
  watch: {
    $route() {
      this.loadRestockCount()
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
