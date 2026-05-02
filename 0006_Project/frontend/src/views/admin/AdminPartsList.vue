<template>
  <div class="page-container">
    <div class="page-title">
      <i class="el-icon-s-grid" style="margin-right: 10px;"></i>库存管理
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
      <el-table-column prop="quantity" label="库存数量" width="100" sortable>
        <template slot-scope="scope">
          <span :class="{'danger-text': scope.row.quantity <= scope.row.minStock}">
            {{ scope.row.quantity }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="minStock" label="最低库存" width="80"></el-table-column>
      <el-table-column prop="unit" label="单位" width="60"></el-table-column>
      <el-table-column label="状态" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.quantity <= scope.row.minStock ? 'danger' : 'success'">
            {{ scope.row.quantity <= scope.row.minStock ? '需补货' : '正常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="可见性" width="70" align="center">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.visible"
            @change="handleVisibilityChange(scope.row)"
            :active-value="true"
            :inactive-value="false"
          >
          </el-switch>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="text" size="small" @click="handleDelete(scope.row)">删除</el-button>
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
        <el-form-item label="可见性" prop="visible">
          <el-switch
            v-model="form.visible"
            active-text="对用户可见"
            inactive-text="仅管理员可见"
            :active-value="true"
            :inactive-value="false"
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
  name: 'AdminPartsList',
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
        unit: '',
        visible: true
      },
      rules: {
        id: [{ required: true, message: '请输入零件编号', trigger: 'blur' }],
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
        const res = await partApi.admin.getAllParts()
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
        unit: '',
        visible: true
      }
      this.dialogVisible = true
    },

    handleEdit(row) {
      this.isEdit = true
      this.dialogTitle = '编辑零件'
      this.form = { ...row }
      this.dialogVisible = true
    },

    async handleVisibilityChange(row) {
      try {
        const res = await partApi.admin.updateVisibility(row.id, row.visible)
        if (res.success) {
          this.$message.success(row.visible ? '已设置为对用户可见' : '已设置为仅管理员可见')
          eventBus.$emit('visibility-changed')
        } else {
          this.$message.error(res.message)
          row.visible = !row.visible
        }
      } catch (error) {
        this.$message.error('更新可见性失败: ' + error.message)
        row.visible = !row.visible
      }
    },

    async handleDelete(row) {
      try {
        await this.$confirm('确定要删除零件「' + row.name + '」吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const res = await partApi.admin.deletePart(row.id)
        if (res.success) {
          this.$message.success('删除成功')
          eventBus.$emit('stock-changed')
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
              res = await partApi.admin.updatePart(this.form.id, this.form)
            } else {
              res = await partApi.admin.addPart(this.form)
            }
            
            if (res.success) {
              this.$message.success(this.isEdit ? '更新成功' : '添加成功')
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
