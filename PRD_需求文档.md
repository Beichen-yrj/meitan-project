# 煤层瓦斯智能分析平台 需求规格说明书（PRD）

> 版本：v1.0 | 日期：2026-08-02 | 基于草稿 v2.2 重构

---

## 一、项目背景

### 1.1 现状

现有「煤层瓦斯智能分析平台」v2.2 为 Python Tkinter + Matplotlib + Pandas 技术栈构建的桌面端应用，包含以下 7 个功能页面：

| 页面 | 文件 | 功能 |
|------|------|------|
| 首页 | `welcome.py` | 侧边栏导航、轮播图、新闻资讯 |
| 瓦斯介绍 | `introduction.py` | 煤层瓦斯科普图文展示 |
| 板块一：瓦斯吸附含量计算与分析 | `analysis.py` | Langmuir 吸附模型计算、多曲线对比、图表导出 |
| 板块二：煤样瓦斯吸附参数统计分析 | `statistics.py` | 散点图/双轴/分组图、地区检索、参数筛选 |
| 板块三：煤层瓦斯突出危险性检测 | `detection.py` | 游离瓦斯计算、双重临界值判定、危险区域标注 |
| 报告导出 | `export.py` | XLSX/HTML 综合报告生成 |
| 用户反馈 | `feedback.py` | 五星评分、意见收集 |
| AI 助手 | `xiaowa.py` | DeepSeek API 对话、自然语言导航、TTS 朗读 |

### 1.2 改造目标

将桌面端应用重构为 **B/S 架构**的 Web 应用：
- 后端：**Java SpringBoot + Maven + MySQL + MyBatis**
- 前端：**Vue 3**
- **保留三个核心科学计算模块使用 Python 实现**，通过 HTTP API 供后端调用

---

## 二、技术架构

### 2.1 整体架构

```
┌─────────────────────────────────────────┐
│              Vue 3 前端                  │
│  (首页/介绍/登录/数据管理/报告/反馈)        │
└──────────────┬──────────────────────────┘
               │ HTTP REST API
┌──────────────▼──────────────────────────┐
│          SpringBoot 后端                 │
│  - 用户管理 / 角色权限                    │
│  - 数据文件管理（上传/解析/存储）           │
│  - 报告生成（聚合三板块结果）              │
│  - 反馈管理                              │
│  - 调用 Python 计算服务                   │
└──────┬──────────────────┬───────────────┘
       │                  │
       │ MyBatis          │ HTTP
       ▼                  ▼
┌──────────────┐  ┌─────────────────────────┐
│   MySQL      │  │  Python 计算服务 (Flask) │
│  - 用户表     │  │  - 瓦斯吸附含量计算      │
│  - 数据文件   │  │  - 参数统计分析          │
│  - 计算结果   │  │  - 突出危险性检测        │
│  - 报告记录   │  │  (保留原 matplotlib 绘图)│
│  - 反馈记录   │  └─────────────────────────┘
└──────────────┘
```

### 2.2 技术选型

| 层级 | 技术 | 说明 |
|------|------|------|
| 前端框架 | Vue 3 + Vite | 组件化开发，替代 Tkinter |
| UI 组件库 | Element Plus | 企业级组件，风格与现有蓝色主题一致 |
| 图表库 | ECharts 5 | 替代 Matplotlib 用于前端图表展示 |
| 后端框架 | SpringBoot 3.x | |
| 构建工具 | Maven 3.9+ | |
| 数据库 | MySQL 8.0 | |
| ORM | MyBatis / MyBatis-Plus | |
| Python 服务 | Flask 3.x | 封装三个科学计算模块 |
| 科学计算库 | numpy, pandas, matplotlib (Python) | 保留原计算逻辑 |
| 认证 | Spring Security + JWT | |

---

## 三、功能模块划分

### 3.1 模块归属

| 模块 | 负责技术栈 | 说明 |
|------|-----------|------|
| 用户登录/注册/权限 | **SpringBoot + Vue** | 新增 |
| 首页（导航/新闻/轮播） | **Vue** | 前端展示，新闻可由后端 API 管理 |
| 瓦斯介绍页 | **Vue** | 静态图文，图片资源托管 |
| **板块一：瓦斯吸附含量计算** | **Python (Flask)** | 核心计算逻辑保留 Python |
| **板块二：参数统计分析** | **Python (Flask)** | 核心计算逻辑保留 Python |
| **板块三：突出危险性检测** | **Python (Flask)** | 核心计算逻辑保留 Python |
| 数据文件上传/管理 | **SpringBoot + Vue** | 替代原文件对话框 |
| 报告导出 | **SpringBoot + Vue** | 替代原 export.py，支持 XLSX/PDF |
| 用户反馈 | **SpringBoot + Vue** | 替代原 feedback.py |
| AI 助手 | **Vue** (可选) | 替代原 xiaowa.py，前端直接调用 DeepSeek API |

