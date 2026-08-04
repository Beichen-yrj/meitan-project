"""全局配置"""
import os

# 标准状况
T0 = 273.2       # 标准温度 K
P0 = 0.101325    # 标准压力 MPa

# 临界值默认值
DEFAULT_CRIT_PRESSURE = 0.74   # 瓦斯压力临界值 MPa
DEFAULT_CRIT_CONTENT = 8.0     # 瓦斯含量临界值 m³/t（常规）
DEFAULT_CRIT_CONTENT_STRUCT = 6.0  # 构造带临界值

# matplotlib 设置
import matplotlib
matplotlib.use('Agg')  # 非GUI后端，用于服务器环境
import matplotlib.pyplot as plt
plt.rcParams['font.family'] = 'sans-serif'
plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei', 'WenQuanYi Micro Hei']
plt.rcParams['axes.unicode_minus'] = False

# 图表配色（保持与原项目一致的蓝色主题）
THEME = {
    'primary': '#1A73E8',
    'primary_dark': '#0D47A1',
    'secondary': '#42A5F5',
    'accent': '#FF6B6B',
    'success': '#27AE60',
    'warning': '#F39C12',
    'info': '#3498DB',
    'background': '#E8F4FD',
    'card': '#FFFFFF',
    'border': '#90CAF9',
    'text_primary': '#1A237E',
    'text_secondary': '#546E7A',
}
