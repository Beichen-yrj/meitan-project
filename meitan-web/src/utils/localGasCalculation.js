import * as echarts from 'echarts'

const THEME = {
  primary: '#1A73E8',
  secondary: '#42A5F5',
  accent: '#FF6B6B',
  warning: '#F39C12',
  success: '#27AE60',
  border: '#90CAF9',
  text: '#1A237E',
}

const COMMON_OPTION = {
  animation: false,
  backgroundColor: '#fff',
  textStyle: { fontFamily: 'Microsoft YaHei, sans-serif', color: THEME.text },
  grid: { left: 82, right: 48, top: 92, bottom: 72, containLabel: false },
  tooltip: { trigger: 'axis' },
}

function finiteNumber(value, fallback = 0) {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}

function round(value, digits = 4) {
  const scale = 10 ** digits
  return Math.round((value + Number.EPSILON) * scale) / scale
}

function mean(values) {
  const valid = values.filter(Number.isFinite)
  return valid.length ? valid.reduce((sum, value) => sum + value, 0) / valid.length : 0
}

function chartToBase64(option, width = 1200, height = 720) {
  const host = document.createElement('div')
  Object.assign(host.style, {
    position: 'fixed',
    left: '-10000px',
    top: '0',
    width: `${width}px`,
    height: `${height}px`,
    pointerEvents: 'none',
  })
  document.body.appendChild(host)

  const chart = echarts.init(host, null, { renderer: 'canvas', width, height })
  try {
    chart.setOption({ ...COMMON_OPTION, ...option }, true)
    const dataUrl = chart.getDataURL({ type: 'png', pixelRatio: 1.5, backgroundColor: '#fff' })
    return dataUrl.substring(dataUrl.indexOf(',') + 1)
  } finally {
    chart.dispose()
    host.remove()
  }
}

function createPressureValues(minimum, maximum, step) {
  if (!(minimum < maximum)) throw new Error('最小压力必须小于最大压力')
  if (!(step > 0)) throw new Error('压力步长必须大于 0')
  if (minimum < 0) throw new Error('最小压力不能小于 0')
  if ((maximum - minimum) / step > 10000) throw new Error('压力步长过小，生成的数据点超过限制')

  const values = []
  for (let value = minimum; value <= maximum + step * 0.5; value += step) {
    values.push(round(value, 8))
  }
  return values
}

