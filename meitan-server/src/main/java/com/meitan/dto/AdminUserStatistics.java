package com.meitan.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUserStatistics {
    private Long totalUsers;
    private Long enabledUsers;
    private Long disabledUsers;
    private Long blacklistedUsers;
    private Long todayLoginCount;
    private Long todayFailedCount;
}
