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

export function buildOutburstRiskEvaluation({ measuredPressure, calculatedContent, critPressure = 0.74, critContent = 8 }) {
  const pressureDanger = measuredPressure >= critPressure
  const contentDanger = calculatedContent >= critContent
  const isDanger = pressureDanger || contentDanger
  const pressureRelation = pressureDanger ? '≥' : '<'
  const contentRelation = contentDanger ? '≥' : '<'
  const pressureDetail = pressureDanger
    ? `${measuredPressure} MPa ${pressureRelation} ${critPressure} MPa，达到或超过压力临界值`
    : `${measuredPressure} MPa ${pressureRelation} ${critPressure} MPa，处于压力安全范围`
  const contentDetail = contentDanger
    ? `${calculatedContent} m³/t ${contentRelation} ${critContent} m³/t，达到或超过含量临界值`
    : `${calculatedContent} m³/t ${contentRelation} ${critContent} m³/t，处于含量安全范围`
  const conclusion = `经计算分析：实测压力值为 ${measuredPressure} MPa，${pressureDanger ? '达到或超过' : '未超过'}压力标准值 ${critPressure} MPa；瓦斯含量计算值为 ${calculatedContent} m³/t，${contentDanger ? '达到或超过' : '未超过'}瓦斯含量临界值 ${critContent} m³/t，判定为${isDanger ? '突出危险区域' : '无突出危险区域'}。`

  return {
    is_danger: isDanger,
    pressure: {
      value: measuredPressure,
      threshold: critPressure,
      relation: pressureRelation,
      is_compliant: !pressureDanger,
      status: pressureDanger ? '不符合' : '符合',
      detail: pressureDetail,
    },
    content: {
      value: calculatedContent,
      threshold: critContent,
      relation: contentRelation,
      is_compliant: !contentDanger,
      status: contentDanger ? '不符合' : '符合',
      detail: contentDetail,
    },
    conclusion,
    summary: `压力指标${pressureDanger ? '不符合' : '符合'}：${pressureDetail}；含量指标${contentDanger ? '不符合' : '符合'}：${contentDetail}。${conclusion}`,
  }
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
  const referenceTemp = 25
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
      showSymbol: false,
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
    legend: { top: 55, type: 'scroll', icon: 'path://M0,5 L40,5', itemWidth: 40, itemHeight: 10 },
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

const STATISTICS_AXIS_UNITS = {
  挥发分: '%',
  水分: '%',
  VL值: 'cm³/g',
  PL值: 'MPa',
}

export function statisticsAxisLabel(key) {
  const unit = STATISTICS_AXIS_UNITS[key]
  return unit ? `${key} (${unit})` : key
}

function categoryValue(value) {
  const text = String(value ?? '').trim()
  return text || '未知'
}

function filterStatisticsRows(rows, params) {
  const regionValue = params.regionFilter ?? params.region_filter ?? []
  const regions = Array.isArray(regionValue)
    ? regionValue.map(String)
    : regionValue === '全部' || regionValue === ''
      ? []
      : [String(regionValue)]
  const volatile = params.volatileFilter ?? params.volatile_filter ?? '全部'
  return rows.filter((row) => {
    const regionMatches = regions.length === 0 || regions.includes(String(row['检索地区'] ?? '未知'))
    const volatileMatches = volatile === '全部' || numericValue(row, '挥发分') === Number(volatile)
    return regionMatches && volatileMatches
  })
}

function statisticsScatterOption(rows, params) {
  const xKey = params.xAxis || params.x_axis || '挥发分'
  const yKey = params.yAxis || params.y_axis || 'VL值'
  const colorKey = params.colorBy || params.color_by || '无'
  const sizeKey = params.sizeBy || params.size_by || '无'
  const useColorEncoding = colorKey !== '无'
  const useSizeEncoding = sizeKey !== '无'
  const xValues = rows.map((row) => numericValue(row, xKey))
  const numericX = xValues.some(Number.isFinite)
  const colorValues = rows.map((row) => useColorEncoding ? numericValue(row, colorKey) : NaN)
  const categoricalFields = new Set(['检索地区', '煤矿', '煤层', '煤种'])
  const populatedColorValues = rows
    .map((row) => row[colorKey])
    .filter((value) => value !== null && value !== undefined && String(value).trim() !== '')
  const numericColor = useColorEncoding && !categoricalFields.has(colorKey)
    && populatedColorValues.length > 0
    && populatedColorValues.every((value) => Number.isFinite(Number(value)))
  const sizeValues = useSizeEncoding ? rows.map((row) => numericValue(row, sizeKey)).filter(Number.isFinite) : []
  const sizeMin = sizeValues.length ? Math.min(...sizeValues) : 0
  const sizeMax = sizeValues.length ? Math.max(...sizeValues) : 1
  const symbolSize = (value) => {
    if (!useSizeEncoding) return 13
    const raw = Number(value?.[3])
    if (!Number.isFinite(raw) || sizeMax === sizeMin) return 13
    return 8 + (raw - sizeMin) / (sizeMax - sizeMin) * 18
  }
  const point = (row, index) => [
    numericX ? xValues[index] : String(row[xKey] ?? '未知'),
    numericValue(row, yKey),
    colorValues[index],
    useSizeEncoding ? numericValue(row, sizeKey) : NaN,
  ]

  let series
  let visualMap
  if (!useColorEncoding) {
    series = [{
      name: yKey,
      type: 'scatter',
      data: rows.map(point),
      symbolSize,
      itemStyle: { color: THEME.primary, opacity: 0.78 },
    }]
  } else if (numericColor) {
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
    title: { text: `${yKey}/${xKey}`, left: 'center', textStyle: { color: THEME.primary } },
    legend: !useColorEncoding || numericColor ? undefined : { top: 42, type: 'scroll' },
    visualMap,
    xAxis: {
      type: numericX ? 'value' : 'category',
      name: statisticsAxisLabel(xKey),
      nameLocation: 'middle',
      nameGap: 42,
      splitLine: { show: true, lineStyle: { type: 'dashed', color: '#d8e8f8' } },
    },
    yAxis: {
      type: 'value',
      name: statisticsAxisLabel(yKey),
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
    legend: { top: 42, icon: 'path://M0,5 C10,0 30,10 40,5', itemWidth: 40, itemHeight: 10 },
    xAxis: { type: numericX ? 'value' : 'category', name: statisticsAxisLabel(xKey), nameLocation: 'middle', nameGap: 42, data: numericX ? undefined : xData },
    yAxis: [
      { type: 'value', name: 'VL值 (cm³/g)', axisLine: { lineStyle: { color: THEME.secondary } } },
      { type: 'value', name: 'PL值 (MPa)', axisLine: { lineStyle: { color: THEME.accent } } },
    ],
    series: [
      { name: 'VL值', type: 'line', smooth: true, showSymbol: false, data: numericX ? prepared.map((item) => [item.x, item.vl]) : prepared.map((item) => item.vl), itemStyle: { color: THEME.secondary } },
      { name: 'PL值', type: 'line', smooth: true, showSymbol: false, yAxisIndex: 1, data: numericX ? prepared.map((item) => [item.x, item.pl]) : prepared.map((item) => item.pl), itemStyle: { color: THEME.accent } },
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
    xAxis: { type: 'category', name: '检索地区', nameLocation: 'middle', nameGap: 56, data: regions, axisLabel: { rotate: 35 } },
    yAxis: { type: 'value', name: '参数均值（VL: cm³/g；PL: MPa）', nameLocation: 'middle', nameGap: 58, splitLine: { lineStyle: { type: 'dashed', color: '#d8e8f8' } } },
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
  if (!rows.length) {
    const selectedRegions = Array.isArray(params.regionFilter) && params.regionFilter.length ? params.regionFilter.join('、') : '全部'
    throw new Error(`筛选后无数据：地区=${selectedRegions}，挥发分=${params.volatileFilter}`)
  }
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
      stats_summary: `数据点: ${rows.length} | ${statisticsAxisLabel(yKey)}均值: ${mean(rows.map((row) => numericValue(row, yKey))).toFixed(2)}`,
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
  const critPressure = 0.74
  const critContent = finiteNumber(params.critContent ?? params.crit_content, 8)
  const measuredPressure = finiteNumber(params.measuredPressure ?? params.measured_pressure, NaN)
  if (!pArray.length || !xxArray.length) throw new Error('吸附数据不能为空')
  if (pArray.length !== xxArray.length) throw new Error('压力和吸附量数据长度不一致')
  if (!(volume > 0) || !(compressFactor > 0)) throw new Error('孔隙容积和压缩系数必须大于 0')
  if (!Number.isFinite(measuredPressure)) throw new Error('实测压力值不能为空')

  const temperatureK = temperature + 273.15
  const xyArray = pArray.map((pressure) => round(volume * pressure * 273.2 / (temperatureK * 0.101325 * compressFactor), 4))
  const qArray = xxArray.map((value, index) => round(value + xyArray[index], 4))
  const pressureContentPairs = pArray
    .map((pressure, index) => ({ pressure, content: qArray[index] }))
    .sort((left, right) => left.pressure - right.pressure)
  const minDataPressure = pressureContentPairs[0].pressure
  const maxDataPressure = pressureContentPairs[pressureContentPairs.length - 1].pressure
  if (measuredPressure < minDataPressure || measuredPressure > maxDataPressure) {
    throw new Error(`实测压力值必须在导入数据的压力范围 ${minDataPressure}-${maxDataPressure} MPa 内`)
  }
  let calculatedContent = pressureContentPairs[pressureContentPairs.length - 1].content
  for (let index = 0; index < pressureContentPairs.length; index += 1) {
    const current = pressureContentPairs[index]
    if (measuredPressure === current.pressure) {
      calculatedContent = current.content
      break
    }
    if (measuredPressure < current.pressure) {
      const previous = pressureContentPairs[index - 1]
      const ratio = (measuredPressure - previous.pressure) / (current.pressure - previous.pressure)
      calculatedContent = previous.content + ratio * (current.content - previous.content)
      break
    }
  }
  calculatedContent = round(calculatedContent, 4)
  const riskEvaluation = buildOutburstRiskEvaluation({ measuredPressure, calculatedContent, critPressure, critContent })
  const isDanger = riskEvaluation.is_danger
  const dangerReason = riskEvaluation.summary

  const adsorptionColor = '#2F9CF4'
  const freeGasColor = '#F39C12'
  const totalGasColor = '#FF6068'
  const measuredPressureColor = '#7B1FA2'
  const calculatedContentColor = '#00897B'
  const dashedLegendIcon = 'path://M0,4 L10,4 L10,6 L0,6 Z M15,4 L25,4 L25,6 L15,6 Z M30,4 L40,4 L40,6 L30,6 Z'
  const solidLegendIcon = 'path://M0,4 L40,4 L40,6 L0,6 Z'
  const chartMaxPressure = Math.max(3, Math.ceil(Math.max(...pArray, measuredPressure, critPressure) * 10.8) / 10)
  const chartMaxContent = Math.max(1, Math.ceil(Math.max(...xxArray, ...xyArray, ...qArray, calculatedContent, critContent) * 1.12))

  const chartImage = chartToBase64({
    title: { text: '煤层区域突出危险性预测（P-W 双指标）', left: 'center', textStyle: { color: THEME.primary } },
    legend: {
      top: 42,
      itemWidth: 40,
      itemHeight: 10,
      data: [
        { name: '吸附瓦斯 Xx', icon: dashedLegendIcon, itemStyle: { color: adsorptionColor } },
        { name: '游离瓦斯 Xy', icon: dashedLegendIcon, itemStyle: { color: freeGasColor } },
        { name: '总瓦斯 Q', icon: solidLegendIcon, itemStyle: { color: totalGasColor } },
        { name: '实测压力值', icon: solidLegendIcon, itemStyle: { color: measuredPressureColor } },
        { name: '瓦斯含量计算值 (m³/t)', icon: solidLegendIcon, itemStyle: { color: calculatedContentColor } },
        { name: '实测评价点', icon: 'circle', itemStyle: { color: '#C62828' } },
      ],
    },
    xAxis: { type: 'value', name: '瓦斯压力 P (MPa)', nameLocation: 'middle', nameGap: 42, min: 0, max: chartMaxPressure },
    yAxis: { type: 'value', name: '瓦斯含量 (m³/t)', nameLocation: 'middle', nameGap: 58, min: 0, max: chartMaxContent },
    series: [
      { name: '吸附瓦斯 Xx', type: 'line', data: pArray.map((pressure, index) => [pressure, xxArray[index]]), itemStyle: { color: adsorptionColor }, lineStyle: { type: 'dashed', color: adsorptionColor, width: 2 }, showSymbol: false },
      { name: '游离瓦斯 Xy', type: 'line', data: pArray.map((pressure, index) => [pressure, xyArray[index]]), itemStyle: { color: freeGasColor }, lineStyle: { type: 'dashed', color: freeGasColor, width: 2 }, showSymbol: false },
      {
        name: '总瓦斯 Q',
        type: 'line',
        data: pArray.map((pressure, index) => [pressure, qArray[index]]),
        itemStyle: { color: totalGasColor },
        lineStyle: { color: totalGasColor, width: 3 },
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
            { name: '含量临界值', yAxis: critContent, lineStyle: { color: '#e53935', type: 'dashed' }, label: { position: 'insideEndTop', formatter: `瓦斯含量临界值：${critContent} m³/t` } },
            { name: '标准值 0.74 MPa', xAxis: critPressure, lineStyle: { color: THEME.warning, type: 'dashed' }, label: { position: 'insideEndTop', formatter: '压力标准值：0.74 MPa' } },
          ],
        },
      },
      {
        name: '实测压力值',
        type: 'line',
        data: [],
        showSymbol: false,
        lineStyle: { color: measuredPressureColor, width: 2 },
        markLine: {
          silent: true,
          symbol: 'none',
          lineStyle: { color: measuredPressureColor, type: 'solid', width: 2 },
          data: [{
            xAxis: measuredPressure,
            label: { position: 'insideStartTop', color: measuredPressureColor, fontWeight: 'bold', formatter: `实测压力值：${measuredPressure} MPa` },
          }],
        },
        z: 9,
      },
      {
        name: '瓦斯含量计算值 (m³/t)',
        type: 'line',
        data: [],
        showSymbol: false,
        lineStyle: { color: calculatedContentColor, width: 2 },
        markLine: {
          silent: true,
          symbol: 'none',
          lineStyle: { color: calculatedContentColor, type: 'solid', width: 2 },
          data: [{
            yAxis: calculatedContent,
            label: { position: 'insideStartBottom', color: calculatedContentColor, fontWeight: 'bold', formatter: `瓦斯含量计算值：${calculatedContent} m³/t` },
          }],
        },
        z: 9,
      },
      {
        name: '实测评价点',
        type: 'scatter',
        data: [[measuredPressure, calculatedContent]],
        symbolSize: 16,
        itemStyle: { color: isDanger ? '#C62828' : '#218739', borderColor: '#fff', borderWidth: 2 },
        label: {
          show: true,
          position: 'top',
          color: isDanger ? '#C62828' : '#218739',
          fontWeight: 'bold',
          formatter: isDanger ? '突出危险区' : '无突出危险区',
        },
        z: 12,
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
      measured_pressure: measuredPressure,
      calculated_content: calculatedContent,
      risk_evaluation: riskEvaluation,
      chart_image_base64: chartImage,
    },
  }
}
