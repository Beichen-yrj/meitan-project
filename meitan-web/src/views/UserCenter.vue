<template>
  <div style="max-width:600px;margin:0 auto">
    <!-- 个人信息 -->
    <el-card>
      <template #header>
        <span style="font-weight:bold;color:var(--primary)">个人信息</span>
      </template>
      <el-form :model="profile" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="profile.username" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="profile.realName" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="profile.email" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="profile.phone" />
        </el-form-item>
        <el-form-item label="角色">
          <el-tag :type="profile.role === 'ADMIN' ? 'danger' : 'primary'">{{ profile.role }}</el-tag>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveProfile" :loading="saving">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 修改密码 -->
    <el-card style="margin-top:20px">
      <template #header>
        <span style="font-weight:bold;color:var(--primary)">修改密码</span>
      </template>
      <el-form :model="pwdForm" label-width="80px">
        <el-form-item label="原密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="changePwd" :loading="changing">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile, changePassword } from '@/api'

const profile = reactive({ username: '', realName: '', email: '', phone: '', role: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '' })
const saving = ref(false)
const changing = ref(false)

onMounted(async () => {
  try {
    const res = await getProfile()
    Object.assign(profile, res.data || {})
  } catch (e) {
    ElMessage.error('加载用户信息失败')
  }
})

async function saveProfile() {
  saving.value = true
  try {
    await updateProfile({ realName: profile.realName, email: profile.email, phone: profile.phone })
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function changePwd() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请填写密码')
    return
  }
  changing.value = true
  try {
    await changePassword(pwdForm)
    ElMessage.success('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '修改失败')
  } finally {
    changing.value = false
  }
}
</script>
