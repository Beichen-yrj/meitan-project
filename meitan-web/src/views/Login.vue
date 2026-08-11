<template>
  <div class="login-container" :style="{ backgroundImage: `url(${loginBackground})` }">
    <div class="login-layout">
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
    <el-card class="login-news-card">
      <template #header>
        <span class="login-news-title">行业新闻资讯</span>
      </template>
      <el-timeline v-if="newsList.length">
        <el-timeline-item
          v-for="news in newsList"
          :key="news.id"
          :timestamp="news.source"
          placement="top"
        >
          <el-link :href="news.url" target="_blank" type="primary" :underline="false">
            {{ news.title }}
          </el-link>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无新闻资讯" :image-size="72" />
    </el-card>
  </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getNews, register as registerApi } from '@/api'
import { ElMessage } from 'element-plus'
import loginBackground from '@/assets/images/7.jpg'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const formRef = ref()
const form = reactive({ username: '', password: '' })
const newsList = ref([])
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

onMounted(async () => {
  try {
    const res = await getNews()
    newsList.value = res.data || []
  } catch (e) {
    newsList.value = [
      { id: 1, title: '国家矿山安全监察局：加强煤矿瓦斯防治工作', source: '政策法规', url: '#' },
      { id: 2, title: '智能化瓦斯监测预警系统在多个矿区推广应用', source: '科技前沿', url: '#' },
    ]
  }
})

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
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  background-color: var(--bg);
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
}
.login-layout {
  width: min(1120px, 100%);
  display: grid;
  grid-template-columns: minmax(360px, 420px) minmax(0, 1fr);
  gap: 40px;
  align-items: stretch;
}
.login-card {
  width: 100%;
  background: rgba(255, 255, 255, 0.94);
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(7, 57, 104, 0.24);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
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
.login-news-card {
  --el-card-bg-color: rgba(255, 255, 255, 0.90);
  display: flex;
  flex-direction: column;
  min-width: 0;
  background-color: rgba(255, 255, 255, 0.90);
  border-color: rgba(255, 255, 255, 0.72) !important;
  box-shadow: 0 20px 60px rgba(7, 57, 104, 0.24);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
}
.login-news-title {
  color: #0b3f73;
  font-size: 20px;
  font-weight: 700;
}
.login-news-card :deep(.el-card__header) {
  padding: 20px 24px;
  border-bottom-color: rgba(144, 202, 249, 0.72);
}
.login-news-card :deep(.el-card__body) {
  flex: 1;
  padding: 24px 30px 12px;
  overflow-y: auto;
}
.login-news-card :deep(.el-timeline) {
  padding-left: 8px;
}
.login-news-card :deep(.el-timeline-item) {
  padding-bottom: 22px;
}
.login-news-card :deep(.el-timeline-item__timestamp) {
  margin-bottom: 6px;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}
.login-news-card :deep(.el-link) {
  --el-link-text-color: #0b3f73;
  --el-link-hover-text-color: #f39c12;
  color: #0b3f73;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.55;
}
@media (max-width: 900px) {
  .login-container {
    align-items: flex-start;
    padding: 24px 16px;
  }
  .login-layout {
    grid-template-columns: minmax(0, 520px);
    justify-content: center;
    gap: 20px;
  }
}
@media (max-width: 480px) {
  .login-card {
    padding: 30px 22px;
  }
}
</style>
