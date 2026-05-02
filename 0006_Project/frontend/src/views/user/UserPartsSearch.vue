<template>
  <div class="page-container">
    <div class="page-title">
      <i class="el-icon-search" style="margin-right: 10px;"></i>零件搜索
    </div>
    
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="分类">
          <el-select v-model="searchForm.category" placeholder="全部分类" clearable style="width: 120px;">
            <el-option label="电池" value="电池"></el-option>
            <el-option label="电机" value="电机"></el-option>
            <el-option label="轮胎" value="轮胎"></el-option>
            <el-option label="控制器" value="控制器"></el-option>
            <el-option label="玻璃" value="玻璃"></el-option>
            <el-option label="机油" value="机油"></el-option>
            <el-option label="其他" value="其他"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="搜索关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="请输入零件编号或名称"
            prefix-icon="el-icon-search"
            clearable
            @keyup.enter.native="handleSearch"
            style="width: 300px;"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="search-tip" v-if="hasSearched">
      <span>搜索「{{ getSearchTip() }}」的结果：共 {{ parts.length }} 条记录</span>
    </div>

    <el-table :data="parts" v-loading="loading" style="width: 100%; margin-top: 20px;">
      <el-table-column prop="id" label="零件编号" width="120">
        <template slot-scope="scope">
          <el-tag>{{ scope.row.id }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="零件名称" width="150">
        <template slot-scope="scope">
          <span class="highlight-text">{{ scope.row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="category" label="分类" width="100"></el-table-column>
      <el-table-column prop="specification" label="规格"></el-table-column>
      <el-table-column prop="quantity" label="库存数量" width="100">
        <template slot-scope="scope">
          <span :class="{'danger-text': scope.row.quantity <= scope.row.minStock}">
            {{ scope.row.quantity }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="minStock" label="最低库存" width="80"></el-table-column>
      <el-table-column prop="unit" label="单位" width="80"></el-table-column>
      <el-table-column label="状态" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.quantity <= scope.row.minStock ? 'danger' : 'success'">
            {{ scope.row.quantity <= scope.row.minStock ? '需补货' : '正常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="handleStockIn(scope.row)">入库</el-button>
          <el-button type="text" size="small" @click="handleStockOut(scope.row)" :disabled="scope.row.quantity <= 0">出库</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && parts.length === 0 && hasSearched" description="未找到相关零件">
      <el-button type="primary" @click="handleReset">查看全部</el-button>
    </el-empty>

    <el-dialog title="入库操作" :visible.sync="stockInDialogVisible" width="400px">
      <div style="margin-bottom: 15px;">
        <p><strong>零件：</strong>{{ currentPart.name }} ({{ currentPart.id }})</p>
        <p><strong>当前库存：</strong>{{ currentPart.quantity }} {{ currentPart.unit }}</p>
      </div>
      <el-form label-width="100px">
        <el-form-item label="入库数量">
          <el-input-number v-model="stockQuantity" :min="1" style="width: 200px"></el-input-number>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="stockInDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmStockIn">确认入库</el-button>
      </span>
    </el-dialog>

    <el-dialog title="出库操作" :visible.sync="stockOutDialogVisible" width="400px">
      <div style="margin-bottom: 15px;">
        <p><strong>零件：</strong>{{ currentPart.name }} ({{ currentPart.id }})</p>
        <p><strong>当前库存：</strong>{{ currentPart.quantity }} {{ currentPart.unit }}</p>
      </div>
      <el-form label-width="100px">
        <el-form-item label="出库数量">
          <el-input-number v-model="stockQuantity" :min="1" :max="currentPart.quantity" style="width: 200px"></el-input-number>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="stockOutDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmStockOut">确认出库</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import partApi from '../../api/partApi'
import { eventBus } from '../../utils/eventBus'

export default {
  name: 'UserPartsSearch',
  data() {
    return {
      loading: false,
      hasSearched: false,
      searchedKeyword: '',
      searchedCategory: '',
      parts: [],
      searchForm: {
        keyword: '',
        category: ''
      },
      stockInDialogVisible: false,
      stockOutDialogVisible: false,
      currentPart: {},
      stockQuantity: 1
    }
  },
  created() {
    this.handleSearch()
  },
  methods: {
    async handleSearch() {
      this.loading = true
      this.hasSearched = true
      this.searchedKeyword = this.searchForm.keyword
      this.searchedCategory = this.searchForm.category
      try {
        const params = {}
        if (this.searchForm.keyword) {
          params.keyword = this.searchForm.keyword
        }
        if (this.searchForm.category) {
          params.category = this.searchForm.category
        }
        const res = await partApi.user.searchVisibleParts(params)
        if (res.success) {
          this.parts = res.data || []
        } else {
          this.$message.error(res.message)
        }
      } catch (error) {
        this.$message.error('搜索失败: ' + error.message)
      } finally {
        this.loading = false
      }
    },

    getSearchTip() {
      let tip = ''
      if (this.searchedCategory) {
        tip += '分类: ' + this.searchedCategory
      }
      if (this.searchedKeyword) {
        if (tip) {
          tip += ', '
        }
        tip += '关键词: ' + this.searchedKeyword
      }
      if (!tip) {
        tip = '全部零件'
      }
      return tip
    },

    handleReset() {
      this.searchForm.keyword = ''
      this.searchForm.category = ''
      this.searchedKeyword = ''
      this.searchedCategory = ''
      this.parts = []
      this.hasSearched = false
      this.handleSearch()
    },

    handleStockIn(row) {
      this.currentPart = { ...row }
      this.stockQuantity = 1
      this.stockInDialogVisible = true
    },

    async confirmStockIn() {
      try {
        const res = await partApi.user.stockIn(this.currentPart.id, this.stockQuantity)
        if (res.success) {
          this.$message.success('入库成功，新增 ' + this.stockQuantity + ' ' + this.currentPart.unit)
          eventBus.$emit('stock-changed')
          this.stockInDialogVisible = false
          this.handleSearch()
        } else {
          this.$message.error(res.message)
        }
      } catch (error) {
        this.$message.error('入库失败: ' + error.message)
      }
    },

    handleStockOut(row) {
      this.currentPart = { ...row }
      this.stockQuantity = 1
      this.stockOutDialogVisible = true
    },

    async confirmStockOut() {
      try {
        const res = await partApi.user.stockOut(this.currentPart.id, this.stockQuantity)
        if (res.success) {
          this.$message.success('出库成功，减少 ' + this.stockQuantity + ' ' + this.currentPart.unit)
          eventBus.$emit('stock-changed')
          this.stockOutDialogVisible = false
          this.handleSearch()
        } else {
          this.$message.error(res.message)
        }
      } catch (error) {
        this.$message.error('出库失败: ' + error.message)
      }
    }
  }
}
</script>

<style scoped>
.search-card {
  margin-bottom: 20px;
}

.search-tip {
  padding: 10px 15px;
  background-color: #ecf5ff;
  border-radius: 4px;
  color: #409EFF;
  font-size: 14px;
}

.highlight-text {
  color: #409EFF;
  font-weight: bold;
}

.danger-text {
  color: #f56c6c;
  font-weight: bold;
}

.dialog-footer {
  text-align: right;
}
</style>
