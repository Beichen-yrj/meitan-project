<template>
  <div class="draft-workbench">
    <ModulePageHeader title="煤层区域突出危险性预测" />

    <div class="draft-workbench__grid draft-grid--detection">
      <aside class="draft-scroll-column">
        <section class="draft-card">
          <div class="draft-card__body">
            <div class="draft-file-actions">
              <el-upload
                :auto-upload="false"
                :show-file-list="false"
                accept=".xlsx,.xls,.csv"
                :on-change="handleAdsorptionFileChange"
              >
                <el-button type="primary" size="large">
                  <el-icon><FolderOpened /></el-icon>
                  导入吸附数据
                </el-button>
              </el-upload>
            </div>
            <div class="draft-file-name">{{ currentFile ? `已加载：${currentFile}` : '未加载吸附数据' }}</div>
            <div v-if="dataInfo" class="draft-muted-text" style="margin-top:6px">{{ dataInfo }}</div>
          </div>
        </section>

        <section class="draft-card">
          <h3 class="draft-card__title">游离瓦斯计算参数</h3>
          <div class="draft-card__body">
            <el-form label-width="165px" label-position="left">
              <el-form-item label="孔隙容积 (m³/t)：">
                <el-input-number v-model="params.volume" :controls="false" :min="0.0001" :step="0.01" />
              </el-form-item>
              <el-form-item label="温度 (°C)：">
                <el-input-number v-model="params.temperature" :controls="false" :min="-20" :max="100" />
              </el-form-item>
              <el-form-item label="压缩系数：">
                <el-input-number v-model="params.compressFactor" :controls="false" :min="0.1" :step="0.1" />
              </el-form-item>
            </el-form>
            <div class="draft-muted-text">标准状况：T₀=273.2K，P₀=0.101325MPa</div>
          </div>
        </section>

        <section class="draft-card">
          <h3 class="draft-card__title">区域突出危险性预测临界值</h3>
          <div class="draft-card__body">
            <el-form class="prediction-threshold-form" label-width="172px" label-position="left">
              <el-form-item label="实测压力值 (MPa)：">
                <el-input-number v-model="params.measuredPressure" :controls="false" :min="measuredPressureMin" :max="measuredPressureMax" :step="0.01" placeholder="请输入实测压力值" />
              </el-form-item>
              <el-form-item label="瓦斯含量计算值(m³/t)：">
                <el-input :model-value="calculatedContentDisplay" readonly />
              </el-form-item>
              <el-form-item label="瓦斯含量临界值(m³/t)：">
                <el-select v-model="critContentPreset">
                  <el-option label="一般区域 W < 8" :value="8" />
                  <el-option label="构造带区域 W < 6" :value="6" />
                  <el-option label="自定义" value="custom" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="critContentPreset === 'custom'" label="自定义值 (m³/t)：">
                <el-input-number v-model="params.critContent" :controls="false" :min="0.01" :step="0.1" />
              </el-form-item>
            </el-form>
          </div>
        </section>

        <section class="draft-card">
          <h3 class="draft-card__title">评估参考与结果</h3>
          <div class="draft-card__body">
            <p class="prediction-reference-description">根据《防治煤与瓦斯突出细则》要求，对照煤层瓦斯压力、瓦斯含量两项指标，可通过本表判定煤层对应的突出危险区域类别。</p>
            <img class="prediction-reference-image" src="/images/区域预测临界值图1.jpg" alt="图1 区域预测临界值" />
            <div v-if="riskEvaluation" class="risk-evaluation">
              <div class="risk-evaluation__heading">
                <span>双指标判定结果</span>
                <el-tag :type="result.is_danger ? 'danger' : 'success'" effect="dark">
                  {{ result.is_danger ? '突出危险区' : '无突出危险区' }}
                </el-tag>
              </div>

              <div class="risk-metric">
                <div class="risk-metric__heading">
                  <strong>瓦斯压力 P</strong>
                  <el-tag :type="riskEvaluation.pressure.is_compliant ? 'success' : 'danger'" size="small">
                    {{ riskEvaluation.pressure.is_compliant ? '符合范围' : '不符合' }}
                  </el-tag>
                </div>
                <div class="risk-metric__values">
                  <span>实测 {{ riskEvaluation.pressure.value }} MPa</span>
                  <span>临界值 {{ riskEvaluation.pressure.threshold }} MPa</span>
                </div>
                <p>{{ riskEvaluation.pressure.detail }}</p>
              </div>

              <div class="risk-metric">
                <div class="risk-metric__heading">
                  <strong>瓦斯含量 W</strong>
                  <el-tag :type="riskEvaluation.content.is_compliant ? 'success' : 'danger'" size="small">
                    {{ riskEvaluation.content.is_compliant ? '符合范围' : '不符合' }}
                  </el-tag>
                </div>
                <div class="risk-metric__values">
                  <span>计算值 {{ riskEvaluation.content.value }} m³/t</span>
                  <span>临界值 {{ riskEvaluation.content.threshold }} m³/t</span>
                </div>
                <p>{{ riskEvaluation.content.detail }}</p>
              </div>

              <div class="risk-evaluation__conclusion" :class="result.is_danger ? 'is-danger' : 'is-safe'">
                <el-icon><component :is="result.is_danger ? 'WarningFilled' : 'CircleCheckFilled'" /></el-icon>
                <strong>{{ riskEvaluation.conclusion }}</strong>
              </div>
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
        <h3 class="draft-card__title">煤层区域突出危险性预测</h3>
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
            <ClickableChartImage :image="chartImage" alt="煤层区域突出危险性预测" />
          </div>
          <DraftChartPlaceholder
            v-else
            title="煤层区域突出危险性预测"
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
  measuredPressure: null,
  critContent: 8.0,
})

