package com.meitan.controller;

import com.meitan.dto.ApiResponse;
import com.meitan.entity.News;
import com.meitan.mapper.NewsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsMapper newsMapper;

    @GetMapping
    public ApiResponse<List<News>> list() {
        List<News> list = newsMapper.selectList(
                new LambdaQueryWrapper<News>().orderByAsc(News::getSortOrder));
        return ApiResponse.ok(list);
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody News news) {
        newsMapper.insert(news);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody News news) {
        news.setId(id);
        newsMapper.updateById(news);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        newsMapper.deleteById(id);
        return ApiResponse.ok();
    }
}