export async function calculateAnalysisLocally(params) {
  const coalType = params.coalType || params.coal_type || '未知'
  const volatile = finiteNumber(params.volatile)
  const temperature = finiteNumber(params.temperature, 20)
  const waterContent = finiteNumber(params.waterContent ?? params.water_content)
  const vl = finiteNumber(params.vl ?? params.Vl)
  const pl = finiteNumber(params.pl ?? params.Pl)
  const referenceTemp = finiteNumber(params.referenceTemp ?? params.reference_temp, 25)
  const pMin = finiteNumber(params.pMin ?? params.p_min, 0)
  const pMax = finiteNumber(params.pMax ?? params.p_max, 16)
  const pStep = finiteNumber(params.pStep ?? params.p_step, 0.1)
  const chartStyle = params.chartStyle || params.chart_style || 'curve'
  const comparisonCurves = params.comparisonCurves || params.comparison_curves || []
  const pArray = createPressureValues(pMin, pMax, pStep)

  const alpha = Math.exp(-(-5.079e-4 * volatile ** 2 + 0.028 * volatile - 0.015) * waterContent)
  const vmArray = pArray.map((pressure) => {
    if (pressure === 0 || pl + pressure === 0) return 0
    const lambda = Math.exp(-0.009 * pressure ** -0.286 * (temperature - referenceTemp))
    return round((vl * pressure) / (pl + pressure) * alpha * lambda, 6)
  })

  const comparisonSeries = comparisonCurves.map((curve, index) => ({
    name: curve.label || `对比${index + 1}`,
    type: 'line',
    data: (curve.p_array || []).map((pressure, pointIndex) => [pressure, curve.vm_array?.[pointIndex]]),
    showSymbol: false,
    lineStyle: { type: 'dashed', width: 2 },
  }))

  const mainData = pArray.map((pressure, index) => [pressure, vmArray[index]])
  const mainSeries = {
    name: `${coalType} | T=${temperature}℃, Vdaf=${volatile}%`,
    data: mainData,
    itemStyle: { color: THEME.primary },
    lineStyle: { color: THEME.primary, width: 3 },
  }
  if (chartStyle === 'scatter') {
    Object.assign(mainSeries, { type: 'scatter', symbolSize: 9 })
  } else if (chartStyle === 'bar') {
    Object.assign(mainSeries, { type: 'bar', barMaxWidth: 24 })
  } else {
    Object.assign(mainSeries, {
      type: 'line',
      smooth: true,
      showSymbol: pArray.length <= 20,
      areaStyle: chartStyle === 'area' ? { color: 'rgba(26,115,232,.25)' } : undefined,
    })
  }

  const chartImage = chartToBase64({
    title: {
      text: '煤层瓦斯吸附定量分析图',
      subtext: 'Vm = (VL × P) / (PL + P) × α × λ',
      left: 'center',
      textStyle: { color: THEME.primary, fontSize: 21 },
    },
    legend: { top: 55, type: 'scroll' },
    xAxis: {
      type: 'value',
      name: '气体压力 P (MPa)',
      min: 0,
      nameLocation: 'middle',
      nameGap: 42,
      splitLine: { lineStyle: { type: 'dashed', color: '#d8e8f8' } },
    },
    yAxis: {
      type: 'value',
      name: '气体吸附量 Vm (cm³/g)',
      nameLocation: 'middle',
      nameGap: 58,
      min: 0,
      splitLine: { lineStyle: { type: 'dashed', color: '#d8e8f8' } },
    },
    series: [...comparisonSeries, mainSeries],
  })

  return {
    code: 200,
    message: 'success',
    data: {
      p_array: pArray.map((value) => round(value, 4)),
      vm_array: vmArray,
      stats: {
        data_points: pArray.length,
        max_vm: round(Math.max(...vmArray), 4),
        min_vm: round(Math.min(...vmArray), 4),
        avg_vm: round(mean(vmArray), 4),
        pressure_range: `${pMin} - ${pMax} MPa`,
      },
      coal_type: coalType,
      chart_image_base64: chartImage,
    },
  }
}

function rowsFromFileData(fileData) {
  if (!Array.isArray(fileData) || fileData.length < 2) throw new Error('请先加载 Excel 地区数据文件')
  const headers = fileData[0].map((value) => String(value ?? '').trim())
  let rows = fileData.slice(1)
  if (rows[0]?.some((value) => String(value ?? '').trim() === '挥发分')) rows = rows.slice(1)
  return rows.map((row) => Object.fromEntries(headers.map((header, index) => [header, row[index]])))
}

function numericValue(row, key) {
  const rawValue = row[key]
  if (rawValue === null || rawValue === undefined || String(rawValue).trim() === '') return NaN
  const value = Number(rawValue)
  return Number.isFinite(value) ? value : NaN
}

function categoryValue(value) {
  const text = String(value ?? '').trim()
  return text || '未知'
}

function filterStatisticsRows(rows, params) {
  const region = params.regionFilter ?? params.region_filter ?? '全部'
  const volatile = params.volatileFilter ?? params.volatile_filter ?? '全部'
  return rows.filter((row) => {
    const regionMatches = region === '全部' || String(row['检索地区'] ?? '未知') === String(region)
    const volatileMatches = volatile === '全部' || numericValue(row, '挥发分') === Number(volatile)
    return regionMatches && volatileMatches
  })
}

function statisticsScatterOption(rows, params) {
  const xKey = params.xAxis || params.x_axis || '挥发分'
  const yKey = params.yAxis || params.y_axis || 'VL值'
  const colorKey = params.colorBy || params.color_by || '检索地区'
  const sizeKey = params.sizeBy || params.size_by || '挥发分'
  const xValues = rows.map((row) => numericValue(row, xKey))
  const numericX = xValues.some(Number.isFinite)
  const colorValues = rows.map((row) => numericValue(row, colorKey))
  const categoricalFields = new Set(['检索地区', '煤矿', '煤层', '煤种'])
  const populatedColorValues = rows
    .map((row) => row[colorKey])
    .filter((value) => value !== null && value !== undefined && String(value).trim() !== '')
  const numericColor = !categoricalFields.has(colorKey)
    && populatedColorValues.length > 0
    && populatedColorValues.every((value) => Number.isFinite(Number(value)))
  const sizeValues = rows.map((row) => numericValue(row, sizeKey)).filter(Number.isFinite)
  const sizeMin = sizeValues.length ? Math.min(...sizeValues) : 0
  const sizeMax = sizeValues.length ? Math.max(...sizeValues) : 1
  const symbolSize = (value) => {
    const raw = Number(value?.[3])
    if (!Number.isFinite(raw) || sizeMax === sizeMin) return 13
    return 8 + (raw - sizeMin) / (sizeMax - sizeMin) * 18
  }
  const point = (row, index) => [
    numericX ? xValues[index] : String(row[xKey] ?? '未知'),
    numericValue(row, yKey),
    colorValues[index],
    numericValue(row, sizeKey),
  ]

  let series
  let visualMap
  if (numericColor) {
    const validColors = colorValues.filter(Number.isFinite)
    series = [{ name: yKey, type: 'scatter', data: rows.map(point), symbolSize }]
    visualMap = {
      min: Math.min(...validColors),
      max: Math.max(...validColors),
      dimension: 2,
      right: 12,
      top: 95,
      text: [colorKey, ''],
      calculable: true,
      inRange: { color: ['#42A5F5', '#1A73E8', '#FF6B6B'] },
    }
  } else {
    const groups = new Map()
    rows.forEach((row, index) => {
      const category = categoryValue(row[colorKey])
      if (!groups.has(category)) groups.set(category, [])
      groups.get(category).push(point(row, index))
    })
    series = [...groups.entries()].map(([name, data]) => ({
      name,
      type: 'scatter',
      data,
      symbolSize,
    }))
  }

  return {
    title: { text: `煤样参数分布 - ${yKey} vs ${xKey}`, left: 'center', textStyle: { color: THEME.primary } },
    legend: numericColor ? undefined : { top: 42, type: 'scroll' },
    visualMap,
    xAxis: {
      type: numericX ? 'value' : 'category',
      name: xKey,
      nameLocation: 'middle',
      nameGap: 42,
      splitLine: { show: true, lineStyle: { type: 'dashed', color: '#d8e8f8' } },
    },
    yAxis: {
      type: 'value',
      name: yKey,
      nameLocation: 'middle',
      nameGap: 58,
      splitLine: { lineStyle: { type: 'dashed', color: '#d8e8f8' } },
    },
    series,
  }
}

function statisticsDualAxisOption(rows, params) {
  const xKey = params.xAxis || params.x_axis || '挥发分'
  const prepared = rows.map((row) => ({
    x: numericValue(row, xKey),
    label: String(row[xKey] ?? '未知'),
    vl: numericValue(row, 'VL值'),
    pl: numericValue(row, 'PL值'),
  }))
  const numericX = prepared.some((item) => Number.isFinite(item.x))
  if (numericX) prepared.sort((a, b) => a.x - b.x)
  const xData = prepared.map((item) => numericX ? item.x : item.label)
  return {
    title: { text: `VL和PL值对比 - ${xKey}`, left: 'center', textStyle: { color: THEME.primary } },
    legend: { top: 42 },
    xAxis: { type: numericX ? 'value' : 'category', name: xKey, data: numericX ? undefined : xData },
    yAxis: [
      { type: 'value', name: 'VL值 (cm³/g)', axisLine: { lineStyle: { color: THEME.secondary } } },
      { type: 'value', name: 'PL值 (MPa)', axisLine: { lineStyle: { color: THEME.accent } } },
    ],
    series: [
      { name: 'VL值', type: 'line', smooth: true, data: numericX ? prepared.map((item) => [item.x, item.vl]) : prepared.map((item) => item.vl), itemStyle: { color: THEME.secondary } },
      { name: 'PL值', type: 'line', smooth: true, yAxisIndex: 1, data: numericX ? prepared.map((item) => [item.x, item.pl]) : prepared.map((item) => item.pl), itemStyle: { color: THEME.accent } },
    ],
  }
}

