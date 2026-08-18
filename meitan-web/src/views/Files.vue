<template>
  <div class="history-page">
    <ModulePageHeader title="数据文件管理" />

    <section class="history-summary-grid">
      <div class="history-summary-card">
        <div class="history-summary-card__icon history-summary-card__icon--all"><el-icon><Files /></el-icon></div>
        <div><strong>{{ history.length }}</strong><span>全部计算记录</span></div>
      </div>
      <div class="history-summary-card">
        <div class="history-summary-card__icon"><el-icon><TrendCharts /></el-icon></div>
        <div><strong>{{ moduleCount('analysis') }}</strong><span>吸附含量分析</span></div>
      </div>
      <div class="history-summary-card">
        <div class="history-summary-card__icon history-summary-card__icon--green"><el-icon><DataAnalysis /></el-icon></div>
        <div><strong>{{ moduleCount('statistics') }}</strong><span>参数统计分析</span></div>
      </div>
      <div class="history-summary-card">
        <div class="history-summary-card__icon history-summary-card__icon--orange"><el-icon><WarningFilled /></el-icon></div>
        <div><strong>{{ moduleCount('detection') }}</strong><span>突出危险性检测</span></div>
      </div>
    </section>

    <section class="history-card">
      <div class="history-card__header">
        <div>
          <h2>计算数据记录</h2>
          <p>三个分析模块计算完成后自动保存，按最新计算时间排列</p>
        </div>
        <div class="history-actions">
          <el-select v-model="moduleFilter" style="width:190px">
            <el-option label="全部模块" value="all" />
            <el-option label="吸附含量分析" value="analysis" />
            <el-option label="参数统计分析" value="statistics" />
            <el-option label="危险性检测" value="detection" />
          </el-select>
          <el-button type="primary" @click="refreshHistory"><el-icon><Refresh /></el-icon>刷新</el-button>
          <el-button type="danger" :disabled="!history.length" @click="clearAll">
            <el-icon><Delete /></el-icon>清除全部
          </el-button>
        </div>
      </div>

      <el-table :data="filteredHistory" border stripe height="calc(100vh - 365px)" empty-text="暂无计算记录，请先前往分析模块完成计算">
        <el-table-column type="index" label="序号" width="62" align="center" />
        <el-table-column label="计算模块" min-width="185">
          <template #default="{ row }">
            <el-tag :type="moduleColor(row.moduleType)" effect="light">{{ shortModuleLabel(row.moduleType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sourceName" label="数据来源" min-width="170" show-overflow-tooltip />
        <el-table-column label="数据点" width="92" align="center">
          <template #default="{ row }">{{ dataPointCount(row) }}</template>
        </el-table-column>
        <el-table-column prop="summary" label="结果摘要" min-width="280" show-overflow-tooltip />
        <el-table-column prop="displayTime" label="计算时间" width="190" />
        <el-table-column label="操作" width="228" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="openDetail(row)">查看</el-button>
            <el-button type="success" size="small" text @click="exportRecord(row)">导出</el-button>
            <el-button type="danger" size="small" text @click="removeRecord(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="detailVisible" :title="selectedRecord?.moduleLabel || '计算记录详情'" width="88%" top="4vh" destroy-on-close>
      <template v-if="selectedRecord">
        <div class="record-detail-meta">
          <span><b>计算时间：</b>{{ selectedRecord.displayTime }}</span>
          <span><b>数据来源：</b>{{ selectedRecord.sourceName || '-' }}</span>
          <span><b>结果摘要：</b>{{ selectedRecord.summary || '-' }}</span>
        </div>

        <div class="record-detail-grid">
          <section class="record-detail-panel">
            <h3>计算参数</h3>
            <el-table :data="parameterRows" border size="small" max-height="330">
              <el-table-column prop="label" label="参数" min-width="150" />
              <el-table-column prop="value" label="数值" min-width="180" show-overflow-tooltip />
            </el-table>
          </section>
          <section v-if="selectedRecord.chartImage" class="record-detail-panel record-chart-panel">
            <h3>分析图表（点击可放大）</h3>
            <div class="record-chart-box">
              <ClickableChartImage :image="selectedRecord.chartImage" :alt="selectedRecord.moduleLabel" />
            </div>
          </section>
        </div>

        <section class="record-detail-panel record-result-panel">
          <h3>计算结果表</h3>
          <el-table :data="detailRows" border stripe max-height="360" empty-text="该记录没有明细数组">
            <el-table-column v-for="column in detailColumns" :key="column.prop" :prop="column.prop" :label="column.label" min-width="130" align="center" />
          </el-table>
        </section>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="success" @click="selectedRecord && exportRecord(selectedRecord)">导出 XLSX</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as XLSX from 'xlsx'
import { ElMessage, ElMessageBox } from 'element-plus'
import ModulePageHeader from '@/components/ModulePageHeader.vue'
import ClickableChartImage from '@/components/ClickableChartImage.vue'
import {
  clearCalculationHistory,
  getCalculationHistory,
  onCalculationHistoryUpdated,
  removeCalculationRecord,
} from '@/utils/calculationHistory'
import { timestampForFilename } from '@/utils/download'
import '@/assets/styles/workbench.css'

const history = ref([])
const moduleFilter = ref('all')
const detailVisible = ref(false)
const selectedRecord = ref(null)
let removeHistoryListener = () => {}

const filteredHistory = computed(() => moduleFilter.value === 'all'
  ? history.value
  : history.value.filter((item) => item.moduleType === moduleFilter.value))

const moduleCount = (moduleType) => history.value.filter((item) => item.moduleType === moduleType).length
const moduleColor = (moduleType) => ({ analysis: 'primary', statistics: 'success', detection: 'warning' }[moduleType] || 'info')
const shortModuleLabel = (moduleType) => ({ analysis: '吸附含量分析', statistics: '参数统计分析', detection: '危险性检测' }[moduleType] || moduleType)

function refreshHistory() {
  history.value = getCalculationHistory()
}

function dataPointCount(record) {
  if (record.moduleType === 'analysis') return record.result?.p_array?.length || 0
  if (record.moduleType === 'statistics') return Math.max((record.inputData?.length || 1) - 1, 0)
  return record.result?.p_array?.length || 0
}

function displayValue(value) {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value === 'boolean') return value ? '是' : '否'
  if (Array.isArray(value)) return `${value.length} 项数据`
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function displayParameterValue(key, value) {
  if (key === 'regionFilter' && Array.isArray(value)) return value.length ? value.join('、') : '全部地区'
  return displayValue(value)
}

const parameterRows = computed(() => Object.entries(selectedRecord.value?.params || {}).map(([key, value]) => ({
  label: parameterLabels[key] || key,
  value: displayParameterValue(key, value),
})))

const parameterLabels = {
  coalType: '煤样编号', volatile: '挥发分 (%)', temperature: '温度 (°C)', waterContent: '含水率 (%)',
  vl: 'Vl值', pl: 'Pl值', pMin: '最小压力 (MPa)', pMax: '最大压力 (MPa)', pStep: '压力步长 (MPa)',
  chartType: '图表类型', xAxis: 'X轴', yAxis: 'Y轴', colorBy: '颜色编码', sizeBy: '大小编码', regionFilter: '地区筛选', volatileFilter: '挥发分筛选',
  volume: '孔隙容积', compressFactor: '压缩系数', measuredPressure: '实测压力值', measuredContent: '实测瓦斯含量值（旧记录）', calculatedContent: '瓦斯含量计算值 (m³/t)', critContent: '瓦斯含量临界值 (m³/t)',
}

const detailColumns = computed(() => {
  if (selectedRecord.value?.moduleType === 'analysis') return [
    { prop: 'index', label: '序号' }, { prop: 'pressure', label: '压力 P (MPa)' }, { prop: 'vm', label: '吸附量 Vm (cm³/g)' },
  ]
  if (selectedRecord.value?.moduleType === 'detection') return [
    { prop: 'index', label: '序号' }, { prop: 'pressure', label: '压力 P (MPa)' }, { prop: 'xx', label: '吸附瓦斯 Xx' }, { prop: 'xy', label: '游离瓦斯 Xy' }, { prop: 'q', label: '总瓦斯 Q' },
  ]
  const headers = selectedRecord.value?.inputData?.[0] || []
  return headers.slice(0, 10).map((header, index) => ({ prop: `column${index}`, label: String(header || `列${index + 1}`) }))
})

const detailRows = computed(() => {
  const record = selectedRecord.value
  if (!record) return []
  if (record.moduleType === 'analysis') return (record.result?.p_array || []).map((pressure, index) => ({ index: index + 1, pressure, vm: record.result?.vm_array?.[index] }))
  if (record.moduleType === 'detection') return (record.result?.p_array || []).map((pressure, index) => ({
    index: index + 1,
    pressure,
    xx: record.inputData?.xxArray?.[index],
    xy: record.result?.xy_array?.[index],
    q: record.result?.q_array?.[index],
  }))
  return (record.inputData || []).slice(1, 101).map((row) => Object.fromEntries(row.slice(0, 10).map((value, index) => [`column${index}`, value])))
})

function openDetail(record) {
  selectedRecord.value = record
  detailVisible.value = true
}

async function removeRecord(record) {
  try {
    await ElMessageBox.confirm(`确定删除 ${record.displayTime} 的计算记录？`, '删除记录', { type: 'warning' })
    removeCalculationRecord(record.id)
    refreshHistory()
    ElMessage.success('计算记录已删除')
  } catch {}
}

async function clearAll() {
  try {
    await ElMessageBox.confirm('确定清除全部计算记录？此操作不可撤销。', '清除全部', { type: 'warning', confirmButtonText: '确定清除' })
    clearCalculationHistory()
    refreshHistory()
    ElMessage.success('全部计算记录已清除')
  } catch {}
}

function exportRecord(record) {
  selectedRecord.value = record
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, XLSX.utils.aoa_to_sheet([
    ['项目', '内容'],
    ['计算模块', record.moduleLabel],
    ['计算时间', record.displayTime],
    ['数据来源', record.sourceName],
    ['结果摘要', record.summary],
    ...Object.entries(record.params || {}).map(([key, value]) => [parameterLabels[key] || key, displayParameterValue(key, value)]),
  ]), '记录信息')
  if (detailRows.value.length) XLSX.utils.book_append_sheet(workbook, XLSX.utils.json_to_sheet(detailRows.value), '计算结果')
  XLSX.writeFile(workbook, `${shortModuleLabel(record.moduleType)}_${timestampForFilename()}.xlsx`)
  ElMessage.success('计算记录已导出')
}

onMounted(() => {
  refreshHistory()
  removeHistoryListener = onCalculationHistoryUpdated(refreshHistory)
})
onBeforeUnmount(() => removeHistoryListener())
</script>

<style scoped>
.history-page { min-height: calc(100vh - 90px); color: var(--text-primary); }
.history-summary-grid { display: grid; grid-template-columns: repeat(4, minmax(190px, 1fr)); gap: 14px; margin-bottom: 14px; }
.history-summary-card { display: flex; align-items: center; gap: 15px; min-height: 86px; padding: 14px 18px; background: #fff; border: 1px solid #91cdf7; box-shadow: 0 1px 4px rgba(13,71,161,.08); }
.history-summary-card__icon { width: 48px; height: 48px; display: grid; place-items: center; color: #fff; font-size: 24px; border-radius: 8px; background: #1a73e8; }
.history-summary-card__icon--all { background: #546e7a; }
.history-summary-card__icon--green { background: #27ae60; }
.history-summary-card__icon--orange { background: #f39c12; }
.history-summary-card strong { display: block; color: var(--primary-dark); font-size: 25px; line-height: 1.2; }
.history-summary-card span { color: var(--text-secondary); font-size: 13px; }
.history-card { background: #fff; border: 1px solid #91cdf7; }
.history-card__header { display: flex; align-items: center; justify-content: space-between; gap: 18px; padding: 16px 18px; border-bottom: 1px solid #9bd2fa; }
.history-card__header h2 { margin: 0 0 5px; color: var(--primary); font-size: 20px; }
.history-card__header p { margin: 0; color: var(--text-secondary); font-size: 13px; }
.history-actions { display: flex; align-items: center; gap: 9px; }
.record-detail-meta { display: flex; flex-wrap: wrap; gap: 12px 28px; padding: 12px 15px; margin-bottom: 14px; color: var(--text-secondary); background: #f4faff; border: 1px solid #b8dcf7; }
.record-detail-grid { display: grid; grid-template-columns: minmax(300px, .8fr) minmax(460px, 1.4fr); gap: 14px; }
.record-detail-panel { padding: 14px; border: 1px solid #b8dcf7; background: #fff; }
.record-detail-panel h3 { margin: 0 0 12px; color: var(--primary); font-size: 17px; }
.record-chart-box { height: 330px; border: 1px solid #d4e7f5; }
.record-result-panel { margin-top: 14px; }
@media (max-width: 1100px) { .history-summary-grid { grid-template-columns: repeat(2, 1fr); } .record-detail-grid { grid-template-columns: 1fr; } }
</style>
