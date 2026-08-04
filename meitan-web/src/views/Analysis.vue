<template>
  <div class="draft-workbench">
    <ModulePageHeader title="瓦斯吸附含量计算与分析" />

    <div class="draft-workbench__grid draft-grid--analysis">
      <aside class="draft-scroll-column">
        <h2 class="draft-panel-heading">参数设置</h2>

        <section class="draft-card">
          <div class="draft-card__body">
            <el-upload
              style="width:100%"
              :auto-upload="false"
              :show-file-list="false"
              accept=".xlsx,.xls,.csv"
              :on-change="handleParameterFileChange"
            >
              <el-button type="primary" size="large" style="width:100%">
                <el-icon><FolderOpened /></el-icon>
                加载参数文件
              </el-button>
            </el-upload>
            <div class="draft-file-name">当前文件：{{ currentFile || '正在加载内置参数文件...' }}</div>
          </div>
        </section>

        <section class="draft-card">
          <h3 class="draft-card__title">煤型参数</h3>
          <div class="draft-card__body">
            <el-form label-width="125px" label-position="left">
              <el-form-item label="煤型及编号：">
                <el-select v-model="params.coalType" filterable @change="handleCoalTypeChange">
                  <el-option v-for="coalType in coalTypeOptions" :key="coalType" :label="coalType" :value="coalType" />
                </el-select>
              </el-form-item>
              <el-form-item label="挥发分 (%)：">
                <el-select v-model="params.volatile" @change="applySelectedRecord">
                  <el-option v-for="value in volatileOptions" :key="value" :label="String(value)" :value="value" />
                </el-select>
              </el-form-item>
              <el-form-item label="温度 (°C)：">
                <el-input-number v-model="params.temperature" :controls="false" :min="0" :max="100" />
              </el-form-item>
              <el-form-item label="含水率 (%)：">
                <el-input-number v-model="params.waterContent" :controls="false" :min="0" :max="50" :step="0.1" />
              </el-form-item>
              <el-form-item label="Vl值 (cm³/g)：">
                <el-input-number v-model="params.vl" :controls="false" :min="0" :step="0.1" />
              </el-form-item>
              <el-form-item label="Pl值 (MPa)：">
                <el-input-number v-model="params.pl" :controls="false" :min="0" :step="0.1" />
              </el-form-item>
            </el-form>
          </div>
        </section>

        <section class="draft-card">
          <h3 class="draft-card__title">计算参数</h3>
          <div class="draft-card__body">
            <el-form label-width="135px" label-position="left">
              <el-form-item label="参考温度 (°C)：">
                <el-input-number v-model="params.referenceTemp" :controls="false" :min="0" />
              </el-form-item>
              <el-form-item label="最小压力 (MPa)：">
                <el-input-number v-model="params.pMin" :controls="false" :min="0" :step="0.5" />
              </el-form-item>
              <el-form-item label="最大压力 (MPa)：">
                <el-input-number v-model="params.pMax" :controls="false" :min="0" :step="1" />
              </el-form-item>
              <el-form-item label="压力步长 (MPa)：">
                <el-input-number v-model="params.pStep" :controls="false" :min="0.01" :step="0.05" :precision="2" />
              </el-form-item>
            </el-form>
          </div>
        </section>

        <section class="draft-card">
          <h3 class="draft-card__title">多曲线对比</h3>
          <div class="draft-card__body draft-action-grid">
            <el-button type="info" @click="addToComparison">
              <el-icon><EditPen /></el-icon>
              添加到对比
            </el-button>
            <el-button type="primary" plain @click="comparisonDialogVisible = true">
              <el-icon><List /></el-icon>
              管理对比曲线
            </el-button>
          </div>
        </section>

        <section class="draft-card">
          <h3 class="draft-card__title">操作控制</h3>
          <div class="draft-card__body">
            <el-button type="success" size="large" :loading="loading" style="width:100%" @click="handleCalculate">
              <el-icon><VideoPlay /></el-icon>
              开始计算并绘图
            </el-button>
            <p class="draft-muted-text" style="margin-top:10px;text-align:center">设置参数后生成吸附曲线及计算结果</p>
            <div class="draft-action-grid" style="margin-top:12px">
              <el-button type="warning" @click="resetParams">
                <el-icon><RefreshLeft /></el-icon>
                重置参数
              </el-button>
              <el-button type="primary" plain :loading="loading" @click="quickCalculate">
                <el-icon><Lightning /></el-icon>
                快速计算
              </el-button>
            </div>
          </div>
        </section>
      </aside>

      <section class="draft-card draft-card--fill">
        <h3 class="draft-card__title">吸附曲线分析图</h3>
        <div class="draft-toolbar">
          <span class="draft-muted-text">图表样式：</span>
          <el-select v-model="params.chartStyle" style="width:120px">
            <el-option label="曲线图" value="curve" />
            <el-option label="散点图" value="scatter" />
            <el-option label="柱状图" value="bar" />
            <el-option label="面积图" value="area" />
          </el-select>
          <el-button type="primary" :loading="loading" @click="redrawChart">
            <el-icon><Refresh /></el-icon>
            重绘
          </el-button>
          <el-button type="warning" @click="clearPlot">
            <el-icon><Delete /></el-icon>
            清空
          </el-button>
          <el-button type="success" :disabled="!chartImage" @click="saveChart">
            <el-icon><Download /></el-icon>
            保存图表
          </el-button>
          <span class="draft-toolbar__spacer"></span>
          <span class="draft-counter">对比曲线：{{ comparisonCurves.length }}</span>
        </div>
        <div class="draft-chart-stage">
          <div v-if="chartImage" class="draft-chart-image-wrap">
            <ClickableChartImage :image="chartImage" alt="瓦斯吸附分析曲线" />
          </div>
          <DraftChartPlaceholder
            v-else
            title="煤层瓦斯吸附定量分析图"
            x-label="气体压力 P (MPa)"
            y-label="气体吸附量 Vm (cm³/g)"
            hint="请设置参数并点击开始计算按钮"
          />
        </div>
      </section>

      <section class="draft-card draft-card--fill">
        <h3 class="draft-card__title">计算结果</h3>
        <div class="draft-toolbar">
          <el-button type="primary" :disabled="!resultData" @click="exportData">
            <el-icon><Upload /></el-icon>
            导出数据
          </el-button>
          <el-button type="success" :disabled="!chartImage" @click="saveChart">
            <el-icon><Download /></el-icon>
            保存图表
          </el-button>
        </div>
        <div class="draft-result-table">
          <el-table :data="tableData" height="100%" border size="small" empty-text="暂无计算数据">
            <el-table-column type="index" label="序号" width="62" align="center" />
            <el-table-column prop="pressure" label="气体压力P (MPa)" min-width="135" align="center" />
            <el-table-column prop="vm" label="气体吸附量Vm (cm³/g)" min-width="165" align="center" />
          </el-table>
        </div>
        <div class="draft-stats-box">
          <div class="draft-stats-box__title">统计信息</div>
          <div v-if="resultData" class="draft-stats-list">
            <span>数据点：{{ resultData.stats?.data_points }}</span>
            <span>压力范围：{{ resultData.stats?.pressure_range }}</span>
            <span>最大吸附量：{{ resultData.stats?.max_vm }}</span>
            <span>最小吸附量：{{ resultData.stats?.min_vm }}</span>
            <span>平均吸附量：{{ resultData.stats?.avg_vm }}</span>
            <span>煤型：{{ resultData.coal_type }}</span>
          </div>
          <div v-else class="draft-muted-text">暂无统计数据</div>
        </div>
      </section>
    </div>

    <div class="draft-status-bar">{{ statusText }}</div>

    <el-dialog v-model="comparisonDialogVisible" title="对比曲线管理" width="720px">
      <el-table :data="comparisonCurves" border empty-text="暂无对比曲线">
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="label" label="曲线名称" />
        <el-table-column label="数据点" width="90">
          <template #default="{ row }">{{ row.p_array.length }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ $index }">
            <el-button type="danger" text @click="removeComparison($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button type="warning" :disabled="!comparisonCurves.length" @click="clearComparisons">清空全部</el-button>
        <el-button type="primary" @click="comparisonDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import * as XLSX from 'xlsx'
