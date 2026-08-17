<template>
  <div class="draft-workbench">
    <ModulePageHeader title="煤层瓦斯吸附参数统计" />

    <div class="draft-workbench__grid draft-grid--statistics">
      <aside class="draft-scroll-column">
        <section class="draft-card">
          <div class="draft-card__body">
            <div class="draft-file-actions">
              <el-upload
                :auto-upload="false"
                :show-file-list="false"
                accept=".xlsx,.xls,.csv"
                :on-change="handleFileChange"
              >
                <el-button type="primary" size="large">
                  <el-icon><FolderOpened /></el-icon>
                  加载数据文件
                </el-button>
              </el-upload>
              <el-button tag="a" :href="statisticsTemplateUrl" download="煤层瓦斯吸附参数统计样板.xlsx" size="large" plain>
                <el-icon><Download /></el-icon>
                样板文件
              </el-button>
            </div>
            <div class="draft-file-name">当前文件：{{ currentFile || '请加载数据文件' }}</div>
          </div>
        </section>

        <section class="draft-card">
          <h3 class="draft-card__title">图表参数</h3>
          <div class="draft-card__body">
            <el-form label-width="100px" label-position="left">
              <el-form-item label="图表类型：">
                <el-select v-model="params.chartType">
                  <el-option label="散点图" value="scatter" />
                  <el-option label="双坐标轴图" value="dual_axis" />
                  <el-option label="分组图" value="grouped" />
                </el-select>
              </el-form-item>
              <el-form-item label="地区比较：">
                <el-select
                  v-model="params.regionFilter"
                  multiple
                  collapse-tags
                  collapse-tags-tooltip
                  filterable
                  clearable
                  placeholder="不选择则显示全部地区"
                >
                  <el-option v-for="region in regionOptions" :key="region" :label="region" :value="region" />
                </el-select>
              </el-form-item>
              <el-form-item label="挥发分：">
                <el-select v-model="params.volatileFilter" filterable>
                  <el-option label="全部挥发分" value="全部" />
                  <el-option v-for="value in volatileOptions" :key="value" :label="String(value)" :value="String(value)" />
                </el-select>
              </el-form-item>
              <el-form-item label="X轴：">
                <el-select v-model="params.xAxis">
                  <el-option v-for="column in xAxisOptions" :key="column" :label="statisticsAxisLabel(column)" :value="column" />
                </el-select>
              </el-form-item>
              <el-form-item label="Y轴：">
                <el-select v-model="params.yAxis">
                  <el-option v-for="column in yAxisOptions" :key="column" :label="statisticsAxisLabel(column)" :value="column" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="params.chartType === 'scatter'" label="颜色：">
                <el-select v-model="params.colorBy" filterable>
                  <el-option label="无（默认颜色）" value="无" />
                  <el-option v-for="column in allColumns" :key="column" :label="column" :value="column" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="params.chartType === 'scatter'" label="大小：">
                <el-select v-model="params.sizeBy" filterable>
                  <el-option label="无（默认大小）" value="无" />
                  <el-option v-for="column in numericColumns" :key="column" :label="column" :value="column" />
                </el-select>
              </el-form-item>
            </el-form>
          </div>
        </section>

        <section class="draft-card">
          <h3 class="draft-card__title">操作控制</h3>
          <div class="draft-card__body">
            <el-button type="success" size="large" :loading="loading" style="width:100%" @click="handleAnalyze">
              <el-icon><DataAnalysis /></el-icon>
              生成图表
            </el-button>
            <div class="draft-action-grid" style="margin-top:12px">
              <el-button type="warning" @click="clearChart">
                <el-icon><Delete /></el-icon>
                清空图表
              </el-button>
              <el-button type="primary" :disabled="!chartImage" @click="saveChart">
                <el-icon><Download /></el-icon>
                导出图表
              </el-button>
            </div>
            <el-button type="info" style="width:100%" :disabled="!fileData" @click="exportRawData">
              <el-icon><Document /></el-icon>
              导出原始数据
            </el-button>
          </div>
        </section>
      </aside>

      <section class="draft-card draft-card--fill">
        <h3 class="draft-card__title">煤样参数分布图</h3>
        <div class="draft-toolbar">
          <el-button type="primary" :loading="loading" @click="handleAnalyze">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button type="success" :disabled="!chartImage" @click="saveChart">
            <el-icon><Download /></el-icon>
            保存
          </el-button>
          <span class="draft-toolbar__spacer"></span>
          <span class="draft-counter">数据点：{{ chartPointCount }}</span>
        </div>
        <div class="draft-chart-stage">
          <div v-if="chartImage" class="draft-chart-image-wrap">
            <ClickableChartImage :image="chartImage" alt="煤样参数分布图" />
          </div>
          <DraftChartPlaceholder
            v-else
            title="煤样参数分布图"
            :x-label="statisticsAxisLabel(params.xAxis)"
            :y-label="statisticsAxisLabel(params.yAxis)"
            hint="请选择参数后点击生成图表按钮"
          />
        </div>
        <div v-if="summary" class="draft-stats-box">
          <div class="draft-stats-box__title">统计摘要</div>
          <div class="draft-muted-text">{{ summary }}</div>
        </div>
      </section>
    </div>

    <div class="draft-status-bar">{{ statusText }}</div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import * as XLSX from 'xlsx'
