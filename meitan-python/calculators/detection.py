"""板块三：煤层区域突出危险性预测引擎
根据瓦斯压力与瓦斯含量双指标进行区域预测

游离瓦斯公式：
    Xy = V × P × T₀ / (T × P₀ × A)
    Q  = Xx + Xy

双重临界值判定（依据《防治煤与瓦斯突出细则》）：
    - 瓦斯压力临界值：默认可设 0.74 MPa
    - 瓦斯含量临界值：8 m³/t（常规）或 6 m³/t（构造带）
    - 任一超标即判定为危险
"""
import numpy as np
import base64
import io
import matplotlib.pyplot as plt
from config import THEME, T0, P0, DEFAULT_CRIT_PRESSURE, DEFAULT_CRIT_CONTENT


def evaluate_outburst_risk(params: dict) -> dict:
    """
    Args:
        params: {
            "adsorption_data": {
                "p_array": [float],    # 压力数组 (MPa)
                "xx_array": [float],   # 吸附瓦斯量 Xx (m³/t)
            },
            "volume": float,           # 孔隙容积 V (m³/t), 默认0.05
            "temperature": float,      # 温度 t (°C), 默认25
            "compress_factor": float,  # 压缩系数 A, 默认1.0
            "crit_pressure": float,    # 压力临界值 (MPa), 默认0.74
            "crit_content": float      # 含量临界值 (m³/t), 默认8.0
        }
    Returns:
        {
            "xy_array": [float],
            "q_array": [float],
            "p_array": [float],
            "is_danger": bool,
            "danger_reason": str,
            "chart_image_base64": str
        }
    """
    # ── 参数提取 ──
    ads_data = params.get('adsorption_data', {})
    p_array = np.array(ads_data.get('p_array', []), dtype=float)
    xx_array = np.array(ads_data.get('xx_array', []), dtype=float)

    V = float(params.get('volume', 0.05))
    t = float(params.get('temperature', 25))
    A = float(params.get('compress_factor', 1.0))
    crit_pressure = float(params.get('crit_pressure', DEFAULT_CRIT_PRESSURE))
    crit_content = float(params.get('crit_content', DEFAULT_CRIT_CONTENT))

    # ── 参数校验 ──
    if len(p_array) == 0 or len(xx_array) == 0:
        raise ValueError("吸附数据不能为空")
    if len(p_array) != len(xx_array):
        raise ValueError("压力和吸附量数据长度不一致")
    if V <= 0 or A <= 0:
        raise ValueError("V和A必须大于0")

    # ── 计算游离瓦斯 ──
    T_k = t + 273.15
    Xy = V * p_array * T0 / (T_k * P0 * A)
    Q = xx_array + Xy   # 总瓦斯含量

    # ── 双重临界值判定 ──
    pressure_danger = np.any(p_array >= crit_pressure)
    content_danger = np.any(Q >= crit_content)

    if not pressure_danger and not content_danger:
        is_danger = False
        danger_reason = (
            f'无突出危险区：瓦斯压力 P < {crit_pressure} MPa，'
            f'瓦斯含量 W < {crit_content} m³/t。'
        )
    else:
        is_danger = True
        messages = []
        if pressure_danger:
            p_idx = np.where(p_array >= crit_pressure)[0][0]
            p_start = p_array[p_idx]
            messages.append(
                f'瓦斯压力超标：当 P >= {p_start:.2f} MPa 时，'
                f'压力 >= {crit_pressure} MPa'
            )
        if content_danger:
            q_idx = np.where(Q >= crit_content)[0][0]
            q_start = p_array[q_idx]
            messages.append(
                f'总瓦斯含量超标：当 P >= {q_start:.2f} MPa 时，'
                f'含量 >= {crit_content} m³/t'
            )
        danger_reason = '突出危险区：' + '；'.join(messages) + f'。除 P < {crit_pressure} MPa 且 W < {crit_content} m³/t 以外的情况均判为突出危险区。'

    # ── 生成图表 ──
    chart_b64 = _generate_risk_chart(p_array, xx_array, Xy, Q,
                                     crit_pressure, crit_content)

    return {
        'xy_array': [round(float(v), 4) for v in Xy],
        'q_array': [round(float(v), 4) for v in Q],
        'p_array': [round(float(v), 4) for v in p_array],
        'is_danger': is_danger,
        'danger_reason': danger_reason,
        'crit_pressure': crit_pressure,
        'crit_content': crit_content,
        'chart_image_base64': chart_b64,
    }


def _generate_risk_chart(P, Xx, Xy, Q, crit_pressure, crit_content):
    """生成总瓦斯含量 Q = Xx + Xy 曲线（含双重临界线）"""
    fig, ax = plt.subplots(figsize=(10, 6))
    fig.patch.set_facecolor(THEME['card'])
    ax.set_facecolor(THEME['card'])

    ax.plot(P, Xx, '--', color=THEME['secondary'], linewidth=2, label='吸附瓦斯 Xx')
    ax.plot(P, Xy, '--', color=THEME['warning'], linewidth=2, label='游离瓦斯 Xy')
    ax.plot(P, Q, '-', color=THEME['accent'], linewidth=3, label='总瓦斯 Q')

    # 含量临界线（水平线）
    ax.axhline(y=crit_content, color='red', linestyle='--', linewidth=1.5,
               label=f'含量临界值 ({crit_content} m³/t)')
    # 压力临界线（垂直线）
    ax.axvline(x=crit_pressure, color='orange', linestyle=':', linewidth=1.5,
               label=f'压力临界值 ({crit_pressure} MPa)')

    # 填充危险区域
    y_max = max(Q.max(), crit_content * 1.2)
    ax.fill_between(P, crit_content, Q, where=Q >= crit_content,
                    color='red', alpha=0.15, interpolate=True)
    ax.axvspan(crit_pressure, P[-1], color='orange', alpha=0.05)

    # 标注危险起始点
    if np.any(Q >= crit_content):
        first_idx = np.where(Q >= crit_content)[0][0]
        P_start = P[first_idx]
        ax.annotate(f'含量危险起始 P={P_start:.2f} MPa',
                    xy=(P_start, crit_content),
                    xytext=(P_start + 0.5, crit_content * 1.1),
                    arrowprops=dict(arrowstyle='->', color='red'),
                    fontsize=9, color='red')

    if np.any(P >= crit_pressure):
        ax.annotate(f'压力临界值 {crit_pressure} MPa',
                    xy=(crit_pressure, y_max * 0.5),
                    xytext=(crit_pressure + 0.5, y_max * 0.7),
                    arrowprops=dict(arrowstyle='->', color='orange'),
                    fontsize=9, color='orange')

    ax.set_title('煤层区域突出危险性预测（P-W 双指标）',
                 fontsize=14, fontweight='bold', color=THEME['primary'])
    ax.set_xlabel('瓦斯压力 P (MPa)', fontsize=12, color=THEME['text_primary'])
    ax.set_ylabel('瓦斯含量 (m³/t)', fontsize=12, color=THEME['text_primary'])
    ax.grid(True, linestyle='--', alpha=0.3, color=THEME['border'])
    ax.legend(loc='best', fontsize=9)
    ax.set_ylim(0, y_max)
    ax.set_xlim(0, 3)
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)

    plt.tight_layout()

    buf = io.BytesIO()
    fig.savefig(buf, format='png', dpi=150, bbox_inches='tight',
                facecolor=THEME['card'])
    buf.seek(0)
    b64 = base64.b64encode(buf.read()).decode('utf-8')
    buf.close()
    plt.close(fig)

    return b64
