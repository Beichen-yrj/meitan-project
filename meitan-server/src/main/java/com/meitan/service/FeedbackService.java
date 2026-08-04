package com.meitan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meitan.dto.FeedbackResponse;
import com.meitan.dto.FeedbackStatistics;
import com.meitan.dto.PageResponse;
import com.meitan.entity.Feedback;
import com.meitan.entity.User;
import com.meitan.mapper.FeedbackMapper;
import com.meitan.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("PENDING", "PROCESSING", "RESOLVED");
    private final FeedbackMapper feedbackMapper;
    private final UserMapper userMapper;

    public FeedbackResponse submit(Long userId, Integer rating, String content) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new RuntimeException("评分必须在1到5星之间");
        }
        String safeContent = content == null ? "" : content.trim();
        if (!StringUtils.hasText(safeContent)) {
            throw new RuntimeException("请填写反馈内容");
        }
        if (safeContent.length() > 2000) {
            throw new RuntimeException("反馈内容不能超过2000字");
        }

        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setRating(rating);
        feedback.setContent(safeContent);
        feedback.setStatus("PENDING");
        feedback.setCreateTime(LocalDateTime.now());
        feedbackMapper.insert(feedback);
        return toResponse(feedback, loadUsers(Set.of(userId)));
    }

    public List<FeedbackResponse> listMine(Long userId) {
        List<Feedback> feedbacks = feedbackMapper.selectList(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getUserId, userId)
                .orderByDesc(Feedback::getCreateTime)
                .last("LIMIT 30"));
        Map<Long, User> users = loadUsers(relatedUserIds(feedbacks));
        return feedbacks.stream().map(item -> toResponse(item, users)).toList();
    }

    public PageResponse<FeedbackResponse> listAll(String keyword,
                                                  String status,
                                                  Integer rating,
                                                  long page,
                                                  long pageSize) {
        LambdaQueryWrapper<Feedback> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            query.eq(Feedback::getStatus, normalizeStatus(status));
        }
        if (rating != null) {
            query.eq(Feedback::getRating, rating);
        }
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            List<Long> matchedUserIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .select(User::getId)
                    .and(wrapper -> wrapper.like(User::getUsername, value)
                            .or().like(User::getRealName, value)))
                    .stream().map(User::getId).toList();
            query.and(wrapper -> {
                wrapper.like(Feedback::getContent, value);
                if (!matchedUserIds.isEmpty()) {
                    wrapper.or().in(Feedback::getUserId, matchedUserIds);
                }
            });
        }
        query.orderByAsc(Feedback::getStatus).orderByDesc(Feedback::getCreateTime);

        Page<Feedback> result = feedbackMapper.selectPage(
                new Page<>(Math.max(1, page), Math.min(100, Math.max(1, pageSize))),
                query
        );
        Map<Long, User> users = loadUsers(relatedUserIds(result.getRecords()));
        List<FeedbackResponse> records = result.getRecords().stream()
                .map(item -> toResponse(item, users))
                .toList();
        return PageResponse.of(result, records);
    }

    public FeedbackStatistics statistics() {
        long total = feedbackMapper.selectCount(null);
        List<Feedback> feedbacks = feedbackMapper.selectList(new LambdaQueryWrapper<Feedback>()
                .select(Feedback::getRating));
        double average = feedbacks.stream()
                .filter(item -> item.getRating() != null)
                .mapToInt(Feedback::getRating)
                .average()
                .orElse(0D);
        return FeedbackStatistics.builder()
                .total(total)
                .pending(countByStatus("PENDING"))
                .processing(countByStatus("PROCESSING"))
                .resolved(countByStatus("RESOLVED"))
                .averageRating(Math.round(average * 10D) / 10D)
                .build();
    }

    public FeedbackResponse handle(Long feedbackId, Long adminId, String status, String adminReply) {
        Feedback feedback = feedbackMapper.selectById(feedbackId);
        if (feedback == null) {
            throw new RuntimeException("反馈不存在");
        }
        String normalizedStatus = normalizeStatus(status);
        String reply = adminReply == null ? "" : adminReply.trim();
        if (reply.length() > 2000) {
            throw new RuntimeException("管理员回复不能超过2000字");
        }
        feedback.setStatus(normalizedStatus);
        feedback.setAdminReply(reply);
        feedback.setHandledBy(adminId);
        feedback.setHandledTime(LocalDateTime.now());
        feedbackMapper.updateById(feedback);

        Set<Long> userIds = java.util.stream.Stream.of(feedback.getUserId(), adminId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        return toResponse(feedback, loadUsers(userIds));
    }

    private long countByStatus(String status) {
        return feedbackMapper.selectCount(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getStatus, status));
    }

    private String normalizeStatus(String status) {
        String value = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(value)) {
            throw new RuntimeException("反馈处理状态不正确");
        }
        return value;
    }

    private Set<Long> relatedUserIds(List<Feedback> feedbacks) {
        return feedbacks.stream()
                .flatMap(item -> java.util.stream.Stream.of(item.getUserId(), item.getHandledBy()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Map<Long, User> loadUsers(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private FeedbackResponse toResponse(Feedback feedback, Map<Long, User> users) {
        User submitter = users.get(feedback.getUserId());
        User handler = users.get(feedback.getHandledBy());
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .userId(feedback.getUserId())
                .username(submitter == null ? "未知用户" : submitter.getUsername())
                .realName(submitter == null ? null : submitter.getRealName())
                .rating(feedback.getRating())
                .content(feedback.getContent())
                .status(feedback.getStatus())
                .adminReply(feedback.getAdminReply())
                .handledBy(feedback.getHandledBy())
                .handledByUsername(handler == null ? null : handler.getUsername())
                .handledTime(feedback.getHandledTime())
                .createTime(feedback.getCreateTime())
                .build();
    }
}
