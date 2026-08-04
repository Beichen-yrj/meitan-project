package com.meitan.controller;

import com.meitan.dto.ApiResponse;
import com.meitan.service.PythonClientService;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/** 三个计算板块：对接 Python Flask 计算服务
 *  自动将前端 camelCase 参数转为 Python 期望的 snake_case
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CalcController {

    private final PythonClientService pythonClient;

    @PostMapping("/analysis/calculate")
    public ApiResponse<Map<String, Object>> calcAnalysis(@RequestBody Map<String, Object> params) {
        Map<String, Object> converted = new HashMap<>();
        converted.put("coal_type", params.getOrDefault("coalType", params.getOrDefault("coal_type", "未知")));
        converted.put("volatile", toDouble(params.get("volatile"), 0));
        converted.put("temperature", toDouble(params.get("temperature"), 30));
        converted.put("water_content", toDouble(params.getOrDefault("waterContent", params.getOrDefault("water_content", 0)), 0));
        converted.put("vl", toDouble(params.getOrDefault("vl", params.get("Vl")), 25));
        converted.put("pl", toDouble(params.getOrDefault("pl", params.get("Pl")), 2.0));
        converted.put("reference_temp", toDouble(params.getOrDefault("referenceTemp", params.getOrDefault("reference_temp", 25)), 25));
        converted.put("p_min", toDouble(params.getOrDefault("pMin", params.getOrDefault("p_min", 1)), 1));
        converted.put("p_max", toDouble(params.getOrDefault("pMax", params.getOrDefault("p_max", 16)), 16));
        converted.put("p_step", toDouble(params.getOrDefault("pStep", params.getOrDefault("p_step", 0.1)), 0.1));
        converted.put("chart_style", params.getOrDefault("chartStyle", params.getOrDefault("chart_style", "curve")));
        converted.put("comparison_curves", params.getOrDefault("comparisonCurves", params.getOrDefault("comparison_curves", params.getOrDefault("comparisonCurves", new java.util.ArrayList<>()))));
        JSONObject result = pythonClient.callAnalysis(converted);
        return ApiResponse.ok(result.toBean(Map.class));
    }

    @PostMapping("/statistics/analyze")
    public ApiResponse<Map<String, Object>> calcStatistics(@RequestBody Map<String, Object> params) {
        Map<String, Object> converted = new HashMap<>();
        converted.put("chart_type", params.getOrDefault("chartType", params.getOrDefault("chart_type", "scatter")));
        converted.put("x_axis", params.getOrDefault("xAxis", params.getOrDefault("x_axis", "挥发分")));
        converted.put("y_axis", params.getOrDefault("yAxis", params.getOrDefault("y_axis", "VL值")));
        converted.put("color_by", params.getOrDefault("colorBy", params.getOrDefault("color_by", "检索地区")));
        converted.put("size_by", params.getOrDefault("sizeBy", params.getOrDefault("size_by", "挥发分")));
        converted.put("region_filter", params.getOrDefault("regionFilter", params.getOrDefault("region_filter", "全部")));
        converted.put("volatile_filter", params.getOrDefault("volatileFilter", params.getOrDefault("volatile_filter", "全部")));
        converted.put("file_data", params.getOrDefault("fileData", params.getOrDefault("file_data", null)));
        JSONObject result = pythonClient.callStatistics(converted);
        return ApiResponse.ok(result.toBean(Map.class));
    }

    @PostMapping("/detection/evaluate")
    public ApiResponse<Map<String, Object>> calcDetection(@RequestBody Map<String, Object> params) {
        Map<String, Object> converted = new HashMap<>();
        // 解析 adsorption_data
        Object adsData = params.getOrDefault("adsorptionData", params.getOrDefault("adsorption_data", null));
        if (adsData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> adsMap = (Map<String, Object>) adsData;
            Map<String, Object> adsSnake = new HashMap<>();
            adsSnake.put("p_array", adsMap.getOrDefault("pArray", adsMap.getOrDefault("p_array", null)));
            adsSnake.put("xx_array", adsMap.getOrDefault("xxArray", adsMap.getOrDefault("xx_array", null)));
            converted.put("adsorption_data", adsSnake);
        }
        converted.put("volume", toDouble(params.get("volume"), 0.05));
        converted.put("temperature", toDouble(params.get("temperature"), 25));
        converted.put("compress_factor", toDouble(params.getOrDefault("compressFactor", params.getOrDefault("compress_factor", 1.0)), 1.0));
        converted.put("crit_pressure", toDouble(params.getOrDefault("critPressure", params.getOrDefault("crit_pressure", 0.74)), 0.74));
        converted.put("crit_content", toDouble(params.getOrDefault("critContent", params.getOrDefault("crit_content", 8.0)), 8.0));
        JSONObject result = pythonClient.callDetection(converted);
        return ApiResponse.ok(result.toBean(Map.class));
    }

    private double toDouble(Object val, double defaultVal) {
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        if (val instanceof String) {
            try { return Double.parseDouble((String) val); } catch (NumberFormatException e) { return defaultVal; }
        }
        return defaultVal;
    }
}
