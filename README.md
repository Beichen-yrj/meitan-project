# 煤层瓦斯智能分析平台 — 技术架构与功能说明

> 版本：v1.0 | 更新日期：2026-08-02

---

## 1. 项目概述

本平台是一个煤层瓦斯安全管理与科学计算系统，面向煤矿安全工程人员，提供瓦斯吸附量计算、煤样参数统计分析和突出危险性检测三大核心功能。

系统采用前后端分离架构，计算逻辑由 Python Flask 微服务承载，通过 Spring Boot 网关统一对外暴露 API，Vue 3 前端提供交互界面。

---

## 2. 技术栈

### 2.1 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.x | 组件化 UI 框架 |
| Vite | 5.4 | 开发服务器与构建工具 |
| Element Plus | 2.x | UI 组件库（表单、表格、卡片、菜单等） |
| Vue Router | 4.x | 前端路由与导航守卫 |
| Pinia | 2.x | 用户状态管理与 localStorage 持久化 |
| Axios | 1.x | HTTP 请求与拦截器 |
| ECharts | 5.x | （预留）数据图表渲染 |
| SheetJS (xlsx) | 0.18 | Excel 文件前端解析 |

### 2.2 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 运行环境 |
| Spring Boot | 3.2.0 | REST API 框架 |
| Spring Security | 6.x | 接口安全、BCrypt 密码加密 |
| MyBatis-Plus | 3.5.5 | MySQL ORM 与分页 |
| JJWT | 0.12 | JWT Token 生成与验证 |
| MySQL Connector | 8.x | 数据库驱动 |
| Hutool | 5.x | 通用工具类 |
| Lombok | - | 简化实体/服务代码 |

### 2.3 Python 计算服务

| 技术 | 用途 |
|------|------|
| Flask + Flask-CORS | HTTP API 服务 |
| NumPy | 数值计算 |
| Pandas | 表格数据分析 |
| Matplotlib | 服务端图表生成（PNG Base64） |
| OpenPyXL | Excel 读取 |

### 2.4 数据库

- **MySQL 8.0+**，数据库名 `meitan`
- 字符集 `utf8mb4`，时区 `Asia/Shanghai`
- MyBatis-Plus 自动下划线转驼峰

---

## 3. 项目结构

