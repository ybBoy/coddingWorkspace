<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>用户列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增用户
          </el-button>
        </div>
      </template>
      
      <el-table :data="employees" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="departmentName" label="部门" width="120" />
        <el-table-column prop="roleName" label="角色" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'ON_LEAVE' ? 'warning' : 'success'">
              {{ scope.row.status === 'ON_LEAVE' ? '请假中' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="密码" :prop="isEdit ? '' : 'password'">
          <el-input 
            v-model="form.password" 
            type="password" 
            :placeholder="isEdit ? '不修改请留空' : '请输入密码'"
          />
        </el-form-item>
        <el-form-item label="部门" prop="departmentId">
          <el-select v-model="form.departmentId" placeholder="请选择部门" style="width: 100%">
            <el-option
              v-for="dept in departments"
              :key="dept.id"
              :label="dept.name"
              :value="dept.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="role in roles"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getEmployees, 
  getDepartments, 
  getRoles, 
  createEmployee, 
  updateEmployee, 
  deleteEmployee 
} from '../api'

const employees = ref([])
const departments = ref([])
const roles = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const loading = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  username: '',
  name: '',
  password: '',
  departmentId: null,
  roleId: null
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  departmentId: [{ required: true, message: '请选择部门', trigger: 'change' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const loadEmployees = async () => {
  const res = await getEmployees()
  employees.value = res.data || []
}

const loadDepartments = async () => {
  const res = await getDepartments()
  departments.value = res.data || []
}

const loadRoles = async () => {
  const res = await getRoles()
  roles.value = res.data || []
}

const handleAdd = () => {
  isEdit.value = false
  form.id = null
  form.username = ''
  form.name = ''
  form.password = ''
  form.departmentId = null
  form.roleId = null
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  form.id = row.id
  form.username = row.username
  form.name = row.name
  form.password = ''
  form.departmentId = row.departmentId
  form.roleId = row.roleId
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteEmployee(row.id)
    ElMessage.success('删除成功')
    loadEmployees()
  }).catch(() => {})
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const dept = departments.value.find(d => d.id === form.departmentId)
  const role = roles.value.find(r => r.id === form.roleId)
  
  const data = {
    ...form,
    departmentName: dept?.name,
    roleName: role?.name
  }

  loading.value = true
  try {
    if (isEdit.value) {
      if (!data.password) {
        delete data.password
      }
      await updateEmployee(form.id, data)
      ElMessage.success('更新成功')
    } else {
      await createEmployee(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadEmployees()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadEmployees()
  loadDepartments()
  loadRoles()
})
</script>

<style scoped>
.page-container {
  padding: 0;
}
</style>