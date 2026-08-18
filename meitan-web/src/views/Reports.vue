<template>
  <div class="report-page">
    <ModulePageHeader title="瓦斯数据导出与报告" />

    <div class="report-status-grid">
      <div v-for="module in modules" :key="module.type" class="report-status-card" :class="{ 'is-ready': latestByModule[module.type] }">
        <span class="report-status-dot"></span>
        <div>
          <strong>{{ module.label }}</strong>
          <small>{{ latestByModule[module.type] ? `数据可用 · ${latestByModule[module.type].displayTime}` : '暂无计算数据' }}</small>
        </div>
      </div>
    </div>

    <div class="report-main-grid">
      <section class="report-left-column">
        <div class="report-section-card report-latest-card">
          <div class="report-section-card__header">
            <div>
              <h2>最近一次计算分析</h2>
              <p>计算完成后自动更新，可在此查看上一次分析结果</p>
            </div>
            <el-button type="primary" @click="refreshHistory"><el-icon><Refresh /></el-icon>刷新数据</el-button>
          </div>

          <el-empty v-if="!latestRecord" description="请先在任一分析模块中完成计算" />
          <template v-else>
            <div class="latest-record-heading">
              <el-tag :type="moduleColor(latestRecord.moduleType)" size="large">{{ latestRecord.moduleLabel }}</el-tag>
              <span>{{ latestRecord.displayTime }}</span>
            </div>
            <div class="latest-record-summary">{{ latestRecord.summary || '计算已完成' }}</div>
            <div class="latest-record-info">
              <span><b>数据来源：</b>{{ latestRecord.sourceName || '-' }}</span>
              <span><b>数据点：</b>{{ dataPointCount(latestRecord) }}</span>
              <span v-if="latestRecord.moduleType === 'detection'">
                <b>风险判定：</b><em :class="latestRecord.result?.is_danger ? 'is-danger' : 'is-safe'">{{ latestRecord.result?.is_danger ? '危险' : '安全' }}</em>
              </span>
            </div>
            <div v-if="latestChartImage" class="latest-chart-box">
              <ClickableChartImage :image="latestChartImage" :alt="latestRecord.moduleLabel" />
            </div>
          </template>
        </div>

        <div v-for="module in modules" :key="`detail-${module.type}`" class="report-section-card module-result-card">
          <div class="module-result-title" :class="`module-result-title--${module.type}`">
            <el-icon><component :is="module.icon" /></el-icon>
            {{ module.label }}
          </div>
          <el-empty v-if="!latestByModule[module.type]" description="暂无数据" :image-size="55" />
          <div v-else class="module-result-content">
            <template v-if="module.type === 'analysis'">
              <span>煤样：<b>{{ latestByModule.analysis.params?.coalType || '-' }}</b></span>
              <span>挥发分：<b>{{ latestByModule.analysis.params?.volatile ?? '-' }}%</b></span>
              <span>Vl / Pl：<b>{{ latestByModule.analysis.params?.vl ?? '-' }} / {{ latestByModule.analysis.params?.pl ?? '-' }}</b></span>
              <span>最大吸附量：<b>{{ latestByModule.analysis.result?.stats?.max_vm ?? '-' }}</b></span>
            </template>
            <template v-else-if="module.type === 'statistics'">
              <span>数据文件：<b>{{ latestByModule.statistics.sourceName || '-' }}</b></span>
              <span>地区筛选：<b>{{ statisticsRegionLabel(latestByModule.statistics.params?.regionFilter) }}</b></span>
              <span>图表类型：<b>{{ chartTypeLabel(latestByModule.statistics.params?.chartType) }}</b></span>
              <span>统计摘要：<b>{{ latestByModule.statistics.summary || '-' }}</b></span>
            </template>
            <template v-else>
              <span>数据文件：<b>{{ latestByModule.detection.sourceName || '-' }}</b></span>
              <span>实测压力：<b>{{ latestByModule.detection.params?.measuredPressure ?? '-' }} MPa</b></span>
              <span>瓦斯含量计算值：<b>{{ detectionCalculatedContent(latestByModule.detection) }} m³/t</b></span>
              <span>瓦斯含量临界值：<b>{{ latestByModule.detection.params?.critContent ?? '-' }} m³/t</b></span>
              <span>检测结果：<b :class="latestByModule.detection.result?.is_danger ? 'is-danger' : 'is-safe'">{{ latestByModule.detection.result?.is_danger ? '存在突出危险' : '未检测到突出危险' }}</b></span>
            </template>
          </div>
        </div>
      </section>

      <aside class="report-opinion-card">
        <h2><el-icon><Search /></el-icon>瓦斯安全综合评价意见</h2>
        <div class="report-opinion-text">{{ professionalOpinion }}</div>
        <div class="report-export-actions">
          <el-button type="success" size="large" :disabled="!history.length" @click="exportXlsx">
            <el-icon><Document /></el-icon>导出 XLSX 报告
          </el-button>
          <el-button type="primary" size="large" :disabled="!history.length" @click="exportHtml">
            <el-icon><Monitor /></el-icon>导出 HTML 报告
          </el-button>
        </div>
        <div class="report-history-title">最近计算记录</div>
        <el-table :data="history.slice(0, 8)" size="small" max-height="300" empty-text="暂无记录">
          <el-table-column label="模块" min-width="105">
            <template #default="{ row }">{{ shortModuleLabel(row.moduleType) }}</template>
          </el-table-column>
          <el-table-column prop="displayTime" label="计算时间" min-width="148" />
        </el-table>
      </aside>
    </div>

    <div class="draft-status-bar">{{ history.length ? `已汇总 ${history.length} 条计算记录，最近更新：${latestRecord?.displayTime}` : '暂无可汇总的计算数据' }}</div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as XLSX from 'xlsx'
import { ElMessage } from 'element-plus'
import ModulePageHeader from '@/components/ModulePageHeader.vue'
import ClickableChartImage from '@/components/ClickableChartImage.vue'
import { getCalculationHistory, onCalculationHistoryUpdated } from '@/utils/calculationHistory'
import { timestampForFilename } from '@/utils/download'
import '@/assets/styles/workbench.css'

const history = ref([])
let removeHistoryListener = () => {}

const modules = [
  { type: 'detection', label: '煤层区域突出危险性预测', icon: 'WarningFilled' },
  { type: 'analysis', label: '瓦斯吸附量计算与分析', icon: 'TrendCharts' },
  { type: 'statistics', label: '煤层瓦斯吸附参数统计', icon: 'DataAnalysis' },
]

const latestRecord = computed(() => history.value[0] || null)
const latestChartImage = computed(() => latestRecord.value?.chartImage || latestRecord.value?.result?.chart_image_base64 || '')
const latestByModule = reactive({ analysis: null, statistics: null, detection: null })
const moduleColor = (moduleType) => ({ analysis: 'primary', statistics: 'success', detection: 'warning' }[moduleType] || 'info')
const shortModuleLabel = (moduleType) => ({ analysis: '吸附分析', statistics: '统计分析', detection: '危险检测' }[moduleType] || moduleType)
const detectionCalculatedContent = (record) => record?.result?.calculated_content ?? record?.result?.measured_content ?? record?.params?.calculatedContent ?? record?.params?.measuredContent ?? '-'
const statisticsRegionLabel = (value) => Array.isArray(value) ? (value.length ? value.join('、') : '全部地区') : (value || '全部地区')
const chartTypeLabel = (type) => ({ scatter: '散点图', dual_axis: '双坐标轴图', grouped: '分组图' }[type] || type || '-')

function refreshHistory() {
  history.value = getCalculationHistory()
  for (const module of modules) latestByModule[module.type] = history.value.find((item) => item.moduleType === module.type) || null
}

function dataPointCount(record) {
  if (record.moduleType === 'statistics') return Math.max((record.inputData?.length || 1) - 1, 0)
  return record.result?.p_array?.length || 0
}

const professionalOpinion = computed(() => {
  if (!history.value.length) return '请先在瓦斯分析模块完成计算，系统将自动汇总计算成果并生成专业评价意见。'
  const lines = []
  const analysis = latestByModule.analysis
  if (analysis) {
    const vl = Number(analysis.params?.vl)
    const level = vl < 15 ? '较弱' : vl <= 30 ? '中等' : '较强'
    lines.push(`【吸附能力】煤样 ${analysis.params?.coalType || ''} 的 Langmuir Vl 值为 ${analysis.params?.vl ?? '-'}，瓦斯吸附能力${level}；计算范围内最大吸附量为 ${analysis.result?.stats?.max_vm ?? '-'}。`)
  }
  const statistics = latestByModule.statistics
  if (statistics) lines.push(`【统计分析】${statistics.summary || '统计图表已生成'}。建议结合地区、挥发分及 VL/PL 参数分布识别异常煤样。`)
  const detection = latestByModule.detection
  if (detection) lines.push(`【突出危险性】${detection.result?.danger_reason || '危险性检测已完成'}`)
  if (!detection) lines.push('【安全建议】当前尚未完成突出危险性检测，综合评价缺少临界压力和含量判定，建议补充检测。')
  else if (detection.result?.is_danger) lines.push('【处置建议】检测结果存在突出危险，应立即落实区域和局部综合防突措施，并加强瓦斯压力、含量及抽采效果监测。')
  else lines.push('【处置建议】当前检测结果未达到突出危险临界值，仍应保持连续监测并定期复核基础参数。')
  return lines.join('\n\n')
})

function reportInfoRows(record) {
  return [
    ['计算模块', record.moduleLabel], ['计算时间', record.displayTime], ['数据来源', record.sourceName], ['结果摘要', record.summary],
    ...Object.entries(record.params || {}).map(([key, value]) => [key, typeof value === 'object' ? JSON.stringify(value) : value]),
  ]
}

function resultRows(record) {
  if (record.moduleType === 'analysis') return (record.result?.p_array || []).map((pressure, index) => ({ 序号: index + 1, '压力P(MPa)': pressure, '吸附量Vm(cm³/g)': record.result?.vm_array?.[index] }))
  if (record.moduleType === 'detection') return (record.result?.p_array || []).map((pressure, index) => ({ 序号: index + 1, '压力P(MPa)': pressure, '吸附瓦斯Xx': record.inputData?.xxArray?.[index], '游离瓦斯Xy': record.result?.xy_array?.[index], '总瓦斯Q': record.result?.q_array?.[index] }))
  const headers = record.inputData?.[0] || []
  return (record.inputData || []).slice(1).map((row) => Object.fromEntries(headers.map((header, index) => [String(header || `列${index + 1}`), row[index]])))
}

function exportXlsx() {
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, XLSX.utils.aoa_to_sheet([
    ['瓦斯安全综合评价报告'], ['生成时间', new Date().toLocaleString('zh-CN', { hour12: false })], ['综合评价意见', professionalOpinion.value],
  ]), '综合意见')
  for (const module of modules) {
    const record = latestByModule[module.type]
    if (!record) continue
    const sheetData = XLSX.utils.aoa_to_sheet([['项目', '内容'], ...reportInfoRows(record)])
    XLSX.utils.book_append_sheet(workbook, sheetData, shortModuleLabel(module.type))
    const rows = resultRows(record)
    if (rows.length) XLSX.utils.book_append_sheet(workbook, XLSX.utils.json_to_sheet(rows), `${shortModuleLabel(module.type)}明细`.slice(0, 31))
  }
  XLSX.writeFile(workbook, `瓦斯数据综合报告_${timestampForFilename()}.xlsx`)
  ElMessage.success('XLSX 综合报告已导出')
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]))
}

function exportHtml() {
  const generatedAt = new Date().toLocaleString('zh-CN', { hour12: false })
  const sections = modules.map((module) => {
    const record = latestByModule[module.type]
    if (!record) return ''
    const rows = resultRows(record).slice(0, 100)
    const headers = rows.length ? Object.keys(rows[0]) : []
    const table = rows.length ? `<table><thead><tr>${headers.map((header) => `<th>${escapeHtml(header)}</th>`).join('')}</tr></thead><tbody>${rows.map((row) => `<tr>${headers.map((header) => `<td>${escapeHtml(row[header])}</td>`).join('')}</tr>`).join('')}</tbody></table>` : ''
    return `<section><h2>${escapeHtml(module.label)}</h2><p><b>计算时间：</b>${escapeHtml(record.displayTime)}</p><p><b>数据来源：</b>${escapeHtml(record.sourceName)}</p><p><b>结果摘要：</b>${escapeHtml(record.summary)}</p>${table}</section>`
  }).join('')
  const html = `<!doctype html><html lang="zh-CN"><head><meta charset="UTF-8"><title>瓦斯数据综合报告</title><style>body{margin:0;padding:32px;background:#e8f4fd;color:#1a237e;font-family:"Microsoft YaHei",sans-serif}.container{max-width:1100px;margin:auto}.header{padding:28px 36px;color:#fff;background:linear-gradient(135deg,#1a73e8,#0d47a1)}section{margin-top:16px;padding:22px 28px;background:#fff;border:1px solid #90caf9}h1,h2{margin-top:0}h2{color:#1a73e8;border-bottom:2px solid #e3f2fd;padding-bottom:9px}.opinion{white-space:pre-wrap;line-height:1.85;background:#fafbfc;border-left:4px solid #1a73e8;padding:18px}table{width:100%;margin-top:14px;border-collapse:collapse}th,td{padding:7px 10px;border:1px solid #d5e8f7;text-align:center;font-size:13px}th{color:#1a73e8;background:#e3f2fd}</style></head><body><div class="container"><div class="header"><h1>瓦斯数据综合报告</h1><div>生成时间：${escapeHtml(generatedAt)}</div></div>${sections}<section><h2>瓦斯安全综合评价意见</h2><div class="opinion">${escapeHtml(professionalOpinion.value)}</div></section></div></body></html>`
  const url = URL.createObjectURL(new Blob([html], { type: 'text/html;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = `瓦斯数据综合报告_${timestampForFilename()}.html`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('HTML 综合报告已导出')
}

onMounted(() => {
  refreshHistory()
  removeHistoryListener = onCalculationHistoryUpdated(refreshHistory)
})
onBeforeUnmount(() => removeHistoryListener())
</script>

<style scoped>
.report-page { min-height: calc(100vh - 90px); color: var(--text-primary); }
.report-status-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 14px; }
.report-status-card { display: flex; align-items: center; gap: 12px; min-height: 70px; padding: 12px 16px; background: #fff; border: 1px solid #b9d9ee; }
.report-status-dot { width: 14px; height: 14px; flex: 0 0 14px; border-radius: 50%; background: #bdbdbd; }
.report-status-card.is-ready .report-status-dot { background: #27ae60; box-shadow: 0 0 0 5px rgba(39,174,96,.12); }
.report-status-card strong, .report-status-card small { display: block; }
.report-status-card strong { color: var(--primary-dark); font-size: 15px; }
.report-status-card small { margin-top: 5px; color: var(--text-secondary); }
.report-main-grid { display: grid; grid-template-columns: minmax(560px, 1.55fr) minmax(360px, .9fr); gap: 14px; align-items: start; }
.report-left-column { display: grid; gap: 14px; }
.report-section-card, .report-opinion-card { background: #fff; border: 1px solid #91cdf7; box-shadow: 0 1px 4px rgba(13,71,161,.08); }
.report-section-card__header { display: flex; justify-content: space-between; align-items: center; gap: 15px; padding: 16px 18px; border-bottom: 1px solid #9bd2fa; }
.report-section-card__header h2, .report-opinion-card h2 { margin: 0; color: var(--primary); font-size: 19px; }
.report-section-card__header p { margin: 5px 0 0; color: var(--text-secondary); font-size: 13px; }
.latest-record-heading { display: flex; align-items: center; justify-content: space-between; padding: 15px 18px 8px; color: var(--text-secondary); font-size: 13px; }
.latest-record-summary { margin: 4px 18px 10px; padding: 11px 13px; color: var(--primary-dark); background: #f4faff; border-left: 4px solid var(--primary); line-height: 1.7; }
.latest-record-info { display: flex; flex-wrap: wrap; gap: 9px 24px; padding: 0 18px 12px; color: var(--text-secondary); font-size: 13px; }
.latest-chart-box { height: 390px; margin: 0 18px 18px; border: 1px solid #c7dfef; }
.module-result-title { display: flex; align-items: center; gap: 8px; padding: 13px 17px; color: #fff; background: #1a73e8; font-size: 16px; font-weight: 700; }
.module-result-title--statistics { background: #3498db; }
.module-result-title--detection { background: #ff6b6b; }
.module-result-content { display: grid; grid-template-columns: repeat(2, minmax(180px,1fr)); gap: 10px 22px; padding: 16px 18px; color: var(--text-secondary); font-size: 13px; }
.module-result-content b { color: var(--text-primary); }
.report-opinion-card { position: sticky; top: 0; padding: 18px; }
.report-opinion-card h2 { display: flex; align-items: center; gap: 8px; padding-bottom: 13px; border-bottom: 1px solid #9bd2fa; }
.report-opinion-text { min-height: 250px; max-height: 420px; margin: 14px 0; padding: 15px; overflow-y: auto; white-space: pre-line; color: var(--text-primary); background: #fafbfc; border: 1px solid #e1ebf2; line-height: 1.85; font-size: 14px; }
.report-export-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.report-export-actions .el-button { margin: 0; }
.report-history-title { margin: 18px 0 9px; color: var(--primary); font-weight: 700; }
.is-danger { color: #c62828 !important; font-style: normal; font-weight: 700; }
.is-safe { color: #218739 !important; font-style: normal; font-weight: 700; }
@media (max-width: 1100px) { .report-main-grid { grid-template-columns: 1fr; } .report-opinion-card { position: static; } }
</style>