```
meitan/
├── docs/                                  # 项目文档
├── sql/init.sql                           # 数据库初始化脚本（9 张表 + 初始数据）
├── PRD_需求文档.md                         # 产品需求规格说明
├── .gitignore
│
├── meitan-web/                            # Vue 3 前端
│   ├── package.json                       # 依赖：Vue3、Element Plus、Axios、Pinia、xlsx
│   ├── vite.config.js                     # Vite 配置 + /api 代理到 :8080
│   ├── index.html
│   └── src/
│       ├── App.vue                        # 根组件
│       ├── main.js                        # 入口：挂载 Element Plus、Router、Pinia
│       ├── api/
│       │   ├── request.js                 # Axios 实例 + 请求/响应拦截器（JWT 注入、401 处理）
│       │   └── index.js                   # API 函数：auth、user、news、calc、files、reports、feedback
│       ├── router/
│       │   └── index.js                   # 路由表 + 登录守卫
│       ├── store/
│       │   └── user.js                    # Pinia 用户状态（token/userId/username/realName/role）
│       ├── assets/
│       │   └── styles/
│       │       └── global.css             # 科技蓝主题 CSS 变量
│       ├── components/
│       │   └── Layout.vue                 # 主布局：侧边栏菜单 + 顶部栏 + 用户退出
│       └── views/
│           ├── Login.vue                  # 登录/注册页
│           ├── Home.vue                   # 首页：功能导航卡片 + 新闻时间线
│           ├── Introduction.vue           # 煤层瓦斯科普介绍
│           ├── Analysis.vue               # 板块一：瓦斯吸附含量计算
│           ├── Statistics.vue             # 板块二：煤样参数统计分析
│           ├── Detection.vue              # 板块三：突出危险性检测
│           ├── Files.vue                  # 数据文件管理
│           ├── Reports.vue                # 报告导出
│           ├── Feedback.vue               # 用户反馈
│           └── UserCenter.vue             # 个人中心
│
├── meitan-server/                         # Spring Boot 后端
│   ├── pom.xml                            # Maven 依赖
│   └── src/main/
│       ├── resources/
│       │   └── application.yml            # 端口 8080、数据库连接、JWT 配置
│       └── java/com/meitan/
│           ├── MeitanApplication.java     # Spring Boot 启动类
│           ├── config/
│           │   ├── SecurityConfig.java    # Spring Security：公开/认证/管理员路由 + JWT 过滤器
│           │   ├── CorsConfig.java        # CORS 跨域配置
│           │   ├── MyBatisPlusConfig.java # MyBatis-Plus 配置 + 自动填充时间戳
│           │   └── MyMetaObjectHandler.java # create_time/update_time 自动填充
│           ├── controller/
│           │   ├── AuthController.java    # POST /auth/login, /auth/register
│           │   ├── UserController.java    # GET|PUT /user/profile, PUT /user/password
│           │   ├── PublicController.java  # GET /public/news（公开新闻列表）
│           │   ├── CalcController.java    # POST /analysis|statistics|detection → Python
│           │   ├── NewsController.java    # CRUD /admin/news（管理员新闻管理）
│           │   ├── FileController.java    # POST /files/upload
│           │   └── FeedbackController.java# GET /admin/feedback（管理员反馈）
│           ├── dto/
│           │   ├── ApiResponse.java       # 统一响应 {code, message, data}
│           │   ├── LoginRequest.java      # 登录请求体
│           │   ├── LoginResponse.java     # 登录响应（token + 用户信息）
│           │   ├── RegisterRequest.java   # 注册请求体
│           │   └── ChangePasswordRequest.java # 修改密码请求体
│           ├── entity/
│           │   ├── User.java              # sys_user 表
│           │   ├── DataFile.java          # data_file 表
│           │   ├── TaskCalculation.java   # task_calculation 表
│           │   ├── CalcResultAnalysis.java# calc_result_analysis 表
│           │   ├── CalcResultStatistics.java # calc_result_statistics 表
│           │   ├── CalcResultDetection.java # calc_result_detection 表
│           │   ├── Report.java            # report 表
│           │   ├── News.java              # news 表
│           │   └── Feedback.java          # feedback 表
│           ├── mapper/                    # MyBatis-Plus Mapper 接口（8 个）
│           ├── service/
│           │   ├── AuthService.java       # 登录（明文→BCrypt 升级）、注册、修改密码、用户管理
│           │   ├── FileService.java       # 文件上传与存储
│           │   └── PythonClientService.java # HTTP 调用 Python 计算服务
│           └── utils/
│               └── JwtUtils.java          # JWT 生成/解析/校验
│
└── meitan-python/                         # Python Flask 计算服务
    ├── app.py                             # Flask 入口（3 个计算端点 + 健康检查）
    ├── config.py                          # 全局配置（T0、P0、matplotlib 主题色）
    ├── requirements.txt                   # flask, numpy, pandas, matplotlib, openpyxl
    └── calculators/
        ├── analysis.py                    # 板块一：Langmuir 吸附 Vm = Vl·P/(Pl+P) × α × λ
        ├── statistics.py                  # 板块二：散点图/双轴图/分组图生成
        └── detection.py                   # 板块三：游离瓦斯 Xy + 双重临界值判定
```

---

## 4. 数据流架构

```
┌──────────┐    HTTP/JSON     ┌──────────────┐    HTTP/JSON     ┌──────────────┐
│  Vue 3   │ ───────────────> │  Spring Boot │ ───────────────> │  Python      │
│  前端    │ <─────────────── │  后端 :8080  │ <─────────────── │  Flask :5000 │
│  :3000   │   ApiResponse    │              │  计算结果+图表    │              │
└──────────┘                  └──────┬───────┘                  └──────────────┘
                                     │
                                     │ MyBatis-Plus / JDBC
                                     ▼
                              ┌──────────────┐
                              │   MySQL      │
                              │   :3306      │
                              │   DB: meitan │
                              └──────────────┘
```

