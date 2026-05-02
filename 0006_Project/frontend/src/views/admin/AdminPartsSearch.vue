<template>
  <div class="page-container">
    <div class="page-title">
      <i class="el-icon-search" style="margin-right: 10px;"></i>零件搜索（管理员）
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
      <el-table-column label="可见性" width="90">
        <template slot-scope="scope">
          <el-tag :type="scope.row.visible ? 'success' : 'info'" size="small">
            {{ scope.row.visible ? '可见' : '隐藏' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="text" size="small" @click="toggleVisibility(scope.row)">
            {{ scope.row.visible ? '隐藏' : '显示' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && parts.length === 0 && hasSearched" description="未找到相关零件">
      <el-button type="primary" @click="handleReset">查看全部</el-button>
    </el-empty>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="零件编号" prop="id">
          <el-input v-model="form.id" disabled placeholder="请输入零件编号"></el-input>
        </el-form-item>
        <el-form-item label="零件名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入零件名称"></el-input>
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="电池" value="电池"></el-option>
            <el-option label="电机" value="电机"></el-option>
            <el-option label="轮胎" value="轮胎"></el-option>
            <el-option label="控制器" value="控制器"></el-option>
            <el-option label="玻璃" value="玻璃"></el-option>
            <el-option label="机油" value="机油"></el-option>
            <el-option label="其他" value="其他"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="规格" prop="specification">
          <el-input v-model="form.specification" placeholder="请输入规格"></el-input>
        </el-form-item>
        <el-form-item label="库存数量" prop="quantity">
          <el-input-number v-model="form.quantity" :min="0" style="width: 100%"></el-input-number>
        </el-form-item>
        <el-form-item label="最低库存" prop="minStock">
          <el-input-number v-model="form.minStock" :min="0" style="width: 100%"></el-input-number>
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" placeholder="请输入单位"></el-input>
        </el-form-item>
        <el-form-item label="可见性" prop="visible">
          <el-switch
            v-model="form.visible"
            active-text="对用户可见"
            inactive-text="仅管理员可见"
          >
          </el-switch>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import partApi from '../../api/partApi'
import { eventBus } from '../../utils/eventBus'

export default {
  name: 'AdminPartsSearch',
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
      dialogVisible: false,
      dialogTitle: '',
      form: {
        id: '',
        name: '',
        category: '',
        specification: '',
        quantity: 0,
        minStock: 0,
        unit: '',
        visible: true
      },
      rules: {
        name: [{ required: true, message: '请输入零件名称', trigger: 'blur' }],
        category: [{ required: true, message: '请选择分类', trigger: 'change' }],
        unit: [{ required: true, message: '请输入单位', trigger: 'blur' }]
      }
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
        const res = await partApi.admin.searchParts(params)
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

    handleEdit(row) {
      this.dialogTitle = '编辑零件'
      this.form = { ...row }
      this.dialogVisible = true
    },

    async toggleVisibility(row) {
      const newVisible = !row.visible
      try {
        const res = await partApi.admin.updateVisibility(row.id, newVisible)
        if (res.success) {
          row.visible = newVisible
          this.$message.success(newVisible ? '已设置为对用户可见' : '已设置为仅管理员可见')
          eventBus.$emit('visibility-changed')
        } else {
          this.$message.error(res.message)
        }
      } catch (error) {
        this.$message.error('更新可见性失败: ' + error.message)
      }
    },

    async handleSubmit() {
      this.$refs.formRef.validate(async (valid) => {
        if (valid) {
          try {
            const res = await partApi.admin.updatePart(this.form.id, this.form)
            if (res.success) {
              this.$message.success('更新成功')
              eventBus.$emit('stock-changed')
              this.dialogVisible = false
              this.handleSearch()
            } else {
              this.$message.error(res.message)
            }
          } catch (error) {
            this.$message.error('操作失败: ' + error.message)
          }
        }
      })
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
