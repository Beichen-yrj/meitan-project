package com.meitan.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FeedbackResponse {
    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private Integer rating;
    private String content;
    private String status;
    private String adminReply;
    private Long handledBy;
    private String handledByUsername;
    private LocalDateTime handledTime;
    private LocalDateTime createTime;
}