**请求流程示例（板块一计算）：**

1. 用户在前端 Analysis.vue 填写参数 → 点击「开始计算并绘图」
2. Vue 调用 `POST /api/analysis/calculate`（camelCase 参数）
3. CalcController 将 camelCase 转为 snake_case
4. PythonClientService 发送 HTTP POST 到 `http://localhost:5000/api/v1/analysis/calculate`
5. Python `calculate_adsorption()` 执行 Langmuir 公式计算 + matplotlib 制图 → 返回 `{p_array, vm_array, stats, chart_image_base64}`
6. 结果经过 CalcController → ApiResponse 包装返回前端
7. 前端展示 PNG 图表 + 统计信息 + P-Vm 数据表

---

## 5. 数据库表

| 表名 | 说明 |
|------|------|
| `sys_user` | 用户账号、密码（BCrypt）、角色（ADMIN/USER）、状态（启用/禁用）、邮箱、手机号 |
| `data_file` | 用户上传的 Excel/CSV 数据文件 |
| `task_calculation` | 计算任务记录 |
| `calc_result_analysis` | 板块一：吸附分析结果 |
| `calc_result_statistics` | 板块二：统计分析结果 |
| `calc_result_detection` | 板块三：危险性检测结果 |
| `report` | 导出的报告记录 |
| `feedback` | 用户反馈（评分、内容、状态、管理员回复） |
| `news` | 首页新闻资讯（标题、来源、URL、排序） |

建表脚本：`sql/init.sql`

---

## 6. API 接口清单

### 6.1 公开接口（无需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 用户登录，返回 JWT Token 和用户信息 |
| POST | `/api/auth/register` | 用户注册 |
| GET | `/api/public/news` | 获取首页新闻列表（status=1，按排序） |

### 6.2 用户接口（需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/profile` | 获取当前用户信息 |
| PUT | `/api/user/profile` | 修改个人信息（姓名、邮箱、手机号） |
| PUT | `/api/user/password` | 修改密码（需验证原密码） |
| POST | `/api/files/upload` | 上传数据文件 |

### 6.3 计算接口（需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/analysis/calculate` | 板块一：Langmuir 吸附量计算 + 图表 |
| POST | `/api/statistics/analyze` | 板块二：煤样参数统计图表生成 |
| POST | `/api/detection/evaluate` | 板块三：游离瓦斯 + 双重临界值判定 |

### 6.4 管理员接口（需 ADMIN 角色）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/list` | 查看所有用户列表 |
| PUT | `/api/user/status/{id}` | 启用/禁用用户 |
| GET | `/api/admin/news` | 查看所有新闻 |
| POST | `/api/admin/news` | 新增新闻 |
| PUT | `/api/admin/news/{id}` | 修改新闻 |
| DELETE | `/api/admin/news/{id}` | 删除新闻 |
| GET | `/api/admin/feedback` | 查看所有反馈 |

