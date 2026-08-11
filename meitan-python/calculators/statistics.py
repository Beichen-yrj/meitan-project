"""板块二：煤层瓦斯吸附参数统计引擎
从原 ui/statistics.py 提取图表生成逻辑
"""
import numpy as np
import pandas as pd
import base64
import io
import matplotlib.pyplot as plt
from matplotlib import colormaps
from config import THEME


def generate_statistics_chart(params: dict) -> dict:
    """
    Args:
        params: {
            "file_data": [[col1, col2, ...], [row1_col1, row1_col2, ...], ...],
                         第一行为表头，第二行可能为双表头的第二行
            "x_axis": str,
            "y_axis": str,
            "color_by": str,
            "size_by": str,
            "region_filter": str,   # "全部" 或具体地区名
            "volatile_filter": str, # "全部" 或具体挥发分值
            "chart_type": str       # scatter | dual_axis | grouped
        }
    Returns:
        {
            "chart_image_base64": str,
            "stats_summary": str,
            "region_list": [str],
            "countries": int
        }
    """
    chart_type = params.get('chart_type', 'scatter')
    x_axis = params.get('x_axis', '挥发分')
    y_axis = params.get('y_axis', 'VL值')
    color_by = params.get('color_by', '检索地区')
    size_by = params.get('size_by', '挥发分')
    region_filter = params.get('region_filter', '全部')
    volatile_filter = params.get('volatile_filter', '全部')

    # ── 构建 DataFrame ──
    if 'file_data' in params and params['file_data']:
        file_data = params['file_data']
        headers = file_data[0]
        rows = file_data[1:]
        # 双表头检测
        if '挥发分' in [str(v) for v in rows[0]]:
            rows = rows[1:]
        df = pd.DataFrame(rows, columns=headers)
    else:
        # 返回空结果
        return {
            'chart_image_base64': '',
            'stats_summary': '无数据',
            'region_list': [],
        }

    # ── 数据清洗 ──
    df.columns = df.columns.astype(str).str.strip()
    if '检索地区' in df.columns:
        df['检索地区'] = df['检索地区'].fillna('未知').astype(str)
    text_cols = ['检索地区', '煤矿', '煤层', '煤种']
    for col in df.columns:
        if col not in text_cols:
            df[col] = pd.to_numeric(df[col], errors='coerce')

    # ── 筛选 ──
    data = df.copy()
    if region_filter != '全部' and '检索地区' in data.columns:
        data = data[data['检索地区'].astype(str) == region_filter]
    if volatile_filter != '全部' and '挥发分' in data.columns:
        try:
            target = float(volatile_filter)
            data = data[data['挥发分'] == target]
        except (ValueError, TypeError):
            pass

    if data.empty:
        raise ValueError(f"筛选后无数据: 地区={region_filter}, 挥发分={volatile_filter}")

    # ── 填充文本缺失 ──
    for col in ['检索地区', '煤矿', '煤层', '煤种']:
        if col in data.columns:
            data[col] = data[col].fillna('未知')

    # ── 生成图表 ──
    if chart_type == 'dual_axis':
        chart_b64 = _generate_dual_axis(data, x_axis, region_filter, volatile_filter)
    elif chart_type == 'grouped':
        chart_b64 = _generate_grouped(data, region_filter, volatile_filter)
    else:
        chart_b64 = _generate_scatter(data, x_axis, y_axis, color_by, size_by,
                                      region_filter, volatile_filter)

    # ── 统计摘要 ──
    region_list = sorted(data['检索地区'].dropna().unique().tolist()) \
        if '检索地区' in data.columns else []
    stats = f"数据点: {len(data)} | {y_axis}均值: {data[y_axis].mean():.2f}"

    return {
        'chart_image_base64': chart_b64,
        'stats_summary': stats,
        'region_list': region_list,
        'countries': len(region_list),
    }


def _generate_scatter(data, x_param, y_param, color_param, size_param,
                       region_filter, volatile_filter):
    """散点图"""
    fig, ax = plt.subplots(figsize=(10, 6))
    fig.patch.set_facecolor(THEME['card'])
    ax.set_facecolor(THEME['card'])

    x_vals = pd.to_numeric(data[x_param], errors='coerce').values
    y_vals = pd.to_numeric(data[y_param], errors='coerce').values

    # 颜色编码
    try:
        color_vals = pd.to_numeric(data[color_param], errors='coerce')
        is_numeric_color = color_vals.notna().any()
    except Exception:
        is_numeric_color = False

    if is_numeric_color:
        cmap = colormaps['viridis']
        norm = plt.Normalize(color_vals.min(), color_vals.max())
        ax.scatter(x_vals, y_vals, c=color_vals.values, cmap=cmap, s=80,
                   alpha=0.7, edgecolors='white', linewidth=0.5)
        plt.colorbar(ax.collections[0], ax=ax, label=color_param)
    else:
        color_series = data[color_param].fillna('未知').astype(str)
        cats = color_series.unique()
        cmap = colormaps['tab20']
        for i, cat in enumerate(cats):
            mask = color_series == cat
            ax.scatter(x_vals[mask.values], y_vals[mask.values],
                       color=cmap(i / max(len(cats) - 1, 1)),
                       s=80, alpha=0.7, edgecolors='white', linewidth=0.5,
                       label=str(cat))
        ax.legend(title=color_param, fontsize=8, loc='upper left',
                  bbox_to_anchor=(1.02, 1), borderaxespad=0)

    title = f"煤样参数分布 - {y_param} vs {x_param}"
    if region_filter != '全部':
        title += f" (地区: {region_filter})"
    ax.set_title(title, fontsize=14, fontweight='bold', color=THEME['primary'])
    ax.set_xlabel(x_param, fontsize=11, color=THEME['text_primary'])
    ax.set_ylabel(y_param, fontsize=11, color=THEME['text_primary'])
    ax.grid(True, linestyle='--', alpha=0.3, color=THEME['border'])
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)

    stats_text = f"数据点: {len(data)} | {y_param}均值: {y_vals.mean():.2f}"
    ax.text(0.02, 0.98, stats_text, transform=ax.transAxes, fontsize=10,
            verticalalignment='top',
            bbox=dict(boxstyle='round', facecolor='white', alpha=0.8))

    plt.tight_layout()
    return _fig_to_base64(fig)


def _generate_dual_axis(data, x_param, region_filter, volatile_filter):
    """双坐标轴图：VL值用左轴，PL值用右轴"""
    fig, ax1 = plt.subplots(figsize=(10, 6))
    fig.patch.set_facecolor(THEME['card'])
    ax1.set_facecolor(THEME['card'])
    ax2 = ax1.twinx()

    x_vals = pd.to_numeric(data[x_param], errors='coerce').values
    vl_vals = pd.to_numeric(data['VL值'], errors='coerce').values
    pl_vals = pd.to_numeric(data['PL值'], errors='coerce').values

    ax1.scatter(x_vals, vl_vals, color=THEME['secondary'], s=80, alpha=0.7,
                label='VL值', edgecolors='white', linewidth=0.5)
    ax2.scatter(x_vals, pl_vals, color=THEME['accent'], s=80, alpha=0.7,
                label='PL值', marker='s', edgecolors='white', linewidth=0.5)

    # 排序连线
    sorted_idx = np.argsort(x_vals)
    ax1.plot(x_vals[sorted_idx], vl_vals[sorted_idx], color=THEME['secondary'],
             linewidth=1.5, alpha=0.5)
    ax2.plot(x_vals[sorted_idx], pl_vals[sorted_idx], color=THEME['accent'],
             linewidth=1.5, alpha=0.5, linestyle='--')

    title = f"VL和PL值对比 - {x_param}"
    if region_filter != '全部':
        title += f" (地区: {region_filter})"
    ax1.set_title(title, fontsize=14, fontweight='bold', color=THEME['primary'])
    ax1.set_xlabel(x_param, fontsize=11, color=THEME['text_primary'])
    ax1.set_ylabel('VL值 (cm³/g)', fontsize=11, color=THEME['secondary'])
    ax2.set_ylabel('PL值 (MPa)', fontsize=11, color=THEME['accent'])
    ax1.grid(True, linestyle='--', alpha=0.3, color=THEME['border'])

    lines1, labels1 = ax1.get_legend_handles_labels()
    lines2, labels2 = ax2.get_legend_handles_labels()
    ax1.legend(lines1 + lines2, labels1 + labels2, loc='upper left', fontsize=9)

    plt.tight_layout()
    return _fig_to_base64(fig)


def _generate_grouped(data, region_filter, volatile_filter):
    """分组图：按地区分组的VL和PL均值对比"""
    if '检索地区' not in data.columns:
        raise ValueError('数据中没有"检索地区"列')

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(12, 6))
    fig.patch.set_facecolor(THEME['card'])
    for ax in [ax1, ax2]:
        ax.set_facecolor(THEME['card'])

    groups = sorted(data['检索地区'].unique())
    vl_means, pl_means = [], []
    for g in groups:
        gd = data[data['检索地区'] == g]
        vl_means.append(gd['VL值'].mean())
        pl_means.append(gd['PL值'].mean())

    x_pos = np.arange(len(groups))
    ax1.bar(x_pos, vl_means, 0.5, color=THEME['secondary'], alpha=0.7)
    ax2.bar(x_pos, pl_means, 0.5, color=THEME['accent'], alpha=0.7)

    for ax, title_suffix in [(ax1, 'VL值'), (ax2, 'PL值')]:
        ax.set_title(f'{title_suffix}分组对比', fontsize=13, fontweight='bold',
                     color=THEME['primary'])
        ax.set_xlabel('检索地区', fontsize=10)
        ax.set_xticks(x_pos)
        ax.set_xticklabels(groups, rotation=45, ha='right', fontsize=8)
        ax.grid(True, linestyle='--', alpha=0.3, color=THEME['border'], axis='y')

    ax1.set_ylabel('VL值 (cm³/g)', fontsize=11, color=THEME['secondary'])
    ax2.set_ylabel('PL值 (MPa)', fontsize=11, color=THEME['accent'])

    title = '按地区分组的VL和PL值对比'
    if region_filter != '全部':
        title += f" (地区: {region_filter})"
    fig.suptitle(title, fontsize=15, fontweight='bold', color=THEME['primary'])

    plt.tight_layout()
    return _fig_to_base64(fig)


def _fig_to_base64(fig):
    """Matplotlib Figure -> Base64 PNG"""
    buf = io.BytesIO()
    fig.savefig(buf, format='png', dpi=150, bbox_inches='tight',
                facecolor=THEME['card'])
    buf.seek(0)
    b64 = base64.b64encode(buf.read()).decode('utf-8')
    buf.close()
    plt.close(fig)
    return b64
