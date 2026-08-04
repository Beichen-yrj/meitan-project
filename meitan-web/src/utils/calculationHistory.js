import { useUserStore } from '@/store/user'

const STORAGE_PREFIX = 'meitan-calculation-history:user'
const LEGACY_STORAGE_KEY = 'meitan-calculation-history'
const HISTORY_EVENT = 'meitan-history-updated'
const MAX_RECORDS = 50
const RETENTION_DAYS = 60
const RETENTION_MILLISECONDS = RETENTION_DAYS * 24 * 60 * 60 * 1000

const MODULE_LABELS = {
  analysis: '瓦斯吸附含量计算与分析',
  statistics: '煤样瓦斯吸附参数统计分析',
  detection: '煤层瓦斯突出危险性检测',
}

function getCurrentAccountId() {
  try {
    const userStore = useUserStore()
    if (userStore.userId !== null && userStore.userId !== undefined && userStore.userId !== '') {
      return String(userStore.userId)
    }
  } catch {}

  // 兼容 Pinia 尚未初始化的极短时间窗口，但仍只读取平台自己的持久化用户标识。
  if (typeof localStorage !== 'undefined') {
    try {
      const persistedUser = JSON.parse(localStorage.getItem('meitan-user') || '{}')
      if (persistedUser.userId !== null && persistedUser.userId !== undefined && persistedUser.userId !== '') {
        return String(persistedUser.userId)
      }
    } catch {}
  }
  return 'anonymous'
}

function storageKey(accountId = getCurrentAccountId()) {
  const safeAccountId = String(accountId).replace(/[^a-zA-Z0-9_-]/g, '_')
  return `${STORAGE_PREFIX}:${safeAccountId}`
}

function recordTimestamp(item) {
  const calculationTimestamp = Date.parse(item?.calculationTime)
  if (Number.isFinite(calculationTimestamp)) return calculationTimestamp

  const idTimestamp = Number(String(item?.id || '').split('-')[0])
  return Number.isFinite(idTimestamp) && idTimestamp > 0 ? idTimestamp : null
}

function retainRecentHistory(history, accountId, now = Date.now()) {
  const cutoff = now - RETENTION_MILLISECONDS
  return history.filter((item) => {
    if (item.ownerUserId && String(item.ownerUserId) !== String(accountId)) return false
    const timestamp = recordTimestamp(item)
    return timestamp === null || timestamp >= cutoff
  })
}

function readHistory(accountId = getCurrentAccountId()) {
  if (typeof localStorage === 'undefined') return []
  let value
  try {
    value = JSON.parse(localStorage.getItem(storageKey(accountId)) || '[]')
  } catch {
    return []
  }
  if (!Array.isArray(value)) return []

  // 同时校验记录归属和保留期限，旧格式且无法识别时间的记录不会被误删。
  const retained = retainRecentHistory(value, accountId)
  if (retained.length !== value.length) {
    try {
      writeHistory(retained, accountId)
    } catch {}
  }
  return retained
}

function writeHistory(history, accountId = getCurrentAccountId()) {
  localStorage.setItem(storageKey(accountId), JSON.stringify(history))
}

function notifyHistoryUpdated(accountId = getCurrentAccountId()) {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent(HISTORY_EVENT, { detail: { accountId: String(accountId) } }))
  }
}

export function getCalculationHistory() {
  return readHistory()
}

export function purgeExpiredCalculationHistory() {
  if (typeof localStorage === 'undefined') return 0
  const accountId = getCurrentAccountId()
  let originalCount = 0
  try {
    const value = JSON.parse(localStorage.getItem(storageKey(accountId)) || '[]')
    originalCount = Array.isArray(value) ? value.length : 0
  } catch {}

  const retained = readHistory(accountId)
  const removedCount = Math.max(originalCount - retained.length, 0)
  if (removedCount > 0) notifyHistoryUpdated(accountId)
  return removedCount
}

export function getLatestCalculation(moduleType) {
  return readHistory().find((item) => !moduleType || item.moduleType === moduleType) || null
}

export function saveCalculationRecord(record) {
  const now = new Date()
  const accountId = getCurrentAccountId()
  const item = {
    id: `${now.getTime()}-${Math.random().toString(36).slice(2, 8)}`,
    ownerUserId: accountId,
    moduleType: record.moduleType,
    moduleLabel: MODULE_LABELS[record.moduleType] || record.moduleType,
    calculationTime: record.calculationTime || now.toISOString(),
    displayTime: record.displayTime || now.toLocaleString('zh-CN', { hour12: false }),
    sourceName: record.sourceName || '',
    params: record.params || {},
    result: record.result || {},
    inputData: record.inputData || null,
    chartImage: record.chartImage || '',
    summary: record.summary || '',
  }

  const history = [item, ...readHistory(accountId)].slice(0, MAX_RECORDS)
  try {
    writeHistory(history, accountId)
    notifyHistoryUpdated(accountId)
  } catch (error) {
    // 图像数据可能使浏览器存储空间不足；仅压缩当前账号的数据，不影响其他账号。
    try {
      const fallback = { ...item, chartImage: '' }
      const compactHistory = readHistory(accountId)
        .filter((historyItem) => historyItem.id !== fallback.id)
        .slice(0, 19)
        .map((historyItem) => ({ ...historyItem, chartImage: '' }))
      writeHistory([fallback, ...compactHistory], accountId)
      notifyHistoryUpdated(accountId)
    } catch {
      console.warn('计算历史保存失败', error)
    }
  }
  return item
}

export function removeCalculationRecord(id) {
  const accountId = getCurrentAccountId()
  const history = readHistory(accountId).filter((item) => item.id !== id)
  writeHistory(history, accountId)
  notifyHistoryUpdated(accountId)
}

export function clearCalculationHistory() {
  const accountId = getCurrentAccountId()
  localStorage.removeItem(storageKey(accountId))
  notifyHistoryUpdated(accountId)
}

export function onCalculationHistoryUpdated(callback) {
  if (typeof window === 'undefined') return () => {}
  const accountId = getCurrentAccountId()
  const historyHandler = (event) => {
    if (!event.detail?.accountId || String(event.detail.accountId) === String(accountId)) callback()
  }
  const storageHandler = (event) => {
    if (event.key === storageKey(accountId)) callback()
  }
  window.addEventListener(HISTORY_EVENT, historyHandler)
  window.addEventListener('storage', storageHandler)
  return () => {
    window.removeEventListener(HISTORY_EVENT, historyHandler)
    window.removeEventListener('storage', storageHandler)
  }
}

// 仅供升级说明和自动化验证使用；旧的无归属记录不会自动分配给任一账号。
export function hasLegacyUnscopedHistory() {
  return typeof localStorage !== 'undefined' && Boolean(localStorage.getItem(LEGACY_STORAGE_KEY))
}

export { MODULE_LABELS, RETENTION_DAYS }
