"""煤层瓦斯智能分析平台 - Python 计算服务
封装三个核心科学计算模块的 HTTP API 服务
"""
from flask import Flask, request, jsonify
from flask_cors import CORS
import traceback

from calculators.analysis import calculate_adsorption
from calculators.statistics import generate_statistics_chart
from calculators.detection import evaluate_outburst_risk

app = Flask(__name__)
CORS(app)


# ════════════════════════════════════════════
# 板块一：瓦斯吸附含量计算与分析
# ════════════════════════════════════════════

@app.route('/api/v1/analysis/calculate', methods=['POST'])
def api_analysis():
    """Langmuir吸附模型计算"""
    try:
        data = request.get_json(force=True)
        result = calculate_adsorption(data)
        return jsonify(result)
    except ValueError as e:
        return jsonify({'error': str(e)}), 400
    except Exception as e:
        traceback.print_exc()
        return jsonify({'error': f'计算失败: {str(e)}'}), 500


# ════════════════════════════════════════════
# 板块二：煤样瓦斯吸附参数统计分析
# ════════════════════════════════════════════

@app.route('/api/v1/statistics/analyze', methods=['POST'])
def api_statistics():
    """煤样参数统计图表生成"""
    try:
        data = request.get_json(force=True)
        result = generate_statistics_chart(data)
        return jsonify(result)
    except ValueError as e:
        return jsonify({'error': str(e)}), 400
    except Exception as e:
        traceback.print_exc()
        return jsonify({'error': f'统计分析失败: {str(e)}'}), 500


# ════════════════════════════════════════════
# 板块三：煤层瓦斯突出危险性检测
# ════════════════════════════════════════════

@app.route('/api/v1/detection/evaluate', methods=['POST'])
def api_detection():
    """突出危险性双重临界值评估"""
    try:
        data = request.get_json(force=True)
        result = evaluate_outburst_risk(data)
        return jsonify(result)
    except ValueError as e:
        return jsonify({'error': str(e)}), 400
    except Exception as e:
        traceback.print_exc()
        return jsonify({'error': f'检测失败: {str(e)}'}), 500


# ════════════════════════════════════════════
# 健康检查
# ════════════════════════════════════════════

@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'ok', 'service': 'meitan-python-service'})


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=False)
