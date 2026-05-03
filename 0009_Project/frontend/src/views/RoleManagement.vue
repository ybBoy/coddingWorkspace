<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>角色列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增角色
          </el-button>
        </div>
      </template>
      
      <el-table :data="roles" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="角色名称" width="180" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入角色描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoles, createRole, updateRole, deleteRole } from '../api'

const roles = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const loading = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  name: '',
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

const loadRoles = async () => {
  const res = await getRoles()
  roles.value = res.data || []
}

const handleAdd = () => {
  isEdit.value = false
  form.id = null
  form.name = ''
  form.description = ''
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  form.id = row.id
  form.name = row.name
  form.description = row.description
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该角色吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteRole(row.id)
    ElMessage.success('删除成功')
    loadRoles()
  }).catch(() => {})
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    if (isEdit.value) {
      await updateRole(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await createRole(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadRoles()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadRoles()
})
</script>

<style scoped>
.page-container {
  padding: 0;
}
</style>