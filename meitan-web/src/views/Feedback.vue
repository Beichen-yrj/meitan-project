<template>
  <div class="user-feedback-page">
    <section class="user-feedback-heading">
      <div><h2>用户反馈</h2><p>您的意见将直接进入管理员反馈处理中心，处理结果会在下方同步更新。</p></div>
      <el-icon><ChatDotRound /></el-icon>
    </section>

    <div class="user-feedback-grid">
      <el-card shadow="never">
        <template #header><span class="card-title">提交新反馈</span></template>
        <el-form label-position="top">
          <el-form-item label="使用体验评分">
            <el-rate v-model="form.rating" show-score text-color="var(--text-secondary)" size="large" />
          </el-form-item>
          <el-form-item label="意见内容">
            <el-input v-model="form.content" type="textarea" :rows="7" maxlength="2000" show-word-limit placeholder="请描述遇到的问题、使用体验或改进建议..." />
          </el-form-item>
          <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
            <el-icon><Promotion /></el-icon>提交给管理员
          </el-button>
        </el-form>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="history-title"><span class="card-title">我的反馈记录</span><el-button link type="primary" @click="loadMine"><el-icon><Refresh /></el-icon>刷新</el-button></div>
        </template>
        <div v-loading="loading" class="my-feedback-list">
          <article v-for="item in feedbacks" :key="item.id" class="my-feedback-item">
            <header>
              <el-rate :model-value="item.rating" disabled size="small" />
              <el-tag :type="statusType(item.status)" effect="dark">{{ statusLabel(item.status) }}</el-tag>
              <time>{{ formatDate(item.createTime) }}</time>
            </header>
            <p>{{ item.content }}</p>
            <div v-if="item.adminReply" class="admin-reply">
              <strong><el-icon><Service /></el-icon>管理员回复</strong>
              <span>{{ item.adminReply }}</span>
              <small>{{ item.handledByUsername || 'admin' }} · {{ formatDate(item.handledTime) }}</small>
            </div>
            <div v-else class="waiting-reply">管理员尚未回复，请耐心等待处理。</div>
          </article>
          <el-empty v-if="!loading && !feedbacks.length" description="您还没有提交过反馈" />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyFeedback, submitFeedback } from '@/api'

const form = reactive({ rating: 0, content: '' })
const feedbacks = ref([])
const submitting = ref(false)
const loading = ref(false)

async function handleSubmit() {
  if (!form.rating) { ElMessage.warning('请先选择评分'); return }
  if (!form.content.trim()) { ElMessage.warning('请填写反馈内容'); return }
  submitting.value = true
  try {
    await submitFeedback({ rating: form.rating, content: form.content.trim() })
    form.rating = 0
    form.content = ''
    ElMessage.success('反馈已提交至管理员处理中心')
    await loadMine()
  } finally { submitting.value = false }
}

async function loadMine() {
  loading.value = true
  try {
    const response = await getMyFeedback()
    feedbacks.value = response.data || []
  } finally { loading.value = false }
}

const statusLabel = (status) => ({ PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已解决' }[status] || status)
const statusType = (status) => ({ PENDING: 'danger', PROCESSING: 'warning', RESOLVED: 'success' }[status] || 'info')
function formatDate(value) {
  if (!value) return '暂无'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value).replace('T', ' ') : date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(loadMine)
</script>

<style scoped>
.user-feedback-page { color:var(--text-primary); }.user-feedback-heading { display:flex;align-items:center;justify-content:space-between;margin-bottom:16px;padding:20px 24px;color:#fff;border-radius:10px;background:linear-gradient(125deg,#0b3f73,#1769aa 70%,#2b9bd9); }.user-feedback-heading h2 { margin:0 0 6px;font-size:24px; }.user-feedback-heading p { margin:0;color:#d9ecf8;font-size:13px; }.user-feedback-heading > .el-icon { font-size:42px;color:#7bdcff; }.user-feedback-grid { display:grid;grid-template-columns:minmax(360px,.75fr) minmax(520px,1.25fr);gap:16px; }.user-feedback-grid .el-card { border-color:#a9d4f1; }.card-title { color:var(--primary);font-size:17px;font-weight:800; }.history-title { display:flex;align-items:center;justify-content:space-between; }.my-feedback-list { min-height:250px;max-height:calc(100vh - 225px);overflow-y:auto;padding-right:4px; }.my-feedback-item { margin-bottom:12px;padding:14px 16px;border:1px solid #cae2f2;border-radius:8px;background:#fbfdff; }.my-feedback-item header { display:flex;align-items:center;gap:10px; }.my-feedback-item time { margin-left:auto;color:#8497a4;font-size:11px; }.my-feedback-item > p { margin:11px 0;color:#294b61;line-height:1.7;white-space:pre-wrap; }.admin-reply { padding:11px 13px;color:#335a73;border-left:4px solid #27ae60;background:#edf9f1; }.admin-reply strong,.admin-reply span,.admin-reply small { display:block; }.admin-reply strong { display:flex;align-items:center;gap:5px;color:#218739;font-size:12px; }.admin-reply span { margin:6px 0;line-height:1.65;white-space:pre-wrap; }.admin-reply small { color:#789184;text-align:right; }.waiting-reply { padding:8px 11px;color:#8b7a52;font-size:11px;background:#fff8e8; }.user-feedback-grid .el-button { width:100%; }
@media (max-width:1000px) { .user-feedback-grid { grid-template-columns:1fr; } }
</style>