### 3.2 详细功能说明

#### 3.2.1 用户系统（新增）

- 用户注册/登录（JWT 认证）
- 角色：管理员、普通用户
- 管理员可查看所有用户的计算记录和报告
- 普通用户可管理自己的数据文件和计算结果

#### 3.2.2 首页

- 平台名称与 Logo
- 功能导航卡片（6 个主要功能入口）
- 行业新闻资讯展示（从数据库读取）
- 数据统计概览（历史计算次数等）

#### 3.2.3 瓦斯介绍页

- 煤层瓦斯科普文字
- 介绍图片画廊（支持放大查看）

#### 3.2.4 板块一：瓦斯吸附含量计算与分析（Python 保留）

**核心公式**（Langmuir 吸附模型）：

```
α = exp(-(-5.079e-4 × Vdaf² + 0.028 × Vdaf - 0.015) × w)
λ = exp(-0.009 × P^(-0.286) × (T - T₀))
Vm = (Vl × P) / (Pl + P) × α × λ
```

**功能点**：
- 加载 Excel 参数文件（煤型及编号、挥发分、温度、含水率、Vl值、Pl值）
- 煤型及编号下拉选择，自动回填对应参数
- 设置计算参数：参考温度 T0、压力范围 P_min~P_max、步长 P_step
- 执行 Langmuir 吸附含量计算
- 4 种图表样式：曲线图、散点图、柱状图、面积图
- 多曲线对比管理（添加/删除/清空对比曲线）
- 计算结果数据表展示（序号、压力P、吸附量Vm）
- 统计信息（最大/最小/平均吸附量）
- 导出计算结果（Excel/CSV）
- 保存图表图片（PNG/JPG/PDF）
- 写入共享数据供报告页读取

#### 3.2.5 板块二：煤样瓦斯吸附参数统计分析（Python 保留）

**功能点**：
- 加载区域煤样参数 Excel 文件（双表头格式，含检索地区、煤矿、煤层、煤种、挥发分、VL值、PL值等）
- 地区检索（关键词模糊匹配 + 下拉筛选）
- 挥发分筛选
- 3 种图表类型：散点图、双坐标轴图（VL/PL 共用 X 轴）、分组图（按地区/煤矿分组对比 VL 和 PL 均值）
- 图表参数可配置：X 轴、Y 轴、颜色编码、尺寸编码
- 图表导出（PNG/JPG/PDF）
- 原始数据导出（Excel/CSV）
- 写入共享数据供报告页读取

#### 3.2.6 板块三：煤层瓦斯突出危险性检测（Python 保留）

**核心公式**（游离瓦斯计算）：

```
Xy = V × P × T₀ / (T × P₀ × A)
Q = Xx + Xy
```

其中 T₀ = 273.2K（标准温度），P₀ = 0.101325 MPa（标准压力）

**双重临界值判定**（依据《防治煤与瓦斯突出细则》）：
- 瓦斯压力临界值：P ≥ 0.74 MPa（可自定义）
- 瓦斯含量临界值：8 m³/t（常规）/ 6 m³/t（构造带）（可自定义）
- 任一超标即判定为危险

**功能点**：
- 导入吸附数据文件（含压力 P 和吸附量 Xx 两列）
- 设置游离瓦斯计算参数：孔隙容积 V、温度 t、压缩系数 A
- 设置双重临界值（含量临界值支持下拉选择：常规 8 / 构造带 6 / 自定义）
- 计算游离瓦斯 Xy 和总瓦斯含量 Q
- 总瓦斯含量曲线图（Q = Xx + Xy），含含量和压力两条临界线
- 危险区域填充标注（红色超过含量临界值，橙色超过压力临界值）
- 评估结果文字结论（安全/危险 + 超标原因）
- 导出计算结果（Excel/CSV）
- 保存图表（PNG/JPG）
- 写入共享数据供报告页读取

#### 3.2.7 数据文件管理

- 上传 Excel/CSV 数据文件
- 文件列表查看、删除
- 文件版本管理
- 文件关联到对应计算模块（板块一/二/三）

#### 3.2.8 报告导出

- 自动聚合三个板块的计算结果
- 模块状态卡片（数据可用/暂无数据）
- 详细数据展示（吸附计算结果表、区域统计摘要、危险判定结果）
- 专业安全分析意见生成（基于阈值自动生成文字评价）
- 导出格式：XLSX（多 Sheet）、HTML
- 报告历史记录管理

