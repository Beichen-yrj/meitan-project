package com.meitan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AiChatRequest {

    @NotEmpty(message = "对话消息不能为空")
    @Size(max = 20, message = "单次最多携带20条对话消息")
    @Valid
    private List<Message> messages;

    @Size(max = 120)
    private String currentPath;

    @Size(max = 120)
    private String currentPage;

    @Data
    public static class Message {
        @NotBlank(message = "消息角色不能为空")
        @Size(max = 20)
        private String role;

        @NotBlank(message = "消息内容不能为空")
        @Size(max = 4000, message = "单条消息不能超过4000字")
        private String content;
    }
}
