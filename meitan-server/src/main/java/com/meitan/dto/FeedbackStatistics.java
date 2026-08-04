package com.meitan.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeedbackStatistics {
    private Long total;
    private Long pending;
    private Long processing;
    private Long resolved;
    private Double averageRating;
}