import { calcStatistics } from '@/api'
import DraftChartPlaceholder from '@/components/DraftChartPlaceholder.vue'
import ClickableChartImage from '@/components/ClickableChartImage.vue'
import ModulePageHeader from '@/components/ModulePageHeader.vue'
import { parseStatisticsWorkbook } from '@/utils/excelData'
import { downloadBase64Png, timestampForFilename } from '@/utils/download'
import { ElMessage } from 'element-plus'
import { saveCalculationRecord } from '@/utils/calculationHistory'
import { statisticsAxisLabel } from '@/utils/localGasCalculation'
import '@/assets/styles/workbench.css'

const loading = ref(false)
const chartImage = ref('')
const summary = ref('')
const fileData = ref(null)
const currentFile = ref('')
const statusText = ref('就绪 - 请加载数据文件')
const chartPointCount = ref(0)
const regionOptions = ref([])
const volatileOptions = ref([])
const allColumns = ref([])
const numericColumns = ref([])
const statisticsTemplateUrl = `/data/${encodeURIComponent('地区数据yyy(1).xlsx')}`
const xAxisOptions = ['挥发分', '水分']
const yAxisOptions = ['VL值', 'PL值']

const params = reactive({
  chartType: 'scatter',
  xAxis: '挥发分',
  yAxis: 'VL值',
  colorBy: '无',
  sizeBy: '无',
  regionFilter: [],
  volatileFilter: '全部',
})

function chooseAvailable(currentValue, preferredValue, options) {
  if (options.includes(currentValue)) return currentValue
  if (options.includes(preferredValue)) return preferredValue
  return options[0] || ''
}

function applyStatisticsData(parsed, filename, showMessage = true) {
  fileData.value = parsed.fileData
  currentFile.value = filename
  regionOptions.value = parsed.regions
  volatileOptions.value = parsed.volatileValues
  allColumns.value = parsed.headers.filter(Boolean)
  numericColumns.value = parsed.numericColumns
  params.regionFilter = []
  params.volatileFilter = '全部'
  params.xAxis = chooseAvailable(params.xAxis, '挥发分', xAxisOptions)
  params.yAxis = chooseAvailable(params.yAxis, 'VL值', yAxisOptions)
  params.colorBy = chooseAvailable(params.colorBy, '无', ['无', ...allColumns.value])
  params.sizeBy = chooseAvailable(params.sizeBy, '无', ['无', ...numericColumns.value])
  chartPointCount.value = 0
  statusText.value = `已加载：${filename}（${parsed.fileData.length - 1} 条记录）`
  if (showMessage) ElMessage.success(`地区数据加载成功，共 ${parsed.fileData.length - 1} 条记录`)
}

async function loadStatisticsFile(file, showMessage = true) {
  try {
    applyStatisticsData(parseStatisticsWorkbook(await file.arrayBuffer()), file.name, showMessage)
  } catch (error) {
    ElMessage.error(error.message || '地区数据文件解析失败，请检查文件格式')
  }
}

function handleFileChange(file) {
  if (file.raw) loadStatisticsFile(file.raw)
}

function countFilteredRows() {
  if (!fileData.value?.length) return 0
  const headers = fileData.value[0]
  const regionIndex = headers.indexOf('检索地区')
  const volatileIndex = headers.indexOf('挥发分')
  return fileData.value.slice(1).filter((row) => {
    const selectedRegions = Array.isArray(params.regionFilter) ? params.regionFilter : params.regionFilter === '全部' ? [] : [params.regionFilter]
    const regionMatches = selectedRegions.length === 0 || selectedRegions.includes(String(row[regionIndex]))
    const volatileMatches = params.volatileFilter === '全部' || Number(row[volatileIndex]) === Number(params.volatileFilter)
    return regionMatches && volatileMatches
  }).length
}

async function handleAnalyze() {
  if (!fileData.value) {
    ElMessage.warning('请先加载 Excel 地区数据文件')
    return
  }
  loading.value = true
  statusText.value = '正在生成统计图表，请稍候...'
  try {
    const res = await calcStatistics({ ...params, fileData: fileData.value })
    chartImage.value = res.data.chart_image_base64 || ''
    summary.value = res.data.stats_summary || ''
    chartPointCount.value = countFilteredRows()
    saveCalculationRecord({
      moduleType: 'statistics',
      sourceName: currentFile.value || '地区数据',
      params: { ...params },
      result: res.data,
      chartImage: chartImage.value,
      inputData: fileData.value,
      summary: summary.value,
    })
    statusText.value = `图表已生成 - ${chartPointCount.value} 个数据点`
    ElMessage.success('图表生成完成')
  } catch (error) {
    statusText.value = '图表生成失败，请检查筛选条件和计算服务'
  } finally {
    loading.value = false
  }
}

function clearChart() {
  chartImage.value = ''
  summary.value = ''
  chartPointCount.value = 0
  statusText.value = '图表已清空'
}

function saveChart() {
  if (!downloadBase64Png(chartImage.value, `煤样分布图_${timestampForFilename()}.png`)) {
    ElMessage.warning('没有图表可以保存')
    return
  }
  statusText.value = '图表已保存'
}

function exportRawData() {
  if (!fileData.value) {
    ElMessage.warning('没有原始数据可以导出')
    return
  }
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, XLSX.utils.aoa_to_sheet(fileData.value), '煤样数据')
  XLSX.writeFile(workbook, `煤样数据_${timestampForFilename()}.xlsx`)
  statusText.value = '原始数据已导出'
}

</script>
