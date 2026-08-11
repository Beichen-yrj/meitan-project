<template>
  <div class="draft-workbench">
    <ModulePageHeader title="煤层区域突出危险性预测" />

    <div class="draft-workbench__grid draft-grid--detection">
      <aside class="draft-scroll-column">
        <section class="draft-card">
          <div class="draft-card__body">
            <el-upload
              style="width:100%"
              :auto-upload="false"
              :show-file-list="false"
              accept=".xlsx,.xls,.csv"
              :on-change="handleAdsorptionFileChange"
            >
              <el-button type="primary" size="large" style="width:100%">
                <el-icon><FolderOpened /></el-icon>
                导入吸附数据 (Xx)
              </el-button>
            </el-upload>
            <div class="draft-file-name">{{ currentFile ? `已加载：${currentFile}` : '未加载吸附数据' }}</div>
            <div v-if="dataInfo" class="draft-muted-text" style="margin-top:6px">{{ dataInfo }}</div>
          </div>
        </section>

        <section class="draft-card">
          <h3 class="draft-card__title">游离瓦斯计算参数</h3>
          <div class="draft-card__body">
            <el-form label-width="165px" label-position="left">
              <el-form-item label="孔隙容积 V (m³/t)：">
                <el-input-number v-model="params.volume" :controls="false" :min="0.0001" :step="0.01" />
              </el-form-item>
              <el-form-item label="温度 t (°C)：">
                <el-input-number v-model="params.temperature" :controls="false" :min="-20" :max="100" />
              </el-form-item>
              <el-form-item label="压缩系数 A：">
                <el-input-number v-model="params.compressFactor" :controls="false" :min="0.1" :step="0.1" />
              </el-form-item>
            </el-form>
            <div class="draft-muted-text">标准状况：T₀=273.2K，P₀=0.101325MPa</div>
          </div>
        </section>

        <section class="draft-card">
          <h3 class="draft-card__title">区域突出危险性预测临界值</h3>
          <div class="draft-card__body">
            <el-form label-width="165px" label-position="left">
              <el-form-item label="压力临界值 (MPa)：">
                <el-input-number v-model="params.critPressure" :controls="false" :min="0.01" :max="3" :step="0.01" />
                <span class="draft-muted-text" style="margin-top:5px">标准值：0.74 MPa</span>
              </el-form-item>
              <el-form-item label="区域/含量临界值：">
                <el-select v-model="critContentPreset">
                  <el-option label="常规区域 W < 8" :value="8" />
                  <el-option label="构造带区域 W < 6" :value="6" />
                  <el-option label="自定义" value="custom" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="critContentPreset === 'custom'" label="自定义值 (m³/t)：">
                <el-input-number v-model="params.critContent" :controls="false" :min="0.01" :step="0.1" />
              </el-form-item>
            </el-form>
            <el-alert
              class="prediction-rule-alert"
              title="区域预测判定规则"
              :description="`P < ${params.critPressure} MPa 且 W < ${params.critContent} m³/t 时为无突出危险区，其他情况为突出危险区（常规区阈值 8，构造带阈值 6）。`"
              type="info"
              :closable="false"
              show-icon
            />
          </div>
        </section>

        <section class="draft-card">
          <h3 class="draft-card__title">评估结果</h3>
          <div class="draft-card__body">
            <div
              class="draft-result-message"
              :class="result ? (result.is_danger ? 'is-danger' : 'is-safe') : ''"
            >
              {{ result ? `${result.is_danger ? '⚠️' : '✅'} ${result.danger_reason}` : '等待计算...' }}
            </div>
          </div>
        </section>

        <section class="draft-card">
          <h3 class="draft-card__title">操作控制</h3>
          <div class="draft-card__body">
            <el-button type="success" size="large" :loading="loading" style="width:100%" @click="handleEvaluate">
              <el-icon><Search /></el-icon>
              计算总瓦斯含量并预测
            </el-button>
            <el-button type="info" style="width:100%" :disabled="!result" @click="exportResults">
              <el-icon><Upload /></el-icon>
              导出结果
            </el-button>
            <el-button type="primary" plain style="width:100%" @click="router.push('/home')">
              <el-icon><HomeFilled /></el-icon>
              返回主页
            </el-button>
          </div>
        </section>
      </aside>

      <section class="draft-card draft-card--fill">
        <h3 class="draft-card__title">煤层区域突出危险性预测曲线</h3>
        <div class="draft-toolbar">
          <el-button type="success" :disabled="!chartImage" @click="saveChart">
            <el-icon><Download /></el-icon>
            保存图表
          </el-button>
          <span class="draft-toolbar__spacer"></span>
          <span class="draft-counter">数据点：{{ chartPointCount }}</span>
        </div>
        <div class="draft-chart-stage">
          <div v-if="chartImage" class="draft-chart-image-wrap">
            <ClickableChartImage :image="chartImage" alt="煤层区域突出危险性预测曲线" />
          </div>
          <DraftChartPlaceholder
            v-else
            title="煤层区域突出危险性预测曲线"
            x-label="瓦斯压力 P (MPa)"
            y-label="瓦斯含量 (m³/t)"
            hint="请导入吸附数据并设置参数"
          />
        </div>
      </section>
    </div>

    <div class="draft-status-bar">{{ statusText }}</div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as XLSX from 'xlsx'