function statisticsGroupedOption(rows) {
  const groups = new Map()
  rows.forEach((row) => {
    const region = String(row['检索地区'] ?? '未知')
    if (!groups.has(region)) groups.set(region, { vl: [], pl: [] })
    const group = groups.get(region)
    group.vl.push(numericValue(row, 'VL值'))
    group.pl.push(numericValue(row, 'PL值'))
  })
  const regions = [...groups.keys()].sort((a, b) => a.localeCompare(b, 'zh-CN'))
  return {
    title: { text: '按地区分组的VL和PL值对比', left: 'center', textStyle: { color: THEME.primary } },
    legend: { top: 42 },
    xAxis: { type: 'category', data: regions, axisLabel: { rotate: 35 } },
    yAxis: { type: 'value', name: '参数均值', splitLine: { lineStyle: { type: 'dashed', color: '#d8e8f8' } } },
    dataZoom: regions.length > 10 ? [{ type: 'inside' }, { type: 'slider', bottom: 12 }] : undefined,
    grid: { left: 70, right: 40, top: 90, bottom: regions.length > 10 ? 112 : 82 },
    series: [
      { name: 'VL值', type: 'bar', data: regions.map((region) => round(mean(groups.get(region).vl), 4)), itemStyle: { color: THEME.secondary } },
      { name: 'PL值', type: 'bar', data: regions.map((region) => round(mean(groups.get(region).pl), 4)), itemStyle: { color: THEME.accent } },
    ],
  }
}

export async function calculateStatisticsLocally(params) {
  const rows = filterStatisticsRows(rowsFromFileData(params.fileData || params.file_data), params)
  if (!rows.length) throw new Error(`筛选后无数据：地区=${params.regionFilter}，挥发分=${params.volatileFilter}`)
  const chartType = params.chartType || params.chart_type || 'scatter'
  const yKey = params.yAxis || params.y_axis || 'VL值'
  const option = chartType === 'dual_axis'
    ? statisticsDualAxisOption(rows, params)
    : chartType === 'grouped'
      ? statisticsGroupedOption(rows)
      : statisticsScatterOption(rows, params)
  const regions = [...new Set(rows.map((row) => String(row['检索地区'] ?? '未知')))].sort((a, b) => a.localeCompare(b, 'zh-CN'))
  return {
    code: 200,
    message: 'success',
    data: {
      chart_image_base64: chartToBase64(option),
      stats_summary: `数据点: ${rows.length} | ${yKey}均值: ${mean(rows.map((row) => numericValue(row, yKey))).toFixed(2)}`,
      region_list: regions,
      countries: regions.length,
    },
  }
}

