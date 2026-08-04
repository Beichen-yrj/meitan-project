<template>
  <div class="admin-users-page">
    <section class="page-heading">
      <div>
        <p class="page-heading__eyebrow">ADMIN CONSOLE</p>
        <h2>用户管理</h2>
        <p>管理平台账号状态与黑名单，查看用户最近登录时间和安全记录。</p>
      </div>
      <el-button type="primary" :loading="loading" @click="refreshAll">
        <el-icon><Refresh /></el-icon>刷新数据
      </el-button>
    </section>

    <el-row :gutter="16" class="statistics-row">
      <el-col v-for="item in statisticCards" :key="item.key" :xs="12" :sm="12" :md="6">
        <el-card shadow="never" class="statistic-card" :class="`is-${item.tone}`">
          <div class="statistic-card__icon"><el-icon><component :is="item.icon" /></el-icon></div>
          <div>
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.caption }}</small>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="manager-card">
      <div class="filter-bar">
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="搜索用户名、姓名、邮箱或手机号"
          class="filter-keyword"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="filters.status" clearable placeholder="账号状态" @change="handleSearch">
          <el-option label="正常启用" :value="1" />
          <el-option label="已停用" :value="0" />
        </el-select>
        <el-select v-model="filters.blacklisted" clearable placeholder="黑名单状态" @change="handleSearch">
          <el-option label="黑名单用户" :value="true" />
          <el-option label="非黑名单用户" :value="false" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="users" stripe class="user-table">
        <el-table-column label="用户" min-width="155" fixed="left">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="38" :class="{ 'is-admin': row.role === 'ADMIN' }">
                {{ (row.realName || row.username || '用').slice(0, 1) }}
              </el-avatar>
              <div>
                <strong>{{ row.username }}</strong>
                <span>{{ row.realName || '未填写姓名' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="联系方式" min-width="180">
          <template #default="{ row }">
            <div class="contact-cell">
              <span>{{ row.phone || '未填写手机号' }}</span>
              <small>{{ row.email || '未填写邮箱' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'" effect="light">
              {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号状态" width="105" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="dark">
              {{ row.status === 1 ? '已启用' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="安全状态" min-width="145">
          <template #default="{ row }">
            <el-tooltip v-if="row.blacklisted === 1" :content="row.blacklistReason || '管理员加入黑名单'" placement="top">
              <el-tag type="danger" effect="dark"><el-icon><Lock /></el-icon> 黑名单</el-tag>
            </el-tooltip>
            <el-tag v-else type="success" effect="plain"><el-icon><CircleCheck /></el-icon> 正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" min-width="175">
          <template #default="{ row }">
            <div class="login-cell">
              <span>{{ formatDate(row.lastLoginTime) }}</span>
              <small>IP：{{ row.lastLoginIp || '暂无记录' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="loginCount" label="登录次数" width="95" align="center">
          <template #default="{ row }">{{ row.loginCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="注册时间" min-width="165">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="管理操作" width="245" fixed="right">
          <template #default="{ row }">
            <div class="action-cell">
              <el-button link type="primary" @click="openLoginLogs(row)">登录记录</el-button>
              <template v-if="row.role !== 'ADMIN'">
                <el-button
                  link
                  :type="row.status === 1 ? 'warning' : 'success'"
                  @click="toggleStatus(row)"
                >
                  {{ row.status === 1 ? '停用' : '启用' }}
                </el-button>
                <el-button v-if="row.blacklisted !== 1" link type="danger" @click="openBlacklist(row)">
                  加入黑名单
                </el-button>
                <el-button v-else link type="success" @click="removeBlacklist(row)">解除黑名单</el-button>
              </template>
              <el-tag v-else size="small" type="info">受保护</el-tag>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="没有符合条件的用户" />
        </template>
      </el-table>

      <div class="pagination-bar">
        <span>共 {{ total }} 个用户</span>
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="sizes, prev, pager, next, jumper"
          @size-change="loadUsers"
          @current-change="loadUsers"
        />
      </div>
    </el-card>

    <el-dialog v-model="blacklistDialog.visible" title="设置用户黑名单" width="500px" destroy-on-close>
      <el-alert type="warning" :closable="false" show-icon>
        <template #title>加入黑名单后，该用户会立即退出登录状态并无法再次登录。</template>
      </el-alert>
      <div class="dialog-user-summary">
        <span>目标用户</span>
        <strong>{{ blacklistDialog.user?.username }}</strong>
        <small>{{ blacklistDialog.user?.realName || '未填写姓名' }}</small>
      </div>
      <el-input
        v-model="blacklistDialog.reason"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        placeholder="请输入加入黑名单的原因，便于后续核查"
      />
      <template #footer>
        <el-button @click="blacklistDialog.visible = false">取消</el-button>
        <el-button type="danger" :loading="blacklistDialog.submitting" @click="confirmBlacklist">确认加入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="logsDialog.visible" :title="`${logsDialog.user?.username || ''} 的登录记录`" width="920px">
      <div class="logs-summary">
        <span>最近登录：<strong>{{ formatDate(logsDialog.user?.lastLoginTime) }}</strong></span>
        <span>最近 IP：<strong>{{ logsDialog.user?.lastLoginIp || '暂无记录' }}</strong></span>
        <span>成功登录：<strong>{{ logsDialog.user?.loginCount || 0 }} 次</strong></span>
      </div>
      <el-table v-loading="logsDialog.loading" :data="logsDialog.records" stripe max-height="440">
        <el-table-column label="登录时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.loginTime) }}</template>
        </el-table-column>
        <el-table-column prop="loginIp" label="登录 IP" min-width="135">
          <template #default="{ row }">{{ row.loginIp || '未知' }}</template>
        </el-table-column>
        <el-table-column label="结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'" effect="dark">
              {{ row.success === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="150">
          <template #default="{ row }">{{ row.failureReason || '正常登录' }}</template>
        </el-table-column>
        <el-table-column label="浏览器 / 设备" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ row.userAgent || '未知设备' }}</template>
        </el-table-column>
        <template #empty><el-empty description="暂无登录记录" /></template>
      </el-table>
      <div class="logs-pagination">
        <el-pagination
          v-model:current-page="logsDialog.page"
          :page-size="logsDialog.pageSize"
          :total="logsDialog.total"
          layout="total, prev, pager, next"
          @current-change="loadLoginLogs"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addUserToBlacklist,
  getAdminUserStatistics,
  getAdminUsers,
  getUserLoginLogs,
  removeUserFromBlacklist,
  updateAdminUserStatus,
} from '@/api'

const loading = ref(false)
const users = ref([])
const total = ref(0)
const statistics = reactive({
  totalUsers: 0,
  enabledUsers: 0,
  disabledUsers: 0,
  blacklistedUsers: 0,
  todayLoginCount: 0,
  todayFailedCount: 0,
})
const filters = reactive({ keyword: '', status: '', blacklisted: '' })
const pagination = reactive({ page: 1, pageSize: 10 })
const blacklistDialog = reactive({ visible: false, submitting: false, user: null, reason: '' })
const logsDialog = reactive({
  visible: false,
  loading: false,
  user: null,
  records: [],
  total: 0,
  page: 1,
  pageSize: 10,
})

const statisticCards = computed(() => [
  { key: 'total', label: '用户总数', value: statistics.totalUsers, caption: '平台全部账号', icon: 'User', tone: 'blue' },
  { key: 'enabled', label: '启用用户', value: statistics.enabledUsers, caption: `停用 ${statistics.disabledUsers} 个`, icon: 'CircleCheckFilled', tone: 'green' },
  { key: 'blacklist', label: '黑名单', value: statistics.blacklistedUsers, caption: '当前受限账号', icon: 'Lock', tone: 'red' },
  { key: 'today', label: '今日登录', value: statistics.todayLoginCount, caption: `失败 ${statistics.todayFailedCount} 次`, icon: 'Clock', tone: 'orange' },
])

function buildQuery() {
  const query = { page: pagination.page, pageSize: pagination.pageSize }
  if (filters.keyword.trim()) query.keyword = filters.keyword.trim()
  if (filters.status !== '') query.status = filters.status
  if (filters.blacklisted !== '') query.blacklisted = filters.blacklisted
  return query
}

async function loadUsers() {
  loading.value = true
  try {
    const response = await getAdminUsers(buildQuery())
    users.value = response.data?.records || []
    total.value = response.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStatistics() {
  const response = await getAdminUserStatistics()
  Object.assign(statistics, response.data || {})
}

async function refreshAll() {
  await Promise.all([loadUsers(), loadStatistics()])
}

function handleSearch() {
  pagination.page = 1
  loadUsers()
}

function resetFilters() {
  filters.keyword = ''
  filters.status = ''
  filters.blacklisted = ''
  handleSearch()
}

async function toggleStatus(user) {
  const nextStatus = user.status === 1 ? 0 : 1
  const action = nextStatus === 1 ? '启用' : '停用'
  await ElMessageBox.confirm(
    `确定要${action}用户“${user.username}”吗？${nextStatus === 0 ? '停用后其现有登录状态会立即失效。' : ''}`,
    `${action}账号`,
    { type: nextStatus === 1 ? 'success' : 'warning', confirmButtonText: `确认${action}`, cancelButtonText: '取消' }
  )
  await updateAdminUserStatus(user.id, { status: nextStatus })
  ElMessage.success(`账号已${action}`)
  await refreshAll()
}

function openBlacklist(user) {
  blacklistDialog.user = user
  blacklistDialog.reason = ''
  blacklistDialog.visible = true
}

async function confirmBlacklist() {
  if (!blacklistDialog.reason.trim()) {
    ElMessage.warning('请填写加入黑名单的原因')
    return
  }
  blacklistDialog.submitting = true
  try {
    await addUserToBlacklist(blacklistDialog.user.id, { reason: blacklistDialog.reason.trim() })
    blacklistDialog.visible = false
    ElMessage.success('该用户已加入黑名单')
    await refreshAll()
  } finally {
    blacklistDialog.submitting = false
  }
}

async function removeBlacklist(user) {
  await ElMessageBox.confirm(
    `确定将用户“${user.username}”移出黑名单吗？移出后仍需账号处于启用状态才能登录。`,
    '解除黑名单',
    { type: 'warning', confirmButtonText: '确认解除', cancelButtonText: '取消' }
  )
  await removeUserFromBlacklist(user.id)
  ElMessage.success('已解除黑名单')
  await refreshAll()
}

async function openLoginLogs(user) {
  logsDialog.user = user
  logsDialog.page = 1
  logsDialog.visible = true
  await loadLoginLogs()
}

async function loadLoginLogs() {
  if (!logsDialog.user) return
  logsDialog.loading = true
  try {
    const response = await getUserLoginLogs(logsDialog.user.id, {
      page: logsDialog.page,
      pageSize: logsDialog.pageSize,
    })
    logsDialog.records = response.data?.records || []
    logsDialog.total = response.data?.total || 0
  } finally {
    logsDialog.loading = false
  }
}

function formatDate(value) {
  if (!value) return '暂无记录'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value).replace('T', ' ') : date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(refreshAll)
</script>

<style scoped>
.admin-users-page { color: var(--text-primary); }
.page-heading { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 18px; padding: 20px 24px; color: #fff; border-radius: 12px; background: linear-gradient(125deg, #0b3f73, #1769aa 66%, #2d9bd8); box-shadow: 0 10px 28px rgba(10, 74, 125, .2); }
.page-heading h2 { margin: 2px 0 7px; font-size: 25px; }
.page-heading p { margin: 0; color: #d8edfc; font-size: 13px; }
.page-heading__eyebrow { color: #79d7ff !important; font-size: 10px !important; font-weight: 800; letter-spacing: 2px; }
.statistics-row { margin-bottom: 16px; }
.statistics-row .el-col { margin-bottom: 12px; }
.statistic-card { position: relative; overflow: hidden; height: 116px; border: 0 !important; }
.statistic-card :deep(.el-card__body) { display: flex; align-items: center; gap: 16px; height: 100%; padding: 19px; }
.statistic-card__icon { display: grid; place-items: center; flex: 0 0 50px; height: 50px; color: #fff; font-size: 24px; border-radius: 14px; background: var(--card-tone); box-shadow: 0 7px 16px color-mix(in srgb, var(--card-tone) 32%, transparent); }
.statistic-card span, .statistic-card strong, .statistic-card small { display: block; }
.statistic-card span { color: #657c8d; font-size: 13px; }
.statistic-card strong { margin: 3px 0; color: #173a53; font-size: 28px; line-height: 1; }
.statistic-card small { color: #91a2ae; font-size: 11px; }
.statistic-card.is-blue { --card-tone: #1a73e8; }
.statistic-card.is-green { --card-tone: #27ae60; }
.statistic-card.is-red { --card-tone: #e34d59; }
.statistic-card.is-orange { --card-tone: #f39c12; }
.manager-card { border-color: #b6daf2 !important; }
.filter-bar { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; margin-bottom: 16px; }
.filter-keyword { width: min(360px, 100%); }
.filter-bar .el-select { width: 150px; }
.user-table { width: 100%; --el-table-header-bg-color: #edf7fe; --el-table-header-text-color: #174c73; }
.user-cell { display: flex; align-items: center; gap: 10px; }
.user-cell .el-avatar { color: #fff; font-weight: 800; background: linear-gradient(135deg, #3f9de5, #1769aa); }
.user-cell .el-avatar.is-admin { background: linear-gradient(135deg, #f06b74, #c62828); }
.user-cell strong, .user-cell span, .contact-cell span, .contact-cell small, .login-cell span, .login-cell small { display: block; }
.user-cell strong { color: #193f5b; }
.user-cell span, .contact-cell small, .login-cell small { margin-top: 3px; color: #8194a2; font-size: 11px; }
.contact-cell span, .login-cell span { color: #3d5f76; font-size: 12px; }
.action-cell { display: flex; align-items: center; flex-wrap: wrap; gap: 0; }
.action-cell .el-button + .el-button { margin-left: 10px; }
.pagination-bar { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-top: 17px; color: #6b8394; font-size: 12px; }
.dialog-user-summary { display: grid; grid-template-columns: 80px 1fr auto; align-items: center; gap: 8px; margin: 15px 0; padding: 12px 14px; color: #647e90; border-radius: 7px; background: #eef7fd; }
.dialog-user-summary strong { color: #174c73; }
.dialog-user-summary small { color: #8799a5; }
.logs-summary { display: flex; flex-wrap: wrap; gap: 12px 28px; margin-bottom: 14px; padding: 12px 15px; color: #607b8e; font-size: 12px; border: 1px solid #c6e3f5; border-radius: 8px; background: #f2f9fe; }
.logs-summary strong { color: #174c73; }
.logs-pagination { display: flex; justify-content: flex-end; margin-top: 15px; }
@media (max-width: 760px) {
  .page-heading { align-items: flex-start; flex-direction: column; }
  .filter-bar > .el-select { width: calc(50% - 5px); }
  .pagination-bar { align-items: flex-start; flex-direction: column; }
}
</style>