#### 3.2.9 用户反馈

- 五星评分系统
- 开放式意见收集
- 反馈历史查看

---

## 四、数据库设计概要

### 4.1 ER 概要

```
users ──1:N── data_files ──1:1── calculation_results
users ──1:N── reports
users ──1:N── feedbacks
```

### 4.2 核心表

| 表名 | 说明 |
|------|------|
| `sys_user` | 用户表（id, username, password, role, status, create_time） |
| `data_file` | 数据文件表（id, user_id, module_type, file_name, file_path, upload_time） |
| `task_calculation` | 计算任务表（id, user_id, module_type, file_id, params_json, status, start_time, end_time） |
| `calc_result_analysis` | 板块一计算结果（id, task_id, coal_type, volatile, vl, pl, p_array_json, vm_array_json, stats_text） |
| `calc_result_statistics` | 板块二统计结果（id, task_id, region_list_json, vol_filter, chart_type, color_by, size_by, stats_summary） |
| `calc_result_detection` | 板块三检测结果（id, task_id, v, temperature, a, crit_pressure, crit_content, xy_array_json, q_array_json, p_array_json, is_danger, danger_reason） |
| `report` | 报告表（id, user_id, title, summary_json, file_path, format, create_time） |
| `feedback` | 反馈表（id, user_id, rating, content, create_time） |
| `news` | 新闻资讯表（id, title, url, sort_order, create_time） |

---

## 五、Python 计算服务接口设计

### 5.1 服务概述

Flask 服务独立部署，暴露三个 REST API 端点供 SpringBoot 后端调用。

### 5.2 接口列表

#### 5.2.1 板块一：瓦斯吸附含量计算

```
POST /api/v1/analysis/calculate
```

**请求参数**：
```json
{
  "file_data": [],           // Excel 数据（二维数组或 Base64）
  "coal_type": "焦煤1号",
  "volatile": 20.5,
  "temperature": 30,
  "water_content": 1.5,
  "vl": 25.0,
  "pl": 2.0,
  "reference_temp": 25,
  "p_min": 1.0,
  "p_max": 16.0,
  "p_step": 0.1,
  "chart_style": "curve",    // curve | scatter | bar | area
  "comparison_curves": []    // 对比曲线数据
}
```

**返回结果**：
```json
{
  "p_array": [1.0, 1.1, ...],
  "vm_array": [12.5, 13.1, ...],
  "stats": {
    "max_vm": 40.2,
    "min_vm": 5.1,
    "avg_vm": 22.3,
    "data_points": 151
  },
  "chart_image_base64": "..."  // Matplotlib 生成的图表 PNG（Base64）
}
```

#### 5.2.2 板块二：煤样参数统计分析

```
POST /api/v1/statistics/analyze
```

**请求参数**：
```json
{
  "file_data": [],
  "x_axis": "挥发分",
  "y_axis": "VL值",
  "color_by": "检索地区",
  "size_by": "挥发分",
  "region_filter": "全部",
  "volatile_filter": "全部",
  "chart_type": "scatter"    // scatter | dual_axis | grouped
}
```

**返回结果**：
```json
{
  "chart_image_base64": "...",
  "stats_summary": "数据点: 85 | VL均值: 18.5",
  "region_list": ["山西", "陕西", "内蒙古"],
  "countries": 12
}
```

#### 5.2.3 板块三：煤层瓦斯突出危险性检测

```
POST /api/v1/detection/evaluate
```

**请求参数**：
```json
{
  "adsorption_data": {
    "p_array": [1.0, 2.0, ...],
    "xx_array": [10.0, 15.0, ...]
  },
  "volume": 0.05,
  "temperature": 25,
  "compress_factor": 1.0,
  "crit_pressure": 0.74,
  "crit_content": 8.0
}
```

**返回结果**：
```json
{
  "xy_array": [0.5, 1.0, ...],
  "q_array": [10.5, 16.0, ...],
  "p_array": [1.0, 2.0, ...],
  "is_danger": true,
  "danger_reason": "瓦斯压力超标：当 P ≥ 2.5 MPa 时，...",
  "chart_image_base64": "..."
}
```

---

## 六、前端页面设计

### 6.1 页面清单