export async function calculateDetectionLocally(params) {
  const adsorption = params.adsorptionData || params.adsorption_data || {}
  const pArray = (adsorption.pArray || adsorption.p_array || []).map(Number)
  const xxArray = (adsorption.xxArray || adsorption.xx_array || []).map(Number)
  const volume = finiteNumber(params.volume, 0.05)
  const temperature = finiteNumber(params.temperature, 25)
  const compressFactor = finiteNumber(params.compressFactor ?? params.compress_factor, 1)
  const critPressure = finiteNumber(params.critPressure ?? params.crit_pressure, 0.74)
  const critContent = finiteNumber(params.critContent ?? params.crit_content, 8)
  if (!pArray.length || !xxArray.length) throw new Error('吸附数据不能为空')
  if (pArray.length !== xxArray.length) throw new Error('压力和吸附量数据长度不一致')
  if (!(volume > 0) || !(compressFactor > 0)) throw new Error('孔隙容积和压缩系数必须大于 0')

  const temperatureK = temperature + 273.15
  const xyArray = pArray.map((pressure) => round(volume * pressure * 273.2 / (temperatureK * 0.101325 * compressFactor), 4))
  const qArray = xxArray.map((value, index) => round(value + xyArray[index], 4))
  const pressureIndex = pArray.findIndex((value) => value >= critPressure)
  const contentIndex = qArray.findIndex((value) => value >= critContent)
  const isDanger = pressureIndex >= 0 || contentIndex >= 0
  const reasons = []
  if (pressureIndex >= 0) reasons.push(`瓦斯压力超标：当 P ≥ ${pArray[pressureIndex].toFixed(2)} MPa 时，压力 ≥ ${critPressure} MPa`)
  if (contentIndex >= 0) reasons.push(`总瓦斯含量超标：当 P ≥ ${pArray[contentIndex].toFixed(2)} MPa 时，含量 ≥ ${critContent} m³/t`)
  const dangerReason = isDanger
    ? `突出危险区：${reasons.join('；')}。除 P < ${critPressure} MPa 且 W < ${critContent} m³/t 以外的情况均判为突出危险区。`
    : `无突出危险区：瓦斯压力 P < ${critPressure} MPa，瓦斯含量 W < ${critContent} m³/t。`

  const chartImage = chartToBase64({
    title: { text: '煤层区域突出危险性预测（P-W 双指标）', left: 'center', textStyle: { color: THEME.primary } },
    legend: { top: 42 },
    xAxis: { type: 'value', name: '瓦斯压力 P (MPa)', nameLocation: 'middle', nameGap: 42, min: 0, max: 3 },
    yAxis: { type: 'value', name: '瓦斯含量 (m³/t)', nameLocation: 'middle', nameGap: 58, min: 0 },
    series: [
      { name: '吸附瓦斯 Xx', type: 'line', data: pArray.map((pressure, index) => [pressure, xxArray[index]]), lineStyle: { type: 'dashed', color: THEME.secondary, width: 2 }, showSymbol: false },
      { name: '游离瓦斯 Xy', type: 'line', data: pArray.map((pressure, index) => [pressure, xyArray[index]]), lineStyle: { type: 'dashed', color: THEME.warning, width: 2 }, showSymbol: false },
      {
        name: '总瓦斯 Q',
        type: 'line',
        data: pArray.map((pressure, index) => [pressure, qArray[index]]),
        lineStyle: { color: THEME.accent, width: 3 },
        showSymbol: false,
        markArea: {
          silent: true,
          itemStyle: { color: 'rgba(39,174,96,.12)' },
          label: { color: '#218739', formatter: '无突出危险区' },
          data: [[
            { xAxis: 0, yAxis: 0 },
            { xAxis: critPressure, yAxis: critContent },
          ]],
        },
        markLine: {
          symbol: 'none',
          data: [
            { name: '含量临界值', yAxis: critContent, lineStyle: { color: '#e53935', type: 'dashed' }, label: { formatter: `含量临界值 ${critContent}` } },
            { name: '压力临界值', xAxis: critPressure, lineStyle: { color: THEME.warning, type: 'dotted' }, label: { formatter: `压力临界值 ${critPressure}` } },
          ],
        },
      },
    ],
  })

  return {
    code: 200,
    message: 'success',
    data: {
      xy_array: xyArray,
      q_array: qArray,
      p_array: pArray.map((value) => round(value, 4)),
      is_danger: isDanger,
      danger_reason: dangerReason,
      crit_pressure: critPressure,
      crit_content: critContent,
      chart_image_base64: chartImage,
    },
  }
}
