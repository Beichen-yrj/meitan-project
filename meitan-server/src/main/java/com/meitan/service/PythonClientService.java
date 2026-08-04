package com.meitan.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.meitan.exception.PythonServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/** Python 科学计算服务客户端。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PythonClientService {

    @Value("${python.service.url}")
    private String pythonServiceUrl;

    public JSONObject callAnalysis(Map<String, Object> params) {
        return post("/api/v1/analysis/calculate", params);
    }

    public JSONObject callStatistics(Map<String, Object> params) {
        return post("/api/v1/statistics/analyze", params);
    }

    public JSONObject callDetection(Map<String, Object> params) {
        return post("/api/v1/detection/evaluate", params);
    }

    private JSONObject post(String path, Map<String, Object> params) {
        String url = pythonServiceUrl + path;
        String body = JSONUtil.toJsonStr(params);

        try (HttpResponse resp = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .timeout(30000)
                .body(body)
                .execute()) {
            if (resp.isOk()) {
                return JSONUtil.parseObj(resp.body());
            }
            log.error("Python服务调用失败: {} -> {}", url, resp.body());
            throw new PythonServiceUnavailableException("Python计算服务返回异常: " + resp.body());
        } catch (PythonServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用Python服务异常: {}", e.getMessage());
            throw new PythonServiceUnavailableException(
                "Python计算服务未启动或暂时不可用，请检查 5000 端口服务", e);
        }
    }
}
