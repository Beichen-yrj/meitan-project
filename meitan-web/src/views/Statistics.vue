<template>
  <div class="draft-workbench">
    <ModulePageHeader title="煤样瓦斯吸附参数统计分析" />

    <div class="draft-workbench__grid draft-grid--statistics">
      <aside class="draft-scroll-column">
        <section class="draft-card">
          <div class="draft-card__body">
            <el-upload
              style="width:100%"
              :auto-upload="false"
              :show-file-list="false"
              accept=".xlsx,.xls,.csv"
              :on-change="handleFileChange"
            >
              <el-button type="primary" size="large" style="width:100%">
                <el-icon><FolderOpened /></el-icon>
                加载数据文件
              </el-button>
            </el-upload>
            <div class="draft-file-name">当前文件：{{ currentFile || '正在加载内置地区数据...' }}</div>
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
              <el-form-item label="地区筛选：">
                <el-select v-model="params.regionFilter" filterable>
                  <el-option label="全部地区" value="全部" />
                  <el-option v-for="region in filteredRegionOptions" :key="region" :label="region" :value="region" />
                </el-select>
              </el-form-item>
              <el-form-item label="检索地区：">
                <el-input v-model="regionSearch" clearable placeholder="输入关键词筛选地区" />
              </el-form-item>
              <el-form-item label="挥发分：">
                <el-select v-model="params.volatileFilter" filterable>
                  <el-option label="全部挥发分" value="全部" />
                  <el-option v-for="value in volatileOptions" :key="value" :label="String(value)" :value="String(value)" />
                </el-select>
              </el-form-item>
              <el-form-item label="X轴：">
                <el-select v-model="params.xAxis" filterable>
                  <el-option v-for="column in allColumns" :key="column" :label="column" :value="column" />
                </el-select>
              </el-form-item>
              <el-form-item label="Y轴：">
                <el-select v-model="params.yAxis" filterable>
                  <el-option v-for="column in numericColumns" :key="column" :label="column" :value="column" />
                </el-select>
              </el-form-item>
              <el-form-item label="颜色：">
                <el-select v-model="params.colorBy" filterable>
                  <el-option v-for="column in allColumns" :key="column" :label="column" :value="column" />
                </el-select>
              </el-form-item>
              <el-form-item label="大小：">
                <el-select v-model="params.sizeBy" filterable>
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
            x-label="X轴参数"
            y-label="Y轴参数"
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import * as XLSX from 'xlsx'
import { calcStatistics } from '@/api'
import DraftChartPlaceholder from '@/components/DraftChartPlaceholder.vue'
import ClickableChartImage from '@/components/ClickableChartImage.vue'
import ModulePageHeader from '@/components/ModulePageHeader.vue'
import { parseStatisticsWorkbook } from '@/utils/excelData'
import { downloadBase64Png, timestampForFilename } from '@/utils/download'
import { ElMessage } from 'element-plus'
import { saveCalculationRecord } from '@/utils/calculationHistory'
import '@/assets/styles/workbench.css'

const loading = ref(false)
const chartImage = ref('')
const summary = ref('')
const fileData = ref(null)
const currentFile = ref('')
const statusText = ref('就绪 - 正在加载内置地区数据')
const chartPointCount = ref(0)
const regionOptions = ref([])
const volatileOptions = ref([])
const allColumns = ref([])
const numericColumns = ref([])
const regionSearch = ref('')

const params = reactive({
  chartType: 'scatter',
  xAxis: '挥发分',
  yAxis: 'VL值',
  colorBy: '检索地区',
  sizeBy: '挥发分',
  regionFilter: '全部',
  volatileFilter: '全部',
})

const filteredRegionOptions = computed(() => {
  const keyword = regionSearch.value.trim().toLocaleLowerCase('zh-CN')
  if (!keyword) return regionOptions.value
  return regionOptions.value.filter((region) => region.toLocaleLowerCase('zh-CN').includes(keyword))
})

watch(filteredRegionOptions, (options) => {
  if (params.regionFilter !== '全部' && !options.includes(params.regionFilter)) params.regionFilter = '全部'
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
  params.regionFilter = '全部'
  params.volatileFilter = '全部'
  params.xAxis = chooseAvailable(params.xAxis, '挥发分', allColumns.value)
  params.yAxis = chooseAvailable(params.yAxis, 'VL值', numericColumns.value)
  params.colorBy = chooseAvailable(params.colorBy, '检索地区', allColumns.value)
  params.sizeBy = chooseAvailable(params.sizeBy, '挥发分', numericColumns.value)
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

async function loadBuiltInStatistics() {
  try {
    const filename = '地区数据yyy(1).xlsx'
    const response = await fetch(`/data/${encodeURIComponent(filename)}`)
    if (!response.ok) throw new Error('内置地区数据文件读取失败')
    applyStatisticsData(parseStatisticsWorkbook(await response.arrayBuffer()), filename, false)
  } catch (error) {
    statusText.value = '内置地区数据加载失败，请手动加载数据文件'
    ElMessage.warning(error.message || '内置地区数据加载失败')
  }
}

function countFilteredRows() {
  if (!fileData.value?.length) return 0
  const headers = fileData.value[0]
  const regionIndex = headers.indexOf('检索地区')
  const volatileIndex = headers.indexOf('挥发分')
  return fileData.value.slice(1).filter((row) => {
    const regionMatches = params.regionFilter === '全部' || String(row[regionIndex]) === params.regionFilter
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
      sourceName: currentFile.value || '内置地区数据',
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

onMounted(loadBuiltInStatistics)
</script>
