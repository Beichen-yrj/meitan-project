package com.meitan.service;

import com.meitan.dto.AiChatRequest;
import com.meitan.dto.AiChatResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeepSeekAssistantService {

    private static final String SYSTEM_PROMPT = """
        你是煤层瓦斯智能分析平台的AI助手“小瓦”。请始终使用简洁、专业、友好的中文回答。

        你熟悉平台全部页面：
        1. /home 首页：功能入口、轮播图和行业新闻。
        2. /introduction 瓦斯介绍：煤层瓦斯基础知识和相关图示。
        3. /analysis 瓦斯吸附量计算与分析：导入煤型及编号、挥发分、Vl、Pl等参数，计算并绘制Langmuir吸附曲线。
        4. /statistics 煤层瓦斯吸附参数统计：按地区、挥发分和参数列筛选，生成散点图、双坐标轴图或分组图。
        5. /detection 煤层区域突出危险性预测：导入压力P和吸附瓦斯Xx，计算游离瓦斯Xy、总含量Q并按P-W临界值进行区域预测。
        6. /files 数据文件管理：查看带计算时间的历史记录、详情、导出、删除或清空。
        7. /reports 瓦斯数据导出与报告：查看最近一次分析、综合安全评价，并导出XLSX或HTML报告。
        8. /feedback 用户反馈：提交使用体验和建议。
        9. /user-center 个人中心：查看账户资料和修改密码。

        回答操作问题时给出清晰步骤。涉及安全判定时提醒用户以现场规范和专业人员意见为准，不得编造实测数据。
        当答案建议用户进入某个页面时，在回答末尾另起一行输出导航标记，格式必须为 [NAVIGATE:/页面路径]；一次最多输出一个最相关的导航标记。
        不要向用户索取或复述API Key，也不要声称已经替用户完成尚未执行的操作。
        """;

    private final RestClient restClient;

    public DeepSeekAssistantService() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(8));
        requestFactory.setReadTimeout(Duration.ofSeconds(60));
        this.restClient = RestClient.builder()
            .baseUrl("https://api.deepseek.com")
            .requestFactory(requestFactory)
            .build();
    }

    public AiChatResponse chat(String apiKey, AiChatRequest request) {
        String normalizedKey = apiKey == null ? "" : apiKey.trim();
        if (!StringUtils.hasText(normalizedKey) || normalizedKey.length() < 12 || normalizedKey.length() > 256) {
            throw new DeepSeekApiException(HttpStatus.BAD_REQUEST, "请填写有效的 DeepSeek API Key");
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", buildSystemPrompt(request)));

        List<AiChatRequest.Message> history = request.getMessages();
        int start = Math.max(0, history.size() - 14);
        for (int i = start; i < history.size(); i++) {
            AiChatRequest.Message message = history.get(i);
            String role = "assistant".equals(message.getRole()) ? "assistant" : "user";
            messages.add(Map.of("role", role, "content", message.getContent().trim()));
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.35);
        requestBody.put("max_tokens", 900);
        requestBody.put("stream", false);

        try {
            Map<String, Object> response = restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + normalizedKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

            String content = extractContent(response);
            String model = response == null ? "deepseek-chat" : String.valueOf(response.getOrDefault("model", "deepseek-chat"));
            return new AiChatResponse(content, model);
        } catch (RestClientResponseException exception) {
            throw mapApiError(exception.getStatusCode().value());
        } catch (ResourceAccessException exception) {
            throw new DeepSeekApiException(HttpStatus.GATEWAY_TIMEOUT, "连接 DeepSeek 超时，请检查网络后重试");
        } catch (DeepSeekApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DeepSeekApiException(HttpStatus.BAD_GATEWAY, "DeepSeek 服务暂时不可用，请稍后重试");
        }
    }

    private String buildSystemPrompt(AiChatRequest request) {
        String page = StringUtils.hasText(request.getCurrentPage()) ? request.getCurrentPage() : "未知页面";
        String path = StringUtils.hasText(request.getCurrentPath()) ? request.getCurrentPath() : "未知路径";
        return SYSTEM_PROMPT + "\n用户当前位于：" + page + "（" + path + "）。";
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        if (response == null) {
            throw new DeepSeekApiException(HttpStatus.BAD_GATEWAY, "DeepSeek 未返回有效内容");
        }
        Object choicesValue = response.get("choices");
        if (!(choicesValue instanceof List<?> choices) || choices.isEmpty()) {
            throw new DeepSeekApiException(HttpStatus.BAD_GATEWAY, "DeepSeek 未返回回答内容");
        }
        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choice)) {
            throw new DeepSeekApiException(HttpStatus.BAD_GATEWAY, "DeepSeek 返回格式异常");
        }
        Object messageValue = choice.get("message");
        if (!(messageValue instanceof Map<?, ?> message)) {
            throw new DeepSeekApiException(HttpStatus.BAD_GATEWAY, "DeepSeek 返回格式异常");
        }
        Object contentValue = message.get("content");
        String content = contentValue == null ? "" : String.valueOf(contentValue).trim();
        if (!StringUtils.hasText(content)) {
            throw new DeepSeekApiException(HttpStatus.BAD_GATEWAY, "DeepSeek 返回了空回答");
        }
        return content;
    }

    private DeepSeekApiException mapApiError(int statusCode) {
        return switch (statusCode) {
            case 401, 403 -> new DeepSeekApiException(HttpStatus.UNAUTHORIZED, "DeepSeek API Key 无效或没有权限，请重新检查");
            case 402 -> new DeepSeekApiException(HttpStatus.PAYMENT_REQUIRED, "DeepSeek 账户余额不足，请前往开放平台充值");
            case 429 -> new DeepSeekApiException(HttpStatus.TOO_MANY_REQUESTS, "DeepSeek 请求过于频繁，请稍后再试");
            default -> new DeepSeekApiException(HttpStatus.BAD_GATEWAY, "DeepSeek 接口调用失败（" + statusCode + "）");
        };
    }

    public static class DeepSeekApiException extends RuntimeException {
        private final HttpStatus status;

        public DeepSeekApiException(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }
}