import { calcDetection } from '@/api'
import DraftChartPlaceholder from '@/components/DraftChartPlaceholder.vue'
import ClickableChartImage from '@/components/ClickableChartImage.vue'
import ModulePageHeader from '@/components/ModulePageHeader.vue'
import { parseDetectionWorkbook } from '@/utils/excelData'
import { downloadBase64Png, timestampForFilename } from '@/utils/download'
import { ElMessage } from 'element-plus'
import { saveCalculationRecord } from '@/utils/calculationHistory'
import '@/assets/styles/workbench.css'

const router = useRouter()
const loading = ref(false)
const chartImage = ref('')
const result = ref(null)
const currentFile = ref('')
const statusText = ref('就绪 - 请导入吸附数据并设置参数')
const adsorptionData = ref(null)
const critContentPreset = ref(8)

const params = reactive({
  volume: 0.05,
  temperature: 25,
  compressFactor: 1.0,
  critPressure: 0.74,
  critContent: 8.0,
})

watch(critContentPreset, (value) => {
  if (value !== 'custom') params.critContent = value
})

const chartPointCount = computed(() => result.value?.p_array?.length || 0)

const dataInfo = computed(() => {
  if (!adsorptionData.value) return ''
  const pArray = adsorptionData.value.pArray
  const xxArray = adsorptionData.value.xxArray
  return `数据点数：${pArray.length} | P范围：${Math.min(...pArray).toFixed(2)}-${Math.max(...pArray).toFixed(2)} MPa | Xx范围：${Math.min(...xxArray).toFixed(2)}-${Math.max(...xxArray).toFixed(2)} m³/t`
})

const resultRows = computed(() => {
  if (!result.value || !adsorptionData.value) return []
  return result.value.p_array.map((pressure, index) => ({
    pressure,
    xx: adsorptionData.value.xxArray[index],
    xy: result.value.xy_array[index],
    q: result.value.q_array[index],
  }))
})

async function loadAdsorptionFile(file) {
  try {
    adsorptionData.value = parseDetectionWorkbook(await file.arrayBuffer())
    currentFile.value = file.name
    chartImage.value = ''
    result.value = null
    statusText.value = '吸附数据已加载，请设置游离瓦斯参数并计算'
    ElMessage.success(`已读取 ${adsorptionData.value.pArray.length} 个吸附数据点`)
  } catch (error) {
    ElMessage.error(error.message || '吸附数据文件解析失败')
  }
}

function handleAdsorptionFileChange(file) {
  if (file.raw) loadAdsorptionFile(file.raw)
}

async function handleEvaluate() {
  if (!adsorptionData.value) {
    ElMessage.warning('请先导入吸附数据')
    return
  }
  if (params.volume <= 0 || params.compressFactor <= 0 || params.critPressure <= 0 || params.critContent <= 0) {
    ElMessage.warning('计算参数和临界值必须大于 0')
    return
  }
  loading.value = true
  statusText.value = '正在计算总瓦斯含量并进行区域预测，请稍候...'
  try {
    const res = await calcDetection({
      ...params,
      adsorptionData: adsorptionData.value,
    })
    result.value = res.data
    chartImage.value = res.data.chart_image_base64 || ''
    saveCalculationRecord({
      moduleType: 'detection',
      sourceName: currentFile.value || '吸附数据',
      params: { ...params },
      result: res.data,
      chartImage: chartImage.value,
      inputData: adsorptionData.value,
      summary: res.data.danger_reason || '',
    })
    const xyArray = res.data.xy_array || []
    const qArray = res.data.q_array || []
    statusText.value = `计算完成 - Xy范围：${Math.min(...xyArray).toFixed(2)}-${Math.max(...xyArray).toFixed(2)} m³/t，Q范围：${Math.min(...qArray).toFixed(2)}-${Math.max(...qArray).toFixed(2)} m³/t`
    ElMessage.success(res.data.is_danger ? '预测结果：突出危险区' : '预测结果：无突出危险区')
  } catch (error) {
    statusText.value = '计算失败，请检查吸附数据、参数或计算服务'
  } finally {
    loading.value = false
  }
}

function saveChart() {
  if (!downloadBase64Png(chartImage.value, `区域突出危险性预测_${timestampForFilename()}.png`)) {
    ElMessage.warning('没有图表可以保存')
    return
  }
  statusText.value = '图表已保存'
}

function exportResults() {
  if (!result.value) {
    ElMessage.warning('没有计算结果可以导出')
    return
  }
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, XLSX.utils.json_to_sheet(resultRows.value.map((row) => ({
    '压力P(MPa)': row.pressure,
    '吸附瓦斯Xx(m³/t)': row.xx,
    '游离瓦斯Xy(m³/t)': row.xy,
    '总瓦斯Q(m³/t)': row.q,
  }))), '检测结果')
  XLSX.utils.book_append_sheet(workbook, XLSX.utils.aoa_to_sheet([
    ['参数', '值'],
    ['孔隙容积V(m³/t)', params.volume],
    ['温度t(°C)', params.temperature],
    ['压缩系数A', params.compressFactor],
    ['压力临界值(MPa)', params.critPressure],
    ['含量临界值(m³/t)', params.critContent],
    ['预测判定', result.value.is_danger ? '突出危险区' : '无突出危险区'],
    ['判定依据', result.value.danger_reason],
  ]), '判定信息')
  XLSX.writeFile(workbook, `区域突出危险性预测_${timestampForFilename()}.xlsx`)
  statusText.value = '预测结果已导出'
}
</script>

<style scoped>
.prediction-rule-alert {
  margin-top: 12px;
}
</style>
