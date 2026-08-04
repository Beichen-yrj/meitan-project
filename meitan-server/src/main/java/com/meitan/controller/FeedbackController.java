package com.meitan.controller;

import com.meitan.dto.ApiResponse;
import com.meitan.dto.FeedbackHandleRequest;
import com.meitan.dto.FeedbackResponse;
import com.meitan.dto.FeedbackStatistics;
import com.meitan.dto.PageResponse;
import com.meitan.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping
    public ApiResponse<PageResponse<FeedbackResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        try {
            return ApiResponse.ok(feedbackService.listAll(keyword, status, rating, page, pageSize));
        } catch (RuntimeException exception) {
            return ApiResponse.error(400, exception.getMessage());
        }
    }

    @GetMapping("/statistics")
    public ApiResponse<FeedbackStatistics> statistics() {
        return ApiResponse.ok(feedbackService.statistics());
    }

    @PutMapping("/{feedbackId}/handle")
    public ApiResponse<FeedbackResponse> handle(@PathVariable Long feedbackId,
                                                @RequestAttribute Long userId,
                                                @RequestBody FeedbackHandleRequest request) {
        try {
            return ApiResponse.ok(feedbackService.handle(
                    feedbackId,
                    userId,
                    request.getStatus(),
                    request.getAdminReply()
            ));
        } catch (RuntimeException exception) {
            return ApiResponse.error(400, exception.getMessage());
        }
    }
}
