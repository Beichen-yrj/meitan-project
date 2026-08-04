package com.meitan.controller;

import com.meitan.dto.ApiResponse;
import com.meitan.entity.News;
import com.meitan.mapper.NewsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final NewsMapper newsMapper;

    @GetMapping("/news")
    public ApiResponse<List<News>> getNews() {
        List<News> list = newsMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<News>()
                        .eq(News::getStatus, 1)
                        .orderByAsc(News::getSortOrder)
        );
        return ApiResponse.ok(list);
    }
}
