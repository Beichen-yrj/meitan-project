"""板块一：瓦斯吸附量计算与分析引擎
从原 ui/analysis.py 提取纯计算逻辑，适配 HTTP API

Langmuir 吸附公式：
    α = exp(-(-5.079e-4 × Vdaf² + 0.028 × Vdaf - 0.015) × w)
    λ = exp(-0.009 × P^(-0.286) × (T - T₀))
    Vm = (Vl × P) / (Pl + P) × α × λ
"""
import numpy as np
import math
import base64
import io
import matplotlib.pyplot as plt
from config import THEME, T0 as REF_T0, P0


def calculate_adsorption(params: dict) -> dict:
    """
    Args:
        params: {
            "coal_type": str,        # 煤型及编号
            "volatile": float,       # 挥发分 Vdaf (%)
            "temperature": float,    # 温度 T (°C)
            "water_content": float,  # 含水率 w (%)
            "vl": float,             # Langmuir Vl值 (cm³/g or m³/t)
            "pl": float,             # Langmuir Pl值 (MPa)
            "reference_temp": float, # 参考温度 T0 (°C), 默认25
            "p_min": float,          # 最小压力 (MPa), 默认0
            "p_max": float,          # 最大压力 (MPa), 默认16
            "p_step": float,         # 压力步长 (MPa), 默认0.1
            "chart_style": str,      # curve | scatter | bar | area
            "comparison_curves": []  # 对比曲线列表
        }
    Returns:
        {
            "p_array": [float],      # 压力数组
            "vm_array": [float],     # 吸附量数组
            "stats": dict,           # 统计信息
            "chart_image_base64": str  # 图表PNG Base64
        }
    """
    # ── 参数提取 ──
    coal_type = params.get('coal_type', '未知')
    Vdaf = float(params.get('volatile', 0))
    T = float(params.get('temperature', 0))
    w = float(params.get('water_content', 0))
    Vl = float(params.get('vl', 0))
    Pl = float(params.get('pl', 0))
    T0 = float(params.get('reference_temp', 25))
    P_min = float(params.get('p_min', 0))
    P_max = float(params.get('p_max', 16))
    P_step = float(params.get('p_step', 0.1))
    chart_style = params.get('chart_style', 'curve')
    comparison_curves = params.get('comparison_curves', [])

    # ── 参数校验 ──
    if P_min >= P_max:
        raise ValueError("最小压力必须小于最大压力")
    if P_min < 0:
        raise ValueError("最小压力不能小于0")
    if P_step <= 0:
        raise ValueError("压力步长必须大于0")

    # ── 计算 P-Vm ──
    P_values = np.arange(P_min, P_max + P_step, P_step)
    Vm_values = []

    for P in P_values:
        if P != 0 and Pl + P != 0:
            Alpha = math.exp(-(-5.079e-4 * Vdaf ** 2 + 0.028 * Vdaf - 0.015) * w)
            Lambda = math.exp(-0.009 * P ** -0.286 * (T - T0))
            Vm = (Vl * P) / (Pl + P) * Alpha * Lambda
            Vm_values.append(Vm)
        else:
            Vm_values.append(0.0)

    # ── 统计信息 ──
    vm_arr = np.array(Vm_values)
    stats = {
        'data_points': len(P_values),
        'max_vm': round(float(vm_arr.max()), 4),
        'min_vm': round(float(vm_arr.min()), 4),
        'avg_vm': round(float(vm_arr.mean()), 4),
        'pressure_range': f'{P_min} - {P_max} MPa',
    }

    # ── 生成图表 ──
    chart_b64 = _generate_chart(P_values, Vm_values, coal_type, T, Vdaf,
                                chart_style, comparison_curves, Vl, Pl)

    return {
        'p_array': [round(float(p), 4) for p in P_values],
        'vm_array': [round(float(v), 6) for v in Vm_values],
        'stats': stats,
        'coal_type': coal_type,
        'chart_image_base64': chart_b64,
    }


def _generate_chart(P_values, Vm_values, coal_type, T, Vdaf,
                    chart_style, comparison_curves, Vl, Pl):
    """生成 Matplotlib 图表并返回 Base64"""
    fig, ax = plt.subplots(figsize=(10, 6))
    fig.patch.set_facecolor(THEME['card'])
    ax.set_facecolor(THEME['card'])

    ax.set_title('煤层瓦斯吸附定量分析图', fontsize=16, fontweight='bold',
                 color=THEME['primary'])
    ax.set_xlabel('气体压力 P (MPa)', fontsize=12, color=THEME['text_primary'])
    ax.set_ylabel('气体吸附量 Vm (cm³/g)', fontsize=12, color=THEME['text_primary'])
    ax.grid(True, linestyle='--', alpha=0.3, color=THEME['border'])
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)

    # 对比曲线
    colors = ['#E53935', '#8E24AA', '#3949AB', '#00ACC1', '#43A047',
              '#FFB300', '#6D4C41', '#546E7A']
    for i, curve in enumerate(comparison_curves):
        cp = curve.get('p_array', [])
        cv = curve.get('vm_array', [])
        if cp and cv:
            color = colors[i % len(colors)]
            ax.plot(cp, cv, color=color, linestyle='--', linewidth=1.5, alpha=0.7,
                    label=curve.get('label', f'对比{i + 1}'))

    # 主曲线
    label = f'{coal_type} | T={T}°C, Vdaf={Vdaf}%'
    if chart_style == 'scatter':
        ax.scatter(P_values, Vm_values, color=THEME['accent'], s=50, alpha=0.8, label=label)
    elif chart_style == 'bar':
        width = P_values[1] - P_values[0] if len(P_values) > 1 else 0.5
        ax.bar(P_values, Vm_values, width=width, color=THEME['primary'], alpha=0.7, label=label)
    elif chart_style == 'area':
        ax.fill_between(P_values, Vm_values, color=THEME['primary'], alpha=0.3, label=label)
        ax.plot(P_values, Vm_values, color=THEME['primary'], linewidth=2)
    else:  # curve (default)
        marker = 'o' if len(P_values) <= 20 else ''
        ax.plot(P_values, Vm_values, color=THEME['primary'], linewidth=2.5,
                marker=marker, markersize=5, label=label)

    # 图例
    if len(comparison_curves) > 0 or len(P_values) > 0:
        ax.legend(loc='best', fontsize=9, framealpha=0.9)

    # 公式标注
    formula_text = r'$V_m = \frac{V_L \cdot P}{P_L + P} \times \alpha \times \lambda$'
    ax.text(0.02, 0.98, formula_text, transform=ax.transAxes, fontsize=10,
            verticalalignment='top',
            bbox=dict(boxstyle='round', facecolor='white', alpha=0.8))

    # 坐标轴范围
    if len(P_values) > 0:
        ax.set_xlim(max(0, min(P_values) - 0.1), max(P_values) * 1.1)
        ax.set_ylim(0, max(Vm_values) * 1.15)

    plt.tight_layout()

    buf = io.BytesIO()
    fig.savefig(buf, format='png', dpi=150, bbox_inches='tight',
                facecolor=THEME['card'])
    buf.seek(0)
    b64 = base64.b64encode(buf.read()).decode('utf-8')
    buf.close()
    plt.close(fig)

    return b64
