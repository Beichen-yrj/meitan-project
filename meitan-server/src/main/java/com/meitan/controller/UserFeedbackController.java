package com.meitan.controller;

import com.meitan.dto.ApiResponse;
import com.meitan.dto.FeedbackResponse;
import com.meitan.entity.Feedback;
import com.meitan.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class UserFeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ApiResponse<FeedbackResponse> submit(@RequestAttribute Long userId,
                                                @RequestBody Feedback request) {
        try {
            return ApiResponse.ok(feedbackService.submit(userId, request.getRating(), request.getContent()));
        } catch (RuntimeException exception) {
            return ApiResponse.error(400, exception.getMessage());
        }
    }

    @GetMapping("/mine")
    public ApiResponse<List<FeedbackResponse>> mine(@RequestAttribute Long userId) {
        return ApiResponse.ok(feedbackService.listMine(userId));
    }
}
