<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <el-icon :size="48" style="color: #409EFF"><OfficeBuilding /></el-icon>
        <h1>公司OA管理系统</h1>
      </div>
      <el-form :model="loginForm" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-tips">
        <p>测试账号：</p>
        <p>管理员：admin / 123456</p>
        <p>普通员工：zhangsan / 123456</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login(loginForm)
    if (res.code === 200) {
      ElMessage.success('登录成功')
      localStorage.setItem('user', JSON.stringify(res.data))
      router.push('/')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h1 {
  margin-top: 15px;
  font-size: 24px;
  color: #333;
  font-weight: 500;
}

.login-tips {
  margin-top: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 5px;
  font-size: 12px;
  color: #909399;
}

.login-tips p {
  margin: 5px 0;
}
</style>