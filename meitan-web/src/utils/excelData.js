import * as XLSX from 'xlsx'

const ANALYSIS_REQUIRED_COLUMNS = ['挥发分', '温度', '含水率', 'Vl值', 'Pl值']
const ANALYSIS_SAMPLE_COLUMNS = ['煤样编号', '煤型及编号']
const STATISTICS_FALLBACK_COLUMNS = [
  '检索地区',
  '煤矿',
  '煤层',
  '煤种',
  '挥发分',
  '水分',
  '灰分',
  '固定碳',
  '温度',
  '含水率',
  'VL值',
  'PL值',
  'r0',
]

function cleanCell(value) {
  return value == null ? '' : String(value).trim()
}

function readFirstSheet(arrayBuffer) {
  const workbook = XLSX.read(arrayBuffer, { type: 'array' })
  const sheetName = workbook.SheetNames[0]
  if (!sheetName) throw new Error('Excel 文件中没有可读取的工作表')

  return XLSX.utils.sheet_to_json(workbook.Sheets[sheetName], {
    header: 1,
    defval: null,
    raw: true,
  })
}

function normalizeRow(row, width) {
  return Array.from({ length: width }, (_, index) => row?.[index] ?? null)
}

function hasValue(row) {
  return row.some((value) => value !== null && cleanCell(value) !== '')
}

function uniqueValues(values) {
  return [...new Set(values.filter((value) => value !== null && value !== undefined && value !== ''))]
}

function normalizeCoalSampleName(value) {
  return cleanCell(value).replace(/\s*及\s*/g, '')
}

export function parseAnalysisWorkbook(arrayBuffer) {
  const rows = readFirstSheet(arrayBuffer)
  if (rows.length < 2) throw new Error('参数文件中没有可用数据')

  const headers = rows[0].map(cleanCell)
  const sampleColumn = ANALYSIS_SAMPLE_COLUMNS.find((column) => headers.includes(column))
  const missingColumns = ANALYSIS_REQUIRED_COLUMNS.filter((column) => !headers.includes(column))
  if (!sampleColumn) missingColumns.unshift('煤样编号')
  if (missingColumns.length) {
    throw new Error(`参数文件缺少必要列：${missingColumns.join('、')}`)
  }

  const indexes = Object.fromEntries(
    ANALYSIS_REQUIRED_COLUMNS.map((column) => [column, headers.indexOf(column)])
  )
  indexes['煤样编号'] = headers.indexOf(sampleColumn)

  const records = rows.slice(1).filter(hasValue).map((row) => ({
    coalType: normalizeCoalSampleName(row[indexes['煤样编号']]),
    volatile: Number(row[indexes['挥发分']]),
    temperature: Number(row[indexes['温度']]),
    waterContent: Number(row[indexes['含水率']]),
    vl: Number(row[indexes['Vl值']]),
    pl: Number(row[indexes['Pl值']]),
  })).filter((record) => record.coalType)

  if (!records.length) throw new Error('参数文件中没有有效的煤样数据')
  return records
}

export function parseStatisticsWorkbook(arrayBuffer) {
  const rows = readFirstSheet(arrayBuffer)
  if (rows.length < 2) throw new Error('地区数据文件中没有可用数据')

  const firstHeader = rows[0].map(cleanCell)
  const secondHeader = (rows[1] || []).map(cleanCell)
  const isDraftDoubleHeader = firstHeader.includes('矿区') && secondHeader.includes('挥发分')

  let headers
  let dataRows
  if (isDraftDoubleHeader) {
    const width = Math.max(firstHeader.length, secondHeader.length, STATISTICS_FALLBACK_COLUMNS.length)
    headers = Array.from({ length: width }, (_, index) => {
      if (index === 0) return '检索地区'
      return secondHeader[index] || firstHeader[index] || STATISTICS_FALLBACK_COLUMNS[index] || `字段${index + 1}`
    })
    dataRows = rows.slice(2)
  } else {
    headers = firstHeader.map((header, index) => {
      if (header === '矿区') return '检索地区'
      return header || STATISTICS_FALLBACK_COLUMNS[index] || `字段${index + 1}`
    })
    dataRows = rows.slice(1)
  }

  const missingColumns = ['检索地区', '挥发分', '水分', 'VL值', 'PL值'].filter((column) => !headers.includes(column))
  if (missingColumns.length) {
    throw new Error(`地区数据文件缺少必要列：${missingColumns.join('、')}`)
  }

  const normalizedRows = dataRows
    .map((row) => normalizeRow(row, headers.length))
    .filter(hasValue)

  if (!normalizedRows.length) throw new Error('地区数据文件中没有有效记录')

  const regionIndex = headers.indexOf('检索地区')
  const volatileIndex = headers.indexOf('挥发分')
  const regions = uniqueValues(normalizedRows.map((row) => cleanCell(row[regionIndex]))).sort((a, b) =>
    a.localeCompare(b, 'zh-CN')
  )
  const volatileValues = uniqueValues(
    normalizedRows
      .map((row) => Number(row[volatileIndex]))
      .filter((value) => Number.isFinite(value))
  ).sort((a, b) => a - b)

  const numericColumns = headers.filter((header, columnIndex) => {
    const values = normalizedRows
      .map((row) => row[columnIndex])
      .filter((value) => value !== null && cleanCell(value) !== '')
    return values.length > 0 && values.some((value) => Number.isFinite(Number(value)))
  })

  return {
    fileData: [headers, ...normalizedRows],
    headers,
    numericColumns,
    regions,
    volatileValues,
  }
}

export function parseDetectionWorkbook(arrayBuffer) {
  const workbook = XLSX.read(arrayBuffer, { type: 'array' })
  const sheetName = workbook.SheetNames.includes('计算结果') ? '计算结果' : workbook.SheetNames[0]
  if (!sheetName) throw new Error('吸附数据文件中没有可读取的工作表')

  const rows = XLSX.utils.sheet_to_json(workbook.Sheets[sheetName], {
    header: 1,
    defval: null,
    raw: true,
  })
  if (rows.length < 2) throw new Error('吸附数据文件中没有可用数据')

  const headers = rows[0].map(cleanCell)
  let pressureIndex = headers.findIndex((header) => header.includes('压力') || header.toLowerCase() === 'p')
  let adsorptionIndex = headers.findIndex((header) =>
    header.includes('吸附量') || ['vm', 'xx'].includes(header.toLowerCase())
  )
  if (pressureIndex < 0 || adsorptionIndex < 0) {
    pressureIndex = 0
    adsorptionIndex = 1
  }

  const points = rows.slice(1).map((row) => ({
    pressure: Number(row[pressureIndex]),
    adsorption: Number(row[adsorptionIndex]),
  })).filter((point) => Number.isFinite(point.pressure) && Number.isFinite(point.adsorption))

  if (!points.length) throw new Error('文件中未找到有效的压力和吸附量数据')
  return {
    pArray: points.map((point) => point.pressure),
    xxArray: points.map((point) => point.adsorption),
  }
}
