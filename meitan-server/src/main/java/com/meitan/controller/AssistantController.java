package com.meitan.controller;

import com.meitan.dto.AiChatRequest;
import com.meitan.dto.AiChatResponse;
import com.meitan.dto.ApiResponse;
import com.meitan.service.DeepSeekAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final DeepSeekAssistantService assistantService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @RequestHeader("X-DeepSeek-Api-Key") String apiKey,
            @Valid @RequestBody AiChatRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(assistantService.chat(apiKey, request)));
        } catch (DeepSeekAssistantService.DeepSeekApiException exception) {
            return ResponseEntity.status(exception.getStatus())
                .body(ApiResponse.error(exception.getStatus().value(), exception.getMessage()));
        }
    }
}
