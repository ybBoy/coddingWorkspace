<template>
  <div class="page-container">
    <div class="page-title">
      <i class="el-icon-warning" style="margin-right: 10px;"></i>需补货列表（管理员）
    </div>
    
    <div class="alert-section" v-if="parts.length > 0">
      <el-alert
        title="库存预警"
        :type="parts.length > 5 ? 'error' : 'warning'"
        :closable="false"
        show-icon
      >
        <template slot="default">
          当前有 <strong style="color: #f56c6c; font-size: 18px;">{{ parts.length }}</strong> 种零件库存低于警戒线，请及时补货！
        </template>
      </el-alert>
    </div>

    <div class="toolbar">
      <el-button type="primary" icon="el-icon-refresh" @click="loadParts" :loading="loading">刷新</el-button>
    </div>

    <el-table
      :data="parts"
      v-loading="loading"
      style="width: 100%"
      class="table-container"
    >
      <el-table-column prop="id" label="零件编号" width="120">
        <template slot-scope="scope">
          <el-tag type="danger">{{ scope.row.id }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="零件名称" width="150">
        <template slot-scope="scope">
          <span class="danger-text">{{ scope.row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="category" label="分类" width="100"></el-table-column>
      <el-table-column prop="specification" label="规格"></el-table-column>
      <el-table-column label="库存状况" width="200">
        <template slot-scope="scope">
          <div class="stock-status">
            <div class="stock-bar">
              <div class="stock-bar-current" :style="{ width: getStockPercentage(scope.row) + '%' }"></div>
              <div class="stock-bar-warning" :style="{ left: (scope.row.minStock / (scope.row.minStock * 3 || 100)) * 100 + '%' }"></div>
            </div>
            <div class="stock-text">
              当前: <span class="danger-text">{{ scope.row.quantity }}</span>
              / 警戒: <span class="warning-text">{{ scope.row.minStock }}</span>
              {{ scope.row.unit }}
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="可见性" width="90">
        <template slot-scope="scope">
          <el-tag :type="scope.row.visible ? 'success' : 'info'" size="small">
            {{ scope.row.visible ? '可见' : '隐藏' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="补货建议" width="120">
        <template slot-scope="scope">
          <span class="suggestion">
            建议补货 <strong>{{ getSuggestedQuantity(scope.row) }}</strong> {{ scope.row.unit }}
          </span>
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

    <el-empty v-if="!loading && parts.length === 0" description="太棒了！所有零件库存正常">
      <el-button type="primary" icon="el-icon-s-grid" @click="$router.push('/admin/parts')">库存管理</el-button>
    </el-empty>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="零件编号" prop="id">
          <el-input v-model="form.id" disabled></el-input>
        </el-form-item>
        <el-form-item label="零件名称" prop="name">
          <el-input v-model="form.name"></el-input>
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" style="width: 100%">
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
          <el-input v-model="form.specification"></el-input>
        </el-form-item>
        <el-form-item label="库存数量" prop="quantity">
          <el-input-number v-model="form.quantity" :min="0" style="width: 100%"></el-input-number>
        </el-form-item>
        <el-form-item label="最低库存" prop="minStock">
          <el-input-number v-model="form.minStock" :min="0" style="width: 100%"></el-input-number>
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit"></el-input>
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
  name: 'AdminRestockList',
  data() {
    return {
      loading: false,
      parts: [],
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
    this.loadParts()
  },
  methods: {
    async loadParts() {
      this.loading = true
      try {
        const res = await partApi.admin.getPartsNeedRestock()
        if (res.success) {
          this.parts = res.data || []
        } else {
          this.$message.error(res.message)
        }
      } catch (error) {
        this.$message.error('加载需补货列表失败: ' + error.message)
      } finally {
        this.loading = false
      }
    },

    getStockPercentage(row) {
      const maxValue = Math.max(row.minStock * 3, row.quantity, 10)
      return Math.min((row.quantity / maxValue) * 100, 100)
    },

    getSuggestedQuantity(row) {
      const suggested = row.minStock * 2 - row.quantity
      return suggested > 0 ? suggested : row.minStock
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
              this.loadParts()
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
.alert-section {
  margin-bottom: 20px;
}

.toolbar {
  margin-bottom: 20px;
}

.danger-text {
  color: #f56c6c;
  font-weight: bold;
}

.warning-text {
  color: #e6a23c;
  font-weight: bold;
}

.stock-status {
  padding: 5px 0;
}

.stock-bar {
  position: relative;
  height: 10px;
  background-color: #ebeef5;
  border-radius: 5px;
  overflow: visible;
  margin-bottom: 8px;
}

.stock-bar-current {
  height: 100%;
  background: linear-gradient(90deg, #f56c6c, #e6a23c);
  border-radius: 5px;
  transition: width 0.3s;
}

.stock-bar-warning {
  position: absolute;
  top: -5px;
  width: 2px;
  height: 20px;
  background-color: #e6a23c;
}

.stock-text {
  font-size: 12px;
  color: #909399;
}

.suggestion {
  font-size: 12px;
  color: #606266;
}

.dialog-footer {
  text-align: right;
}
</style>
