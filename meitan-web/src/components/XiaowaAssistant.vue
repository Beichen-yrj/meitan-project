<template>
  <div class="xiaowa-layer">
    <transition name="xiaowa-panel">
      <section v-if="panelOpen" class="xiaowa-panel" :style="panelPositionStyle" @pointerdown.stop>
        <header class="xiaowa-panel__header">
          <div class="xiaowa-panel__identity">
            <span class="xiaowa-mini-coal">◆</span>
            <div>
              <strong>小瓦</strong>
              <small>DeepSeek 智能助手</small>
            </div>
          </div>
          <div class="xiaowa-header-actions">
            <button type="button" title="API设置" @click="openSettings"><el-icon><Setting /></el-icon></button>
            <button type="button" title="关闭助手" @click="panelOpen = false"><el-icon><CloseBold /></el-icon></button>
          </div>
        </header>

        <div class="xiaowa-toolbar">
          <div class="xiaowa-font-tools" aria-label="文字大小">
            <button type="button" title="缩小文字" @click="changeFontSize(-1)">A−</button>
            <span>{{ messageFontSize }}px</span>
            <button type="button" title="放大文字" @click="changeFontSize(1)">A＋</button>
          </div>
          <button type="button" class="xiaowa-tool-button" :class="{ active: guideOpen }" @click="guideOpen = !guideOpen">
            <el-icon><Compass /></el-icon>页面导航
          </button>
          <label class="xiaowa-speech-switch" :class="{ disabled: !speechSupported }" title="自动朗读小瓦的回答">
            <el-icon><Headset /></el-icon>
            <span>朗读</span>
            <input v-model="autoSpeak" type="checkbox" :disabled="!speechSupported" />
          </label>
        </div>

        <div class="xiaowa-context">当前页面：{{ route.meta.title || '煤层瓦斯智能分析平台' }}</div>

        <div v-if="guideOpen" class="xiaowa-guide">
          <div class="xiaowa-guide__heading">
            <strong>平台功能导航</strong>
            <span>点击即可前往</span>
          </div>
          <div class="xiaowa-guide__grid">
            <button v-for="page in pageGuides" :key="page.path" type="button" @click="goToPage(page.path)">
              <el-icon><component :is="page.icon" /></el-icon>
              <span>{{ page.shortLabel }}</span>
            </button>
          </div>
        </div>

        <main ref="messageArea" class="xiaowa-messages" :style="{ '--message-font-size': `${messageFontSize}px` }">
          <article v-for="message in messages" :key="message.id" class="xiaowa-message" :class="`is-${message.role}`">
            <div class="xiaowa-message__label">{{ message.role === 'assistant' ? '小瓦' : '我' }}</div>
            <div class="xiaowa-message__bubble">
              <div class="xiaowa-message__content">{{ message.content }}</div>
              <div v-if="message.role === 'assistant'" class="xiaowa-message__actions">
                <button v-if="speechSupported" type="button" @click="toggleSpeech(message.content)">
                  <el-icon><component :is="isSpeaking ? 'VideoPause' : 'Headset'" /></el-icon>
                  {{ isSpeaking ? '停止' : '朗读' }}
                </button>
                <button v-if="message.navigation" type="button" class="is-navigation" @click="goToPage(message.navigation)">
                  前往{{ pageName(message.navigation) }}
                </button>
              </div>
            </div>
          </article>

          <article v-if="loading" class="xiaowa-message is-assistant">
            <div class="xiaowa-message__label">小瓦</div>
            <div class="xiaowa-message__bubble xiaowa-typing"><i></i><i></i><i></i></div>
          </article>
        </main>

        <div class="xiaowa-quick-questions">
          <button v-for="question in quickQuestions" :key="question" type="button" @click="useQuickQuestion(question)">
            {{ question }}
          </button>
        </div>

        <footer class="xiaowa-composer">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="2"
            resize="none"
            maxlength="1000"
            show-word-limit
            placeholder="问小瓦平台操作、瓦斯知识，或让小瓦带你前往页面…"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <el-button type="primary" :loading="loading" :disabled="!inputText.trim()" @click="sendMessage">
            <el-icon><Promotion /></el-icon>
          </el-button>
        </footer>
        <div class="xiaowa-key-status" :class="{ ready: hasApiKey }">
          <span>{{ hasApiKey ? 'DeepSeek API Key 已就绪（仅本次浏览会话）' : '尚未设置 DeepSeek API Key' }}</span>
          <button type="button" @click="openSettings">{{ hasApiKey ? '更换' : '设置与教程' }}</button>
        </div>
      </section>
    </transition>

    <button
      type="button"
      class="xiaowa-mascot"
      :class="{ dragging: isDragging, opened: panelOpen }"
      :style="mascotPositionStyle"
      aria-label="打开AI助手小瓦，可拖拽移动"
      title="小瓦AI助手（拖拽移动，点击打开）"
      @pointerdown="startDrag"
    >
      <span v-if="!panelOpen && !isDragging" class="xiaowa-mascot__bubble">有问题问小瓦</span>
      <span class="xiaowa-coal">
        <span class="xiaowa-coal__shine"></span>
        <span class="xiaowa-coal__eye is-left"><i></i></span>
        <span class="xiaowa-coal__eye is-right"><i></i></span>
        <span class="xiaowa-coal__smile"></span>
      </span>
      <span class="xiaowa-mascot__name">小瓦</span>
    </button>
  </div>

  <el-dialog v-model="settingsVisible" title="小瓦 · DeepSeek API 设置" width="560px" append-to-body destroy-on-close>
    <div class="xiaowa-settings">
      <div class="xiaowa-security-note">
        <el-icon><Lock /></el-icon>
        <div><strong>Key 安全说明</strong><p>API Key 仅保存在当前浏览器会话中，通过本平台后端临时转发；不会写入数据库或前端源码。</p></div>
      </div>

      <label class="xiaowa-key-label">DeepSeek API Key</label>
      <el-input v-model="draftApiKey" type="password" show-password clearable placeholder="请输入以 sk- 开头的 API Key" />
      <div class="xiaowa-setting-actions">
        <el-button v-if="hasApiKey" type="danger" plain @click="clearApiKey">清除 Key</el-button>
        <span></span>
        <el-button @click="settingsVisible = false">取消</el-button>
        <el-button type="primary" :disabled="draftApiKey.trim().length < 12" @click="saveApiKey">保存并使用</el-button>
      </div>

      <div class="xiaowa-tutorial">
        <h3>第一次使用？按下面步骤获取 Key</h3>
        <ol>
          <li>打开 DeepSeek 开放平台并登录或注册账号。</li>
          <li>进入“API Keys”，点击“创建 API Key”。</li>
          <li>立即复制生成的 Key（通常以 <code>sk-</code> 开头）。</li>
          <li>粘贴到上方输入框并点击“保存并使用”。</li>
          <li>如调用提示余额不足，请先在开放平台完成充值。</li>
        </ol>
        <div class="xiaowa-tutorial__links">
          <a href="https://platform.deepseek.com/api_keys" target="_blank" rel="noopener noreferrer">打开 DeepSeek API Keys</a>
          <a href="https://api-docs.deepseek.com/" target="_blank" rel="noopener noreferrer">查看 DeepSeek API 文档</a>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { chatWithXiaowa } from '@/api/assistant'

const API_KEY_STORAGE = 'xiaowa-deepseek-api-key'
const POSITION_STORAGE = 'xiaowa-assistant-position'
const FONT_SIZE_STORAGE = 'xiaowa-message-font-size'
const NAVIGATION_PATTERN = /\[NAVIGATE:(\/[a-z-]+)]/i
const MASCOT_SIZE = 80

const router = useRouter()
const route = useRoute()
const panelOpen = ref(false)
const guideOpen = ref(false)
const settingsVisible = ref(false)
const inputText = ref('')
const loading = ref(false)
const messageArea = ref(null)
const apiKey = ref(sessionStorage.getItem(API_KEY_STORAGE) || '')
const draftApiKey = ref(apiKey.value)
const messageFontSize = ref(Math.min(20, Math.max(14, Number(localStorage.getItem(FONT_SIZE_STORAGE)) || 15)))
const autoSpeak = ref(false)
const isSpeaking = ref(false)
const speechSupported = typeof window !== 'undefined' && 'speechSynthesis' in window
const isDragging = ref(false)
const viewport = reactive({ width: window.innerWidth, height: window.innerHeight })
const position = reactive(loadInitialPosition())
let dragState = null
let messageId = 1

const pageGuides = [
  { path: '/home', shortLabel: '首页', label: '首页', icon: 'HomeFilled' },
  { path: '/introduction', shortLabel: '瓦斯知识', label: '瓦斯介绍', icon: 'InfoFilled' },
  { path: '/analysis', shortLabel: '吸附计算', label: '瓦斯吸附量计算与分析', icon: 'TrendCharts' },
  { path: '/statistics', shortLabel: '参数统计', label: '煤层瓦斯吸附参数统计', icon: 'DataAnalysis' },
  { path: '/detection', shortLabel: '危险预测', label: '煤层区域突出危险性预测', icon: 'WarningFilled' },
  { path: '/files', shortLabel: '数据记录', label: '数据文件管理', icon: 'FolderOpened' },
  { path: '/reports', shortLabel: '导出报告', label: '瓦斯数据导出与报告', icon: 'Document' },
  { path: '/feedback', shortLabel: '用户反馈', label: '用户反馈', icon: 'ChatLineSquare' },
  { path: '/user-center', shortLabel: '个人中心', label: '个人中心', icon: 'UserFilled' },
]

const quickQuestions = ['如何计算吸附含量？', '如何检测突出危险？', '查看上次计算分析']

const messages = ref([
  {
    id: messageId++,
    role: 'assistant',
    content: '你好，我是煤炭小助手“小瓦”。我能讲解平台操作和瓦斯知识，也能带你前往各个功能页面。首次使用 DeepSeek 对话时，请先完成 API Key 设置。',
    navigation: '',
  },
])

const hasApiKey = computed(() => apiKey.value.trim().length >= 12)
const mascotPositionStyle = computed(() => ({ left: `${position.x}px`, top: `${position.y}px` }))
const panelPositionStyle = computed(() => {
  const width = Math.min(410, viewport.width - 24)
  const height = Math.min(650, viewport.height - 24)
  const left = clamp(position.x + MASCOT_SIZE / 2 - width / 2, 12, viewport.width - width - 12)
  const preferredTop = position.y > viewport.height / 2 ? position.y - height - 12 : position.y + MASCOT_SIZE + 12
  const top = clamp(preferredTop, 12, viewport.height - height - 12)
  return { left: `${left}px`, top: `${top}px`, width: `${width}px`, height: `${height}px` }
})

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), Math.max(min, max))
}

function loadInitialPosition() {
  try {
    const saved = JSON.parse(localStorage.getItem(POSITION_STORAGE) || 'null')
    if (Number.isFinite(saved?.x) && Number.isFinite(saved?.y)) return saved
  } catch {}
  return { x: Math.max(16, window.innerWidth - 110), y: Math.max(80, window.innerHeight - 130) }
}

function constrainPosition() {
  position.x = clamp(position.x, 8, viewport.width - MASCOT_SIZE - 8)
  position.y = clamp(position.y, 58, viewport.height - MASCOT_SIZE - 8)
}

function handleResize() {
  viewport.width = window.innerWidth
  viewport.height = window.innerHeight
  constrainPosition()
}

function startDrag(event) {
  if (event.button !== 0) return
  event.preventDefault()
  dragState = { pointerId: event.pointerId, startX: event.clientX, startY: event.clientY, originX: position.x, originY: position.y, moved: false }
  document.addEventListener('pointermove', handleDrag)
  document.addEventListener('pointerup', endDrag, { once: true })
}

function handleDrag(event) {
  if (!dragState || event.pointerId !== dragState.pointerId) return
  const dx = event.clientX - dragState.startX
  const dy = event.clientY - dragState.startY
  if (Math.abs(dx) + Math.abs(dy) > 5) {
    dragState.moved = true
    isDragging.value = true
  }
  position.x = clamp(dragState.originX + dx, 8, viewport.width - MASCOT_SIZE - 8)
  position.y = clamp(dragState.originY + dy, 58, viewport.height - MASCOT_SIZE - 8)
}

function endDrag(event) {
  document.removeEventListener('pointermove', handleDrag)
  if (!dragState || event.pointerId !== dragState.pointerId) return
  const moved = dragState.moved
  dragState = null
  isDragging.value = false
  localStorage.setItem(POSITION_STORAGE, JSON.stringify({ x: position.x, y: position.y }))
  if (!moved) panelOpen.value = !panelOpen.value
}

function changeFontSize(delta) {
  messageFontSize.value = clamp(messageFontSize.value + delta, 14, 20)
  localStorage.setItem(FONT_SIZE_STORAGE, String(messageFontSize.value))
}

function openSettings() {
  draftApiKey.value = apiKey.value
  settingsVisible.value = true
}

function saveApiKey() {
  const value = draftApiKey.value.trim()
  if (value.length < 12) return
  apiKey.value = value
  sessionStorage.setItem(API_KEY_STORAGE, value)
  settingsVisible.value = false
  ElMessage.success('DeepSeek API Key 已保存到当前浏览会话')
}

function clearApiKey() {
  apiKey.value = ''
  draftApiKey.value = ''
  sessionStorage.removeItem(API_KEY_STORAGE)
  ElMessage.success('API Key 已清除')
}

function pageName(path) {
  return pageGuides.find((page) => page.path === path)?.label || '相关页面'
}

async function goToPage(path) {
  if (!pageGuides.some((page) => page.path === path)) return
  guideOpen.value = false
  await router.push(path)
}

function useQuickQuestion(question) {
  inputText.value = question
  sendMessage()
}

function parseAssistantContent(rawContent) {
  const match = rawContent.match(NAVIGATION_PATTERN)
  return {
    content: rawContent.replace(NAVIGATION_PATTERN, '').trim(),
    navigation: match && pageGuides.some((page) => page.path === match[1]) ? match[1] : '',
  }
}

function localNavigationForQuestion(question) {
  const mappings = [
    { words: ['吸附', 'langmuir', '含量计算'], path: '/analysis' },
    { words: ['统计', '地区', '挥发分'], path: '/statistics' },
    { words: ['突出', '危险', '检测'], path: '/detection' },
    { words: ['历史', '记录', '数据文件'], path: '/files' },
    { words: ['报告', '导出', '上次计算', '上一次'], path: '/reports' },
    { words: ['瓦斯介绍', '瓦斯知识'], path: '/introduction' },
    { words: ['反馈', '建议'], path: '/feedback' },
    { words: ['密码', '个人中心'], path: '/user-center' },
  ]
  const text = question.toLowerCase()
  return mappings.find((item) => item.words.some((word) => text.includes(word)))?.path || ''
}

async function sendMessage() {
  const content = inputText.value.trim()
  if (!content || loading.value) return
  inputText.value = ''
  messages.value.push({ id: messageId++, role: 'user', content, navigation: '' })
  await scrollToBottom()

  if (!hasApiKey.value) {
    const navigation = localNavigationForQuestion(content)
    messages.value.push({
      id: messageId++,
      role: 'assistant',
      content: navigation
        ? `我已经找到最相关的“${pageName(navigation)}”页面。完整智能问答需要先设置 DeepSeek API Key，你也可以直接点击下方按钮前往。`
        : '完整智能问答需要先设置 DeepSeek API Key。请点击底部“设置与教程”，按步骤创建并填写 Key；页面导航功能现在仍可直接使用。',
      navigation,
    })
    await scrollToBottom()
    openSettings()
    return
  }

  loading.value = true
  try {
    const history = messages.value
      .filter((message) => ['user', 'assistant'].includes(message.role))
      .slice(-14)
      .map(({ role, content: messageContent }) => ({ role, content: messageContent }))
    const response = await chatWithXiaowa({
      messages: history,
      currentPath: route.path,
      currentPage: String(route.meta.title || ''),
    }, apiKey.value)
    const parsed = parseAssistantContent(response.data.content)
    messages.value.push({ id: messageId++, role: 'assistant', ...parsed })
    if (autoSpeak.value) speak(parsed.content)
  } catch (error) {
    const errorMessage = error.response?.data?.message || error.message || '请求失败'
    messages.value.push({
      id: messageId++,
      role: 'assistant',
      content: `暂时无法连接 DeepSeek：${errorMessage}。你可以检查 API Key、账户余额和后端服务后重试。`,
      navigation: localNavigationForQuestion(content),
    })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

function speak(text) {
  if (!speechSupported || !text) return
  window.speechSynthesis.cancel()
  const utterance = new SpeechSynthesisUtterance(text.replace(/\[NAVIGATE:[^\]]+]/g, ''))
  const voices = window.speechSynthesis.getVoices()
  utterance.voice = voices.find((voice) => /xiaoxiao|xiaoyi|huihui/i.test(voice.name))
    || voices.find((voice) => voice.lang?.toLowerCase().startsWith('zh'))
    || null
  utterance.lang = 'zh-CN'
  utterance.rate = 0.88
  utterance.pitch = 1.02
  utterance.volume = 0.9
  utterance.onstart = () => { isSpeaking.value = true }
  utterance.onend = () => { isSpeaking.value = false }
  utterance.onerror = () => { isSpeaking.value = false }
  window.speechSynthesis.speak(utterance)
}

function toggleSpeech(text) {
  if (isSpeaking.value) {
    window.speechSynthesis.cancel()
    isSpeaking.value = false
    return
  }
  speak(text)
}

async function scrollToBottom() {
  await nextTick()
  if (messageArea.value) messageArea.value.scrollTop = messageArea.value.scrollHeight
}

onMounted(() => {
  constrainPosition()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('pointermove', handleDrag)
  window.speechSynthesis?.cancel()
})
</script>

<style scoped>
.xiaowa-layer { position: relative; z-index: 3900; }
.xiaowa-mascot {
  position: fixed;
  z-index: 3902;
  width: 80px;
  height: 92px;
  padding: 0;
  border: 0;
  outline: none;
  color: inherit;
  background: transparent;
  cursor: grab;
  user-select: none;
  touch-action: none;
  filter: drop-shadow(0 7px 9px rgba(4, 27, 50, .28));
  transition: filter .2s ease, transform .2s ease;
}
.xiaowa-mascot:hover { transform: translateY(-3px) scale(1.04); filter: drop-shadow(0 10px 12px rgba(4, 27, 50, .34)); }
.xiaowa-mascot.dragging { cursor: grabbing; transform: scale(1.06); transition: none; }
.xiaowa-mascot.opened { filter: drop-shadow(0 7px 13px rgba(26, 115, 232, .38)); }
.xiaowa-coal {
  position: absolute;
  left: 8px;
  top: 4px;
  width: 65px;
  height: 64px;
  overflow: hidden;
  clip-path: polygon(17% 8%, 52% 0, 83% 13%, 100% 43%, 88% 80%, 59% 100%, 23% 92%, 0 64%, 4% 29%);
  background:
    radial-gradient(circle at 28% 22%, rgba(255,255,255,.35) 0 4%, transparent 19%),
    radial-gradient(circle at 70% 72%, #101b25 0 20%, transparent 43%),
    linear-gradient(145deg, #4b5d6c 0%, #253541 42%, #0d1821 100%);
  border: 2px solid rgba(149, 199, 230, .75);
  box-shadow: inset -8px -10px 14px rgba(0,0,0,.35), inset 7px 7px 10px rgba(255,255,255,.12);
}
.xiaowa-coal__shine { position: absolute; left: 13px; top: 10px; width: 19px; height: 8px; border-radius: 50%; background: rgba(255,255,255,.28); transform: rotate(-25deg); }
.xiaowa-coal__eye { position: absolute; top: 25px; width: 14px; height: 15px; border-radius: 50%; background: #f7fbff; box-shadow: 0 0 4px rgba(103,209,255,.75); }
.xiaowa-coal__eye.is-left { left: 17px; }
.xiaowa-coal__eye.is-right { right: 16px; }
.xiaowa-coal__eye i { position: absolute; left: 5px; top: 5px; width: 6px; height: 7px; border-radius: 50%; background: #0d47a1; }
.xiaowa-coal__smile { position: absolute; left: 25px; top: 43px; width: 20px; height: 10px; border-bottom: 3px solid #75d8ff; border-radius: 0 0 18px 18px; }
.xiaowa-mascot__name { position: absolute; left: 11px; top: 68px; width: 58px; padding: 3px 0; color: #fff; font-size: 13px; font-weight: 800; letter-spacing: 2px; border-radius: 12px; background: linear-gradient(90deg,#0d47a1,#1a73e8); }
.xiaowa-mascot__bubble { position: absolute; right: 68px; top: 5px; width: 102px; padding: 7px 8px; color: #0b3f73; font-size: 12px; font-weight: 700; border: 1px solid #9bd2fa; border-radius: 13px 13px 2px 13px; background: rgba(255,255,255,.94); pointer-events: none; }
.xiaowa-panel {
  position: fixed;
  z-index: 3901;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: #19364d;
  border: 1px solid #79bde9;
  border-radius: 16px;
  background: rgba(247, 252, 255, .97);
  box-shadow: 0 18px 48px rgba(5, 48, 88, .28);
  backdrop-filter: blur(12px);
}
.xiaowa-panel__header { display: flex; align-items: center; justify-content: space-between; flex: 0 0 62px; padding: 0 14px 0 17px; color: #fff; background: linear-gradient(125deg,#0b3f73,#1769aa 65%,#2997d6); }
.xiaowa-panel__identity { display: flex; align-items: center; gap: 10px; }
.xiaowa-panel__identity strong, .xiaowa-panel__identity small { display: block; }
.xiaowa-panel__identity strong { font-size: 18px; letter-spacing: 2px; }
.xiaowa-panel__identity small { margin-top: 2px; color: #cdeaff; font-size: 11px; }
.xiaowa-mini-coal { display: grid; place-items: center; width: 35px; height: 35px; color: #8ee1ff; font-size: 22px; clip-path: polygon(20% 0,80% 7%,100% 45%,78% 100%,25% 91%,0 56%); background: #172a38; }
.xiaowa-header-actions { display: flex; gap: 5px; }
.xiaowa-header-actions button { display: grid; place-items: center; width: 31px; height: 31px; color: #fff; font-size: 17px; border: 0; border-radius: 7px; background: rgba(255,255,255,.12); cursor: pointer; }
.xiaowa-header-actions button:hover { background: rgba(255,255,255,.25); }
.xiaowa-toolbar { display: flex; align-items: center; gap: 8px; flex: 0 0 43px; padding: 6px 11px; border-bottom: 1px solid #c8e3f5; background: #eef8ff; }
.xiaowa-font-tools { display: flex; align-items: center; overflow: hidden; border: 1px solid #a9d4f1; border-radius: 7px; background: #fff; }
.xiaowa-font-tools button { width: 31px; height: 27px; border: 0; color: #0d47a1; background: transparent; cursor: pointer; }
.xiaowa-font-tools button:hover { background: #dff2ff; }
.xiaowa-font-tools span { min-width: 35px; color: #536b7d; font-size: 11px; text-align: center; }
.xiaowa-tool-button { display: flex; align-items: center; gap: 4px; height: 29px; padding: 0 8px; color: #315b78; border: 1px solid #a9d4f1; border-radius: 7px; background: #fff; cursor: pointer; }
.xiaowa-tool-button:hover, .xiaowa-tool-button.active { color: #fff; border-color: #1a73e8; background: #1a73e8; }
.xiaowa-speech-switch { display: flex; align-items: center; gap: 4px; margin-left: auto; color: #315b78; font-size: 12px; cursor: pointer; }
.xiaowa-speech-switch input { accent-color: #1a73e8; cursor: pointer; }
.xiaowa-speech-switch.disabled { opacity: .45; cursor: not-allowed; }
.xiaowa-context { flex: 0 0 27px; padding: 6px 13px; overflow: hidden; color: #617d91; font-size: 11px; white-space: nowrap; text-overflow: ellipsis; border-bottom: 1px solid #d9ebf7; }
.xiaowa-guide { flex: 0 0 auto; padding: 10px 11px; border-bottom: 1px solid #bcdff5; background: #f3faff; }
.xiaowa-guide__heading { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 8px; color: #0d47a1; font-size: 13px; }
.xiaowa-guide__heading span { color: #7890a1; font-size: 10px; }
.xiaowa-guide__grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 6px; }
.xiaowa-guide__grid button { display: flex; align-items: center; gap: 4px; min-width: 0; padding: 6px; color: #315b78; font-size: 11px; border: 1px solid #c6e2f5; border-radius: 6px; background: #fff; cursor: pointer; }
.xiaowa-guide__grid button span { overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.xiaowa-guide__grid button:hover { color: #fff; border-color: #1a73e8; background: #1a73e8; }
.xiaowa-messages { flex: 1 1 auto; min-height: 110px; padding: 13px 12px; overflow-y: auto; background: linear-gradient(180deg,#fafdff,#eef7fd); }
.xiaowa-message { display: flex; flex-direction: column; align-items: flex-start; margin-bottom: 12px; }
.xiaowa-message.is-user { align-items: flex-end; }
.xiaowa-message__label { margin: 0 5px 4px; color: #748b9b; font-size: 10px; }
.xiaowa-message__bubble { max-width: 88%; padding: 10px 12px 8px; border: 1px solid #c2e1f4; border-radius: 5px 14px 14px 14px; background: #fff; box-shadow: 0 3px 9px rgba(13,71,161,.07); }
.is-user .xiaowa-message__bubble { color: #fff; border-color: #1a73e8; border-radius: 14px 5px 14px 14px; background: linear-gradient(135deg,#1a73e8,#1769aa); }
.xiaowa-message__content { font-size: var(--message-font-size); line-height: 1.65; white-space: pre-wrap; overflow-wrap: anywhere; }
.xiaowa-message__actions { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 7px; padding-top: 6px; border-top: 1px solid #e1edf5; }
.xiaowa-message__actions button { display: flex; align-items: center; gap: 3px; padding: 3px 7px; color: #567083; font-size: 10px; border: 1px solid #cee3f1; border-radius: 10px; background: #f6fbff; cursor: pointer; }
.xiaowa-message__actions button:hover, .xiaowa-message__actions .is-navigation { color: #0d47a1; border-color: #79bde9; background: #e7f5ff; }
.xiaowa-typing { display: flex; gap: 5px; padding: 13px 16px; }
.xiaowa-typing i { width: 6px; height: 6px; border-radius: 50%; background: #1a73e8; animation: typing-dot 1.1s infinite ease-in-out; }
.xiaowa-typing i:nth-child(2) { animation-delay: .15s; }
.xiaowa-typing i:nth-child(3) { animation-delay: .3s; }
.xiaowa-quick-questions { display: flex; gap: 6px; flex: 0 0 auto; padding: 7px 10px; overflow-x: auto; border-top: 1px solid #d4e8f5; background: #f8fcff; }
.xiaowa-quick-questions button { flex: 0 0 auto; padding: 5px 8px; color: #315b78; font-size: 10px; border: 1px solid #bdddf1; border-radius: 12px; background: #fff; cursor: pointer; }
.xiaowa-quick-questions button:hover { color: #fff; border-color: #1a73e8; background: #1a73e8; }
.xiaowa-composer { display: grid; grid-template-columns: 1fr 42px; gap: 8px; flex: 0 0 auto; padding: 9px 10px 7px; border-top: 1px solid #c6e2f5; background: #fff; }
.xiaowa-composer .el-button { width: 42px; height: 52px; margin: 0; font-size: 19px; }
.xiaowa-key-status { display: flex; align-items: center; justify-content: space-between; flex: 0 0 25px; padding: 4px 11px 6px; color: #ad6800; font-size: 9px; background: #fff7e8; }
.xiaowa-key-status.ready { color: #237b45; background: #edf9f1; }
.xiaowa-key-status button { padding: 0; color: inherit; font-size: 10px; text-decoration: underline; border: 0; background: transparent; cursor: pointer; }
.xiaowa-settings { color: #28465b; }
.xiaowa-security-note { display: flex; gap: 10px; padding: 11px 13px; margin-bottom: 16px; color: #24516f; border: 1px solid #b9dcf3; border-radius: 8px; background: #eff9ff; }
.xiaowa-security-note .el-icon { flex: 0 0 auto; margin-top: 2px; color: #1a73e8; font-size: 20px; }
.xiaowa-security-note p { margin: 3px 0 0; color: #617d91; font-size: 12px; line-height: 1.55; }
.xiaowa-key-label { display: block; margin-bottom: 7px; color: #0d47a1; font-weight: 700; }
.xiaowa-setting-actions { display: grid; grid-template-columns: auto 1fr auto auto; gap: 8px; margin-top: 12px; }
.xiaowa-tutorial { margin-top: 20px; padding: 15px 17px; border: 1px solid #d5e7f3; border-radius: 8px; background: #fafcfe; }
.xiaowa-tutorial h3 { margin: 0 0 10px; color: #0b3f73; font-size: 15px; }
.xiaowa-tutorial ol { padding-left: 22px; color: #496579; font-size: 13px; line-height: 1.85; }
.xiaowa-tutorial code { padding: 1px 5px; color: #c62828; border-radius: 4px; background: #fff0f0; }
.xiaowa-tutorial__links { display: flex; flex-wrap: wrap; gap: 9px; margin-top: 12px; }
.xiaowa-tutorial__links a { padding: 7px 11px; color: #fff; font-size: 12px; text-decoration: none; border-radius: 6px; background: #1a73e8; }
.xiaowa-tutorial__links a:last-child { background: #607d8b; }
.xiaowa-tutorial__links a:hover { filter: brightness(1.08); }
.xiaowa-panel-enter-active, .xiaowa-panel-leave-active { transition: opacity .2s ease, transform .2s ease; }
.xiaowa-panel-enter-from, .xiaowa-panel-leave-to { opacity: 0; transform: translateY(10px) scale(.97); }
@keyframes typing-dot { 0%,60%,100% { transform: translateY(0); opacity: .45; } 30% { transform: translateY(-5px); opacity: 1; } }
@media (max-width: 600px) {
  .xiaowa-mascot__bubble { display: none; }
  .xiaowa-panel { border-radius: 12px; }
}
</style>
