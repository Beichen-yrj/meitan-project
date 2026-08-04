$ErrorActionPreference = 'Stop'
$encoding = New-Object System.Text.UTF8Encoding($false)
$stageDir = Join-Path (Get-Location) '.history_stage'
New-Item -ItemType Directory -Force $stageDir | Out-Null

$jobs = @(
  @{
    File = 'Analysis.vue'
    Anchor = "import { ElMessage } from 'element-plus'"
    Insert = @'
import { ElMessage } from 'element-plus'
import { saveCalculationRecord } from '@/utils/calculationHistory'
'@
    Old = @'
    chartImage.value = res.data.chart_image_base64 || ''
    statusText.value = `计算完成 - ${res.data.stats?.data_points || 0} 个数据点`
'@
    New = @'
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
'@
  },
  @{
    File = 'Statistics.vue'
    Anchor = "import { ElMessage } from 'element-plus'"
    Insert = @'
import { ElMessage } from 'element-plus'
import { saveCalculationRecord } from '@/utils/calculationHistory'
'@
    Old = @'
    chartPointCount.value = countFilteredRows()
    statusText.value = `图表已生成 - ${chartPointCount.value} 个数据点`
'@
    New = @'
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
'@
  },
  @{
    File = 'Detection.vue'
    Anchor = "import { ElMessage } from 'element-plus'"
    Insert = @'
import { ElMessage } from 'element-plus'
import { saveCalculationRecord } from '@/utils/calculationHistory'
'@
    Old = @'
    chartImage.value = res.data.chart_image_base64 || ''
    const xyArray = res.data.xy_array || []
'@
    New = @'
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
'@
  }
)

foreach ($job in $jobs) {
  $source = Join-Path 'src\views' $job.File
  $stage = Join-Path $stageDir $job.File
  Copy-Item -LiteralPath $source -Destination $stage -Force
  $content = [System.IO.File]::ReadAllText((Resolve-Path -LiteralPath $stage))
  if (([regex]::Matches($content, [regex]::Escape($job.Anchor))).Count -ne 1) { throw "$($job.File): import anchor mismatch" }
  if (([regex]::Matches($content, [regex]::Escape($job.Old))).Count -ne 1) { throw "$($job.File): calculation anchor mismatch" }
  $content = $content.Replace($job.Anchor, $job.Insert).Replace($job.Old, $job.New)
  [System.IO.File]::WriteAllText((Resolve-Path -LiteralPath $stage), $content, $encoding)
}

foreach ($job in $jobs) {
  Copy-Item -LiteralPath (Join-Path $stageDir $job.File) -Destination (Join-Path 'src\views' $job.File) -Force
}
Remove-Item -LiteralPath $stageDir -Recurse -Force