import { calcAnalysis } from '@/api'
import DraftChartPlaceholder from '@/components/DraftChartPlaceholder.vue'
import ClickableChartImage from '@/components/ClickableChartImage.vue'
import ModulePageHeader from '@/components/ModulePageHeader.vue'
import { parseAnalysisWorkbook } from '@/utils/excelData'
import { downloadBase64Png, timestampForFilename } from '@/utils/download'
import { ElMessage } from 'element-plus'
import { saveCalculationRecord } from '@/utils/calculationHistory'
import '@/assets/styles/workbench.css'

const loading = ref(false)
const chartImage = ref('')
const resultData = ref(null)
const currentFile = ref('')
const statusText = ref('就绪 - 正在加载内置参数文件')
const analysisRecords = ref([])
const coalTypeOptions = ref([])
const volatileOptions = ref([])
const comparisonCurves = ref([])
const comparisonDialogVisible = ref(false)

const params = reactive({
  coalType: '',
  volatile: undefined,
  temperature: 20,
  waterContent: 0,
  vl: 0,
  pl: 0,
  referenceTemp: 25,
  pMin: 1,
  pMax: 16,
  pStep: 0.1,
  chartStyle: 'curve',
})

const tableData = computed(() => {
  const pArray = resultData.value?.p_array || []
  const vmArray = resultData.value?.vm_array || []
  return pArray.map((pressure, index) => ({ pressure, vm: vmArray[index] }))
})

