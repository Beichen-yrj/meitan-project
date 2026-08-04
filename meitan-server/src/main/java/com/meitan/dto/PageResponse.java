package com.meitan.dto;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PageResponse<T> {
    private List<T> records;
    private Long total;
    private Long current;
    private Long size;

    public static <T> PageResponse<T> of(IPage<?> page, List<T> records) {
        return PageResponse.<T>builder()
                .records(records)
                .total(page.getTotal())
                .current(page.getCurrent())
                .size(page.getSize())
                .build();
    }
}
