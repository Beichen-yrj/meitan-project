<template>
  <div class="admin-feedback-page">
    <section class="feedback-heading">
      <div>
        <p>ADMIN FEEDBACK CENTER</p>
        <h2>用户反馈处理</h2>
        <span>集中查看用户意见、评分和处理进度，并将回复反馈给提交用户。</span>
      </div>
      <el-button type="primary" :loading="loading" @click="refreshAll">
        <el-icon><Refresh /></el-icon>刷新数据
      </el-button>
    </section>

    <section class="feedback-statistics">
      <div v-for="item in statisticCards" :key="item.label" class="feedback-stat-card" :class="`is-${item.tone}`">
        <el-icon><component :is="item.icon" /></el-icon>
        <div><span>{{ item.label }}</span><strong>{{ item.value }}</strong><small>{{ item.caption }}</small></div>
      </div>
    </section>

    <el-card shadow="never" class="feedback-table-card">
      <div class="feedback-filters">
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="搜索用户名、姓名或反馈内容"
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="filters.status" clearable placeholder="处理状态" @change="search">
          <el-option label="待处理" value="PENDING" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已解决" value="RESOLVED" />
        </el-select>
        <el-select v-model="filters.rating" clearable placeholder="用户评分" @change="search">
          <el-option v-for="value in [5, 4, 3, 2, 1]" :key="value" :label="`${value} 星`" :value="value" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="feedbacks" stripe border>
        <el-table-column label="提交用户" min-width="145">
          <template #default="{ row }">
            <div class="feedback-user-cell">
              <el-avatar :size="34">{{ (row.realName || row.username || '用').slice(0, 1) }}</el-avatar>
              <div><strong>{{ row.username }}</strong><span>{{ row.realName || '未填写姓名' }}</span></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="145">
          <template #default="{ row }"><el-rate :model-value="row.rating" disabled /></template>
        </el-table-column>
        <el-table-column prop="content" label="反馈内容" min-width="290" show-overflow-tooltip />
        <el-table-column label="提交时间" min-width="165">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="处理状态" width="105" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="dark">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="管理员回复" min-width="230" show-overflow-tooltip>
          <template #default="{ row }">{{ row.adminReply || '暂无回复' }}</template>
        </el-table-column>
        <el-table-column label="处理时间" min-width="165">
          <template #default="{ row }">{{ formatDate(row.handledTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="105" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openHandleDialog(row)">
              {{ row.status === 'PENDING' ? '开始处理' : '查看处理' }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无用户反馈" /></template>
      </el-table>

      <div class="feedback-pagination">
        <span>共 {{ total }} 条反馈</span>
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="sizes, prev, pager, next, jumper"
          @size-change="loadFeedbacks"
          @current-change="loadFeedbacks"
        />
      </div>
    </el-card>

    <el-dialog v-model="handleDialog.visible" title="处理用户反馈" width="620px" destroy-on-close>
      <div v-if="handleDialog.feedback" class="feedback-dialog-content">
        <div class="feedback-original">
          <div><span>提交用户</span><strong>{{ handleDialog.feedback.username }} · {{ handleDialog.feedback.realName || '未填写姓名' }}</strong></div>
          <div><span>用户评分</span><el-rate :model-value="handleDialog.feedback.rating" disabled /></div>
          <p>{{ handleDialog.feedback.content }}</p>
        </div>
        <el-form label-width="90px" label-position="top">
          <el-form-item label="处理状态">
            <el-radio-group v-model="handleDialog.status">
              <el-radio-button value="PENDING">待处理</el-radio-button>
              <el-radio-button value="PROCESSING">处理中</el-radio-button>
              <el-radio-button value="RESOLVED">已解决</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="回复用户">
            <el-input
              v-model="handleDialog.reply"
              type="textarea"
              :rows="5"
              maxlength="2000"
              show-word-limit
              placeholder="填写处理结果、使用指导或后续说明，用户可在反馈页面查看"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="handleDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="handleDialog.submitting" @click="saveHandleResult">保存处理结果</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminFeedback, getAdminFeedbackStatistics, handleAdminFeedback } from '@/api'

const loading = ref(false)
const feedbacks = ref([])
const total = ref(0)
const filters = reactive({ keyword: '', status: '', rating: '' })
const pagination = reactive({ page: 1, pageSize: 10 })
const statistics = reactive({ total: 0, pending: 0, processing: 0, resolved: 0, averageRating: 0 })
const handleDialog = reactive({ visible: false, submitting: false, feedback: null, status: 'PROCESSING', reply: '' })

const statisticCards = computed(() => [
  { label: '全部反馈', value: statistics.total, caption: '累计提交数量', icon: 'ChatDotRound', tone: 'blue' },
  { label: '待处理', value: statistics.pending, caption: '需要管理员跟进', icon: 'Bell', tone: 'red' },
  { label: '处理中', value: statistics.processing, caption: '正在跟进处理', icon: 'Loading', tone: 'orange' },
  { label: '已解决', value: statistics.resolved, caption: '已完成反馈', icon: 'CircleCheckFilled', tone: 'green' },
  { label: '平均评分', value: Number(statistics.averageRating || 0).toFixed(1), caption: '满分 5.0', icon: 'StarFilled', tone: 'purple' },
])

function queryParams() {
  const params = { page: pagination.page, pageSize: pagination.pageSize }
  if (filters.keyword.trim()) params.keyword = filters.keyword.trim()
  if (filters.status) params.status = filters.status
  if (filters.rating !== '') params.rating = filters.rating
  return params
}

async function loadFeedbacks() {
  loading.value = true
  try {
    const response = await getAdminFeedback(queryParams())
    feedbacks.value = response.data?.records || []
    total.value = response.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStatistics() {
  const response = await getAdminFeedbackStatistics()
  Object.assign(statistics, response.data || {})
}

async function refreshAll() {
  await Promise.all([loadFeedbacks(), loadStatistics()])
}

function search() {
  pagination.page = 1
  loadFeedbacks()
}

function resetFilters() {
  Object.assign(filters, { keyword: '', status: '', rating: '' })
  search()
}

function openHandleDialog(feedback) {
  handleDialog.feedback = feedback
  handleDialog.status = feedback.status === 'PENDING' ? 'PROCESSING' : feedback.status
  handleDialog.reply = feedback.adminReply || ''
  handleDialog.visible = true
}

async function saveHandleResult() {
  if (!handleDialog.feedback) return
  if (handleDialog.status === 'RESOLVED' && !handleDialog.reply.trim()) {
    ElMessage.warning('标记为已解决时，请填写处理结果')
    return
  }
  handleDialog.submitting = true
  try {
    await handleAdminFeedback(handleDialog.feedback.id, {
      status: handleDialog.status,
      adminReply: handleDialog.reply.trim(),
    })
    handleDialog.visible = false
    ElMessage.success('反馈处理结果已保存')
    await refreshAll()
  } finally {
    handleDialog.submitting = false
  }
}

const statusLabel = (status) => ({ PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已解决' }[status] || status)
const statusType = (status) => ({ PENDING: 'danger', PROCESSING: 'warning', RESOLVED: 'success' }[status] || 'info')
function formatDate(value) {
  if (!value) return '暂无'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value).replace('T', ' ') : date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(refreshAll)
</script>

<style scoped>
.admin-feedback-page { color: var(--text-primary); }
.feedback-heading { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 16px; padding: 20px 24px; color: #fff; border-radius: 12px; background: linear-gradient(125deg,#0b3f73,#1769aa 65%,#2997d6); box-shadow: 0 10px 26px rgba(11,63,115,.18); }
.feedback-heading p { margin: 0; color: #79d7ff; font-size: 10px; font-weight: 800; letter-spacing: 2px; }
.feedback-heading h2 { margin: 3px 0 7px; font-size: 25px; }
.feedback-heading span { color: #d7ecfa; font-size: 13px; }
.feedback-statistics { display: grid; grid-template-columns: repeat(5,minmax(150px,1fr)); gap: 12px; margin-bottom: 16px; }
.feedback-stat-card { display: flex; align-items: center; gap: 13px; min-height: 94px; padding: 15px 16px; background: #fff; border: 1px solid #c1def1; border-radius: 9px; }
.feedback-stat-card > .el-icon { display: grid; place-items: center; flex: 0 0 42px; width: 42px; height: 42px; color: #fff; font-size: 21px; border-radius: 12px; background: var(--tone); }
.feedback-stat-card span, .feedback-stat-card strong, .feedback-stat-card small { display: block; }
.feedback-stat-card span { color: #6f8493; font-size: 12px; }
.feedback-stat-card strong { margin: 2px 0; color: #183e59; font-size: 24px; }
.feedback-stat-card small { color: #9aa9b3; font-size: 10px; }
.feedback-stat-card.is-blue { --tone:#1a73e8; }.feedback-stat-card.is-red { --tone:#e34d59; }.feedback-stat-card.is-orange { --tone:#f39c12; }.feedback-stat-card.is-green { --tone:#27ae60; }.feedback-stat-card.is-purple { --tone:#7653c7; }
.feedback-table-card { border-color: #b8daf0 !important; }
.feedback-filters { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 15px; }
.feedback-filters .el-input { width: 340px; }.feedback-filters .el-select { width: 145px; }
.feedback-user-cell { display: flex; align-items: center; gap: 9px; }
.feedback-user-cell .el-avatar { color: #fff; font-weight: 800; background: linear-gradient(135deg,#4da3e5,#1769aa); }
.feedback-user-cell strong, .feedback-user-cell span { display: block; }.feedback-user-cell strong { color: #1d435e; }.feedback-user-cell span { margin-top: 2px; color: #8a9aa5; font-size: 10px; }
.feedback-pagination { display: flex; align-items: center; justify-content: space-between; margin-top: 16px; color: #738a9a; font-size: 12px; }
.feedback-original { margin-bottom: 16px; padding: 14px 16px; color: #617b8d; border: 1px solid #c5e0f1; border-radius: 8px; background: #f2f9fe; }
.feedback-original > div { display: flex; align-items: center; gap: 15px; margin-bottom: 7px; }.feedback-original span { min-width: 65px; font-size: 12px; }.feedback-original strong { color: #174c73; }.feedback-original p { margin: 11px 0 0; padding-top: 11px; color: #274a61; border-top: 1px solid #d3e7f4; line-height: 1.7; white-space: pre-wrap; }
@media (max-width: 1150px) { .feedback-statistics { grid-template-columns: repeat(3,1fr); } }
</style>