function setNumericParam(key, value) {
  if (Number.isFinite(value)) params[key] = value
}

function applySelectedRecord() {
  const record = analysisRecords.value.find((item) =>
    item.coalType === params.coalType && Number(item.volatile) === Number(params.volatile)
  )
  if (!record) return
  setNumericParam('temperature', record.temperature)
  setNumericParam('waterContent', record.waterContent)
  setNumericParam('vl', record.vl)
  setNumericParam('pl', record.pl)
}

function handleCoalTypeChange() {
  volatileOptions.value = [...new Set(
    analysisRecords.value
      .filter((item) => item.coalType === params.coalType && Number.isFinite(item.volatile))
      .map((item) => item.volatile)
  )].sort((a, b) => a - b)
  if (!volatileOptions.value.some((value) => Number(value) === Number(params.volatile))) {
    params.volatile = volatileOptions.value[0]
  }
  applySelectedRecord()
}

function applyParameterRecords(records, filename, showMessage = true) {
  analysisRecords.value = records
  coalTypeOptions.value = [...new Set(records.map((item) => item.coalType))]
  params.coalType = coalTypeOptions.value.includes(params.coalType) ? params.coalType : coalTypeOptions.value[0]
  currentFile.value = filename
  handleCoalTypeChange()
  statusText.value = `已加载：${filename}（可选择煤型及编号、挥发分）`
  if (showMessage) ElMessage.success(`参数文件加载成功，共 ${records.length} 条数据`)
}

async function loadParameterFile(file, showMessage = true) {
  try {
    applyParameterRecords(parseAnalysisWorkbook(await file.arrayBuffer()), file.name, showMessage)
  } catch (error) {
    ElMessage.error(error.message || '参数文件解析失败，请检查文件格式')
  }
}

function handleParameterFileChange(file) {
  if (file.raw) loadParameterFile(file.raw)
}

async function loadBuiltInParameters() {
  try {
    const response = await fetch('/data/data(3).xlsx')
    if (!response.ok) throw new Error('内置参数文件读取失败')
    applyParameterRecords(parseAnalysisWorkbook(await response.arrayBuffer()), 'data(3).xlsx', false)
  } catch (error) {
    statusText.value = '内置参数文件加载失败，请手动加载参数文件'
    ElMessage.warning(error.message || '内置参数文件加载失败')
  }
}

