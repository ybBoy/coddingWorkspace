<template>
  <div class="page-container">
    <div class="page-title">
      <i class="el-icon-s-grid" style="margin-right: 10px;"></i>库存列表
    </div>
    
    <div class="toolbar">
      <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新增零件</el-button>
      <el-button type="primary" icon="el-icon-refresh" @click="loadParts" :loading="loading">刷新</el-button>
    </div>

    <el-table :data="parts" v-loading="loading" style="width: 100%" class="table-container">
      <el-table-column prop="id" label="零件编号" width="120" sortable></el-table-column>
      <el-table-column prop="name" label="零件名称" width="150"></el-table-column>
      <el-table-column prop="category" label="分类" width="100"></el-table-column>
      <el-table-column prop="specification" label="规格"></el-table-column>
      <el-table-column prop="quantity" label="库存数量" width="120" sortable>
        <template slot-scope="scope">
          <span :class="{'danger-text': scope.row.quantity <= scope.row.minStock}">
            {{ scope.row.quantity }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="minStock" label="最低库存" width="100"></el-table-column>
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
          <el-button type="text" size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="text" size="small" @click="handleDelete(scope.row)">删除</el-button>
          <el-button type="text" size="small" @click="handleStockIn(scope.row)">入库</el-button>
          <el-button type="text" size="small" @click="handleStockOut(scope.row)">出库</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="零件编号" prop="id">
          <el-input v-model="form.id" :disabled="isEdit" placeholder="请输入零件编号"></el-input>
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
          <el-input v-model="form.unit" placeholder="请输入单位，如：块、台、条等"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </span>
    </el-dialog>

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
import partApi from '../api/partApi'

export default {
  name: 'PartsList',
  data() {
    return {
      loading: false,
      parts: [],
      dialogVisible: false,
      isEdit: false,
      dialogTitle: '',
      form: {
        id: '',
        name: '',
        category: '',
        specification: '',
        quantity: 0,
        minStock: 0,
        unit: ''
      },
      rules: {
        id: [{ required: true, message: '请输入零件编号', trigger: 'blur' }],
        name: [{ required: true, message: '请输入零件名称', trigger: 'blur' }],
        category: [{ required: true, message: '请选择分类', trigger: 'change' }],
        unit: [{ required: true, message: '请输入单位', trigger: 'blur' }]
      },
      stockInDialogVisible: false,
      stockOutDialogVisible: false,
      currentPart: {},
      stockQuantity: 1
    }
  },
  created() {
    this.loadParts()
  },
  methods: {
    async loadParts() {
      this.loading = true
      try {
        const res = await partApi.getAllParts()
        if (res.success) {
          this.parts = res.data || []
        } else {
          this.$message.error(res.message)
        }
      } catch (error) {
        this.$message.error('加载零件列表失败: ' + error.message)
      } finally {
        this.loading = false
      }
    },

    handleAdd() {
      this.isEdit = false
      this.dialogTitle = '新增零件'
      this.form = {
        id: '',
        name: '',
        category: '',
        specification: '',
        quantity: 0,
        minStock: 0,
        unit: ''
      }
      this.dialogVisible = true
    },

    handleEdit(row) {
      this.isEdit = true
      this.dialogTitle = '编辑零件'
      this.form = { ...row }
      this.dialogVisible = true
    },

    async handleDelete(row) {
      try {
        await this.$confirm(`确定要删除零件「${row.name}」吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const res = await partApi.deletePart(row.id)
        if (res.success) {
          this.$message.success('删除成功')
          this.loadParts()
        } else {
          this.$message.error(res.message)
        }
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('删除失败: ' + (error.message || error))
        }
      }
    },

    async handleSubmit() {
      this.$refs.formRef.validate(async (valid) => {
        if (valid) {
          try {
            let res
            if (this.isEdit) {
              res = await partApi.updatePart(this.form.id, this.form)
            } else {
              res = await partApi.addPart(this.form)
            }
            
            if (res.success) {
              this.$message.success(this.isEdit ? '更新成功' : '添加成功')
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
    },

    handleStockIn(row) {
      this.currentPart = { ...row }
      this.stockQuantity = 1
      this.stockInDialogVisible = true
    },

    async confirmStockIn() {
      try {
        const res = await partApi.stockIn(this.currentPart.id, this.stockQuantity)
        if (res.success) {
          this.$message.success(`入库成功，新增 ${this.stockQuantity} ${this.currentPart.unit}`)
          this.stockInDialogVisible = false
          this.loadParts()
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
        const res = await partApi.stockOut(this.currentPart.id, this.stockQuantity)
        if (res.success) {
          this.$message.success(`出库成功，减少 ${this.stockQuantity} ${this.currentPart.unit}`)
          this.stockOutDialogVisible = false
          this.loadParts()
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
.toolbar {
  margin-bottom: 20px;
}

.danger-text {
  color: #f56c6c;
  font-weight: bold;
}

.dialog-footer {
  text-align: right;
}
</style>