| 路由 | 页面 | 对应原页面 |
|------|------|-----------|
| `/` | 登录/注册页 | 新增 |
| `/home` | 首页（导航 + 新闻） | `welcome.py` |
| `/introduction` | 瓦斯介绍 | `introduction.py` |
| `/analysis` | 板块一：吸附含量计算 | `analysis.py` |
| `/statistics` | 板块二：参数统计分析 | `statistics.py` |
| `/detection` | 板块三：突出危险性检测 | `detection.py` |
| `/files` | 数据文件管理 | 新增 |
| `/reports` | 报告列表 / 导出 | `export.py` |
| `/feedback` | 用户反馈 | `feedback.py` |
| `/admin` | 管理后台（管理员） | 新增 |

### 6.2 设计风格延续

保持现有"工业科技蓝"主题色系：
- 主色：`#1A73E8`
- 背景：`#E8F4FD`
- 侧边栏：`#0B3F73`
- 卡片：`#FFFFFF`
- 成功：`#27AE60`，警告：`#F39C12`，危险：`#FF6B6B`

---

## 七、项目目录结构

```
meitan/
├── meitan-server/                    # SpringBoot 后端
│   ├── pom.xml                       # Maven 配置
│   └── src/main/java/com/meitan/
│       ├── MeitanApplication.java    # 启动类
│       ├── config/                   # 配置（Security、CORS、MyBatis）
│       ├── controller/               # REST 控制器
│       ├── service/                  # 业务逻辑
│       ├── mapper/                   # MyBatis Mapper
│       ├── entity/                   # 实体类
│       ├── dto/                      # 数据传输对象
│       └── utils/                    # 工具类
│   └── src/main/resources/
│       ├── application.yml           # 应用配置
│       └── mapper/                   # MyBatis XML
│
├── meitan-web/                       # Vue 3 前端
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── views/                    # 页面组件
│       ├── components/               # 通用组件
│       ├── api/                      # API 请求层
│       ├── router/                   # 路由配置
│       ├── store/                    # Pinia 状态管理
│       └── assets/                   # 静态资源
│
├── meitan-python/                    # Python 计算服务
│   ├── app.py                        # Flask 入口
│   ├── requirements.txt
│   ├── config.py                     # 配置（移植）
│   ├── calculators/
│   │   ├── analysis.py               # 板块一计算引擎（提取自原 ui/analysis.py）
│   │   ├── statistics.py             # 板块二图表引擎（提取自原 ui/statistics.py）
│   │   └── detection.py              # 板块三检测引擎（提取自原 ui/detection.py）
│   └── utils/
│       └── chart_utils.py            # Matplotlib 图表工具
│
└── sql/
    └── init.sql                      # 数据库初始化脚本
```

---

## 八、开发阶段划分

### 阶段一：基础设施
- 数据库设计与建表
- SpringBoot 项目初始化 + MyBatis 配置
- Vue 项目初始化 + 路由 + 布局
- Python Flask 服务搭建

### 阶段二：用户系统 + 首页
- 登录/注册
- 首页布局与导航
- 新闻资讯展示

### 阶段三：三个科学计算模块（Python 迁移）
- 板块一：从 `analysis.py` 提取纯计算逻辑，封装为 Flask API
- 板块二：从 `statistics.py` 提取图表生成逻辑
- 板块三：从 `detection.py` 提取检测计算逻辑

### 阶段四：前后端联调
- 数据文件上传与管理
- 前端调用 Python 计算 API
- ECharts 图表展示（若需要前端渲染图表）

### 阶段五：报告 + 反馈
- 报告生成与导出
- 用户反馈系统

---

## 九、非功能需求

| 类别 | 要求 |
|------|------|
| 性能 | Python 计算接口响应时间 < 5s（含图表生成） |
| 安全性 | JWT 认证，密码 BCrypt 加密存储 |
| 兼容性 | 支持 Chrome/Firefox/Edge 最新两个版本 |
| 数据 | 支持最大 10MB Excel 文件上传 |
| 部署 | SpringBoot JAR 独立部署，Python Flask 独立进程，Nginx 反向代理 |

---

## 十、风险与注意事项

1. **Python 计算依赖**：numpy, pandas, matplotlib 需要预装，且 matplotlib 在无 GUI 环境中需设置 `Agg` 后端
2. **Excel 双表头兼容**：板块二的数据文件为双表头格式，Python 解析逻辑需保留
3. **图表生成方案**：`chart_image_base64` 返回 Matplotlib 生成的静态图片，或由前端 ECharts 根据返回的数据数组自行渲染（推荐后者以减少 Python 服务负担）
4. **共享数据同步**：原 `SharedData` 单例模式平替为 MySQL 中的 `calc_result_*` 表，通过 task_id 关联
5. **DeepSeek AI 助手**：改为前端直接调用 DeepSeek API（需处理跨域），或通过后端代理