async function handleCalculate() {
  if (!params.coalType || params.volatile === undefined) {
    ElMessage.warning('请先选择煤型及编号和挥发分')
    return
  }
  loading.value = true
  statusText.value = '正在计算吸附曲线，请稍候...'
  try {
    const res = await calcAnalysis({ ...params, comparisonCurves: comparisonCurves.value })
    resultData.value = res.data
    chartImage.value = res.data.chart_image_base64 || ''
    saveCalculationRecord({
      moduleType: 'analysis',
      sourceName: currentFile.value || '内置参数文件',
      params: { ...params },
      result: res.data,
      chartImage: chartImage.value,
      summary: `吸附量范围：${res.data.stats?.min_vm ?? '-'} ~ ${res.data.stats?.max_vm ?? '-'}，平均值：${res.data.stats?.avg_vm ?? '-'}`,
    })
    statusText.value = `计算完成 - ${res.data.stats?.data_points || 0} 个数据点`
    ElMessage.success('计算完成')
  } catch (error) {
    statusText.value = '计算失败，请检查计算服务和参数设置'
  } finally {
    loading.value = false
  }
}

function redrawChart() {
  if (!resultData.value) {
    ElMessage.warning('请先完成一次计算')
    return
  }
  handleCalculate()
}

function clearPlot() {
  chartImage.value = ''
  resultData.value = null
  statusText.value = '图表已清空'
}

function resetParams() {
  params.referenceTemp = 25
  params.pMin = 1
  params.pMax = 16
  params.pStep = 0.1
  params.chartStyle = 'curve'
  params.coalType = coalTypeOptions.value[0] || ''
  handleCoalTypeChange()
  clearPlot()
  statusText.value = '参数已重置'
}

function quickCalculate() {
  params.pMin = 1
  params.pMax = 16
  params.pStep = 0.1
  handleCalculate()
}

function addToComparison() {
  if (!resultData.value) {
    ElMessage.warning('请先完成计算，再添加到对比')
    return
  }
  comparisonCurves.value.push({
    label: `${params.coalType} | T=${params.temperature}°C, Vdaf=${params.volatile}%`,
    p_array: [...(resultData.value.p_array || [])],
    vm_array: [...(resultData.value.vm_array || [])],
  })
  statusText.value = `已添加到对比，共 ${comparisonCurves.value.length} 条曲线`
  ElMessage.success('已添加到对比曲线')
}

function removeComparison(index) {
  comparisonCurves.value.splice(index, 1)
  statusText.value = `已删除选中曲线，剩余 ${comparisonCurves.value.length} 条`
}

function clearComparisons() {
  comparisonCurves.value = []
  statusText.value = '已清空全部对比曲线'
}

function saveChart() {
  if (!downloadBase64Png(chartImage.value, `吸附曲线_${timestampForFilename()}.png`)) {
    ElMessage.warning('没有图表可以保存')
    return
  }
  statusText.value = '图表已保存'
}

function exportData() {
  if (!resultData.value) {
    ElMessage.warning('没有数据可以导出')
    return
  }
  const workbook = XLSX.utils.book_new()
  const resultRows = tableData.value.map((row) => ({
    '气体压力P(MPa)': row.pressure,
    '气体吸附量Vm(cm³/g)': row.vm,
  }))
  XLSX.utils.book_append_sheet(workbook, XLSX.utils.json_to_sheet(resultRows), '计算结果')
  XLSX.utils.book_append_sheet(workbook, XLSX.utils.aoa_to_sheet([
    ['参数', '值'],
    ['煤型及编号', params.coalType],
    ['挥发分(%)', params.volatile],
    ['温度(°C)', params.temperature],
    ['含水率(%)', params.waterContent],
    ['Vl值(cm³/g)', params.vl],
    ['Pl值(MPa)', params.pl],
    ['参考温度(°C)', params.referenceTemp],
  ]), '参数信息')
  comparisonCurves.value.forEach((curve, index) => {
    const rows = curve.p_array.map((pressure, pointIndex) => ({
      '气体压力P(MPa)': pressure,
      '气体吸附量Vm(cm³/g)': curve.vm_array[pointIndex],
    }))
    XLSX.utils.book_append_sheet(workbook, XLSX.utils.json_to_sheet(rows), `对比曲线${index + 1}`)
  })
  XLSX.writeFile(workbook, `吸附数据_${timestampForFilename()}.xlsx`)
  statusText.value = '计算数据已导出'
}

onMounted(loadBuiltInParameters)
</script>
