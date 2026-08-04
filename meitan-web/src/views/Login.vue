<template>
  <div class="login-container">
    <div class="login-card">
      <h1 class="login-title">煤层瓦斯智能分析平台</h1>
      <p class="login-subtitle">煤矿安全 · 科学分析 · 精准预警</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleLogin" style="width:100%">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div style="text-align:center;color:var(--text-secondary);font-size:13px;margin-top:10px">
        还没有账号？<el-link type="primary" @click="showRegister = true">立即注册</el-link>
      </div>

      <!-- 注册弹窗 -->
      <el-dialog v-model="showRegister" title="用户注册" width="420px">
        <el-form :model="regForm" :rules="regRules" ref="regFormRef" label-position="top">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="regForm.username" placeholder="4-20位字母或数字" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="regForm.password" type="password" placeholder="6位以上" show-password />
          </el-form-item>
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="regForm.realName" placeholder="请输入真实姓名" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="regForm.email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="regLoading" @click="handleRegister" style="width:100%">
              注 册
            </el-button>
          </el-form-item>
        </el-form>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { register as registerApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const formRef = ref()
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/home')
  } catch (e) {
    // 拦截器已统一提示，此处不重复弹框
  } finally {
    loading.value = false
  }
}

// 注册
const showRegister = ref(false)
const regLoading = ref(false)
const regFormRef = ref()
const regForm = reactive({ username: '', password: '', realName: '', email: '' })
const regRules = {
  username: [
    { required: true, message: '请输入用户名' },
    { min: 4, max: 20, message: '4-20位字母或数字' },
  ],
  password: [
    { required: true, message: '请输入密码' },
    { min: 6, message: '密码至少6位' },
  ],
  realName: [{ required: true, message: '请输入真实姓名' }],
}

async function handleRegister() {
  const valid = await regFormRef.value.validate().catch(() => false)
  if (!valid) return
  regLoading.value = true
  try {
    await registerApi(regForm)
    ElMessage.success('注册成功，请登录')
    showRegister.value = false
  } catch (e) {
    ElMessage.error(e.message || '注册失败')
  } finally {
    regLoading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary-dark), var(--primary));
}
.login-card {
  width: 420px;
  background: white;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0,0,0,.2);
}
.login-title {
  text-align: center;
  color: var(--primary-dark);
  font-size: 24px;
  margin-bottom: 6px;
}
.login-subtitle {
  text-align: center;
  color: var(--text-secondary);
  font-size: 13px;
  margin-bottom: 30px;
}
</style>