watch(critContentPreset, (value) => {
  if (value !== 'custom') params.critContent = value
})

const chartPointCount = computed(() => result.value?.p_array?.length || 0)
const riskEvaluation = computed(() => result.value?.risk_evaluation || null)
const measuredPressureMin = computed(() => adsorptionData.value ? Math.min(...adsorptionData.value.pArray) : 0)
const measuredPressureMax = computed(() => adsorptionData.value ? Math.max(...adsorptionData.value.pArray) : 10)
const calculatedContentDisplay = computed(() => (
  Number.isFinite(result.value?.calculated_content)
    ? result.value.calculated_content
    : '计算后自动生成'
))

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
    if (Number.isFinite(params.measuredPressure) && (params.measuredPressure < measuredPressureMin.value || params.measuredPressure > measuredPressureMax.value)) {
      params.measuredPressure = null
    }
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
  if (params.volume <= 0 || params.compressFactor <= 0 || params.critContent <= 0) {
    ElMessage.warning('计算参数和临界值必须大于 0')
    return
  }
  if (!Number.isFinite(params.measuredPressure)) {
    ElMessage.warning('请输入实测压力值')
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
      params: { ...params, calculatedContent: res.data.calculated_content },
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
    statusText.value = error.message || '计算失败，请检查吸附数据和参数'
    ElMessage.error(statusText.value)
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
    ['孔隙容积(m³/t)', params.volume],
    ['温度(°C)', params.temperature],
    ['压缩系数', params.compressFactor],
    ['实测压力值(MPa)', params.measuredPressure],
    ['压力标准值(MPa)', 0.74],
    ['瓦斯含量计算值(m³/t)', result.value.calculated_content],
    ['瓦斯含量临界值(m³/t)', params.critContent],
    ['压力指标判定', riskEvaluation.value?.pressure.detail || '-'],
    ['瓦斯含量指标判定', riskEvaluation.value?.content.detail || '-'],
    ['预测判定', result.value.is_danger ? '突出危险区' : '无突出危险区'],
    ['最终结论', riskEvaluation.value?.conclusion || result.value.danger_reason],
    ['判定依据', result.value.danger_reason],
  ]), '判定信息')
  XLSX.writeFile(workbook, `区域突出危险性预测_${timestampForFilename()}.xlsx`)
  statusText.value = '预测结果已导出'
}
</script>

<style scoped>
.prediction-threshold-form :deep(.el-form-item__label) {
  white-space: nowrap;
}

.prediction-reference-description {
  margin: 0 0 12px;
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.75;
}

.prediction-reference-image {
  display: block;
  width: 100%;
  height: auto;
  margin-bottom: 12px;
  border: 1px solid #d8e8f4;
}

.risk-evaluation {
  border-top: 3px solid var(--primary);
}

.risk-evaluation__heading,
.risk-metric__heading,
.risk-metric__values,
.risk-evaluation__conclusion {
  display: flex;
  align-items: center;
}

.risk-evaluation__heading {
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0 10px;
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 800;
}

.risk-metric {
  padding: 12px 0;
  border-bottom: 1px solid #d8e8f4;
}

.risk-metric__heading {
  justify-content: space-between;
  gap: 10px;
  color: var(--text-primary);
  font-size: 14px;
}

.risk-metric__values {
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 6px 12px;
  margin-top: 8px;
  color: #475569;
  font-size: 12px;
}

.risk-metric p {
  margin: 6px 0 0;
  color: #334155;
  font-size: 12px;
  line-height: 1.6;
}

.risk-evaluation__conclusion {
  align-items: flex-start;
  gap: 8px;
  margin-top: 12px;
  padding: 11px 12px;
  border: 1px solid currentColor;
  background: #fff;
  font-size: 13px;
  line-height: 1.65;
}

.risk-evaluation__conclusion .el-icon {
  flex: 0 0 auto;
  margin-top: 3px;
}

.is-danger {
  color: #c62828;
}

.is-safe {
  color: #218739;
}
</style>