### 6.5 Python 计算服务

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/analysis/calculate` | Langmuir 吸附计算 |
| POST | `/api/v1/statistics/analyze` | 统计图表生成 |
| POST | `/api/v1/detection/evaluate` | 危险性检测 |
| GET | `/health` | 健康检查 |

---

## 7. 功能模块说明

### 7.1 用户认证

- 登录返回 JWT Token（24 小时有效期），前端存入 Pinia + localStorage
- 密码支持明文→BCrypt 自动升级（首次登录后自动加密）
- 路由守卫：未登录自动跳转 `/login`
- Axios 拦截器自动在请求头注入 `Authorization: Bearer <token>`

### 7.2 板块一：瓦斯吸附含量计算与分析

- **科学模型**：Langmuir 等温吸附公式
  - `α = exp(-(-5.079e-4 × Vdaf² + 0.028 × Vdaf - 0.015) × w)`
  - `λ = exp(-0.009 × P^(-0.286) × (T - T₀))`
  - `Vm = (Vl × P) / (Pl + P) × α × λ`
- **参数**：煤型、挥发分 Vdaf、温度 T、含水率 w、Vl、Pl、参考温度 T₀、压力范围
- **输出**：P-Vm 吸附曲线图（PNG）+ 统计信息 + 原始数据表
- **图表样式**：曲线图 / 散点图 / 柱状图 / 面积图

### 7.3 板块二：煤样瓦斯吸附参数统计分析

- 上传 Excel 数据文件（xlsx 库前端解析为二维数组）
- 图表类型：散点图、双坐标轴图（VL+PL）、分组柱状图
- 可筛选地区（山西/陕西/河南/全部等）
- X/Y 轴字段选择、颜色/尺寸编码
- 输出统计摘要 + 图表

### 7.4 板块三：煤层瓦斯突出危险性检测

- **计算公式**：`Xy = V × P × T₀ / (T × P₀ × A)`，`Q = Xx + Xy`
- **双重临界值判定**（依据《防治煤与瓦斯突出细则》）：
  - 压力临界值默认 0.74 MPa
  - 含量临界值：常规区域 8 m³/t / 构造带 6 m³/t
  - 任一超标即判定为「危险」
- 输入吸附数据（P 和 Xx 数组），结果含超标标记表格

### 7.5 数据文件管理

- 支持上传 `.xlsx` / `.xls` / `.csv` 文件
- 按模块分类（analysis / statistics / detection）
- 文件列表、大小格式化、删除确认

### 7.6 用户反馈

- 1~5 星评分 + 文字内容
- 提交后存入 `feedback` 表

### 7.7 个人中心

- 查看/修改个人信息（姓名、邮箱、手机号）
- 修改密码（验证原密码 → BCrypt 加密新密码）
- 角色标签显示

### 7.8 首页

- 6 张功能导航卡片跳转到各模块
- 新闻资讯时间线（从 `news` 表读取，公开接口）

### 7.9 权限控制

| 角色 | 权限范围 |
|------|----------|
| ADMIN | 全部功能 + 用户管理 + 新闻管理 + 反馈管理 |
| USER | 个人计算/文件/反馈/个人中心 |

---

## 8. 开发环境启动

### 8.1 环境要求

- JDK 17、Maven 3.8+
- MySQL 8.0+（root / 123456 或自行修改 `application.yml`）
- Node.js 18+、npm 9+
- Python 3.10+（计算服务）

### 8.2 启动步骤

```powershell
# 1. 初始化数据库
cd meitan
mysql -u root -p < sql/init.sql

# 2. 启动 Python 计算服务
cd meitan-python
pip install -r requirements.txt
python app.py                         # :5000

# 3. 启动 Spring Boot 后端（新终端）
cd meitan-server
mvn spring-boot:run                   # :8080

# 4. 启动 Vue 前端（新终端）
cd meitan-web
npm install
npm run dev                           # :3000
```

### 8.3 默认账号

- 用户名：`admin`，密码：`admin123`（首次登录后自动 BCrypt 加密）

### 8.4 配置修改

- 数据库连接：`meitan-server/src/main/resources/application.yml`
- JWT 密钥与过期时间：同上 `jwt.secret` / `jwt.expiration`
- Python 服务地址：同上 `python.service.url`

---

## 9. 常见问题

| 问题 | 解决方案 |
|------|----------|
| 后端启动报 `NoClassDefFoundError: RegisterRequest` | 确保 `RegisterRequest.java` 在 `dto/` 目录下，执行 `mvn clean compile` |
| 数据库连接失败 `utf8mb4` 不支持 | 将 `characterEncoding=utf8mb4` 改为 `characterEncoding=UTF-8` |
| `create_time cannot be null` | 检查 `MyMetaObjectHandler` 是否正确注册 |
| 前端 401 后白屏 | 非登录接口的 401 才会退出，登录接口 401 只提示错误 |
| Python 服务返回 500 | 检查参数名是否为 snake_case，数值是否合法 |
| npm 缓存权限错误 `EPERM` | 设置项目本地缓存：`npm config set cache "项目路径\.npm-cache"` |
| 端口被占用 | 修改 `application.yml` 的 `server.port` 或 `vite.config.js` 的代理端口 |
