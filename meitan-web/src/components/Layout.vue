<template>
  <el-container style="height:100vh">
    <el-aside width="300px" style="background:var(--sidebar-bg);overflow:hidden">
      <div style="padding:20px;display:flex;align-items:center;gap:10px">
        <el-icon :size="28" color="#67D1FF"><Monitor /></el-icon>
        <span style="color:#DCEEFF;font-size:17px;font-weight:bold">瓦斯智能分析</span>
      </div>
      <el-menu :default-active="activeMenu" background-color="var(--sidebar-bg)" text-color="var(--sidebar-text)" active-text-color="#fff" router style="border-right:none">
        <el-menu-item index="/home"><el-icon><HomeFilled /></el-icon><span>首页</span></el-menu-item>
        <el-menu-item index="/introduction"><el-icon><InfoFilled /></el-icon><span>瓦斯介绍</span></el-menu-item>
        <el-menu-item-group title="瓦斯分析模块" style="color:var(--sidebar-muted)">
          <el-menu-item index="/analysis"><el-icon><TrendCharts /></el-icon><span>瓦斯吸附含量计算与分析</span></el-menu-item>
          <el-menu-item index="/statistics"><el-icon><DataAnalysis /></el-icon><span>煤样瓦斯吸附参数统计分析</span></el-menu-item>
          <el-menu-item index="/detection"><el-icon><WarningFilled /></el-icon><span>煤层瓦斯突出危险性检测</span></el-menu-item>
        </el-menu-item-group>
        <el-menu-item index="/files"><el-icon><FolderOpened /></el-icon><span>数据文件管理</span></el-menu-item>
        <el-menu-item index="/reports"><el-icon><Document /></el-icon><span>瓦斯数据导出与报告</span></el-menu-item>
        <el-menu-item index="/feedback"><el-icon><ChatLineSquare /></el-icon><span>用户反馈</span></el-menu-item>
        <el-menu-item v-if="isAdmin" index="/admin/users" class="admin-menu-item">
          <el-icon><UserFilled /></el-icon><span>用户管理</span><span class="admin-badge">ADMIN</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/admin/feedback" class="admin-menu-item">
          <el-icon><ChatDotRound /></el-icon><span>反馈处理</span><span class="admin-badge">ADMIN</span>
        </el-menu-item>
        <el-menu-item index="/user-center"><el-icon><UserFilled /></el-icon><span>个人中心</span></el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header height="50px" style="background:var(--card);border-bottom:1px solid var(--border);display:flex;align-items:center;justify-content:space-between;padding:0 20px">
        <span style="color:var(--text-secondary);font-size:14px">{{ route.meta.title }}</span>
        <div style="display:flex;align-items:center;gap:15px">
          <el-tag v-if="isAdmin" type="danger" size="small" effect="plain">管理员</el-tag>
          <span style="color:var(--text-secondary);font-size:13px">{{ userStore.username }}</span>
          <el-button type="danger" size="small" @click="handleLogout" text><el-icon><SwitchButton /></el-icon> 退出</el-button>
        </div>
      </el-header>
      <el-main style="background:var(--bg);padding:20px;overflow-y:auto">
        <router-view v-slot="{ Component }">
          <keep-alive :max="10">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>

    <Teleport to="body"><XiaowaAssistant /></Teleport>
  </el-container>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import XiaowaAssistant from '@/components/XiaowaAssistant.vue'
import { purgeExpiredCalculationHistory } from '@/utils/calculationHistory'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const activeMenu = computed(() => route.path)
const isAdmin = computed(() => userStore.role === 'ADMIN')

onMounted(() => purgeExpiredCalculationHistory())

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-menu-item { margin-top:6px;border-top:1px solid rgba(136,180,216,.22); }
.admin-menu-item + .admin-menu-item { margin-top:0;border-top:0; }
.admin-badge { margin-left:auto;padding:1px 5px;color:#082f59;font-size:8px;font-weight:900;line-height:15px;border-radius:7px;background:#67d1ff; }
</style>
