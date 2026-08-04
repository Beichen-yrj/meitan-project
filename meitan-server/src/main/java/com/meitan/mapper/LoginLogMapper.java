package com.meitan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meitan.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {

    @Select("SELECT COUNT(*) FROM sys_login_log " +
            "WHERE success = 1 AND login_time >= CURRENT_DATE() AND login_time < DATE_ADD(CURRENT_DATE(), INTERVAL 1 DAY)")
    long countTodaySuccess();

    @Select("SELECT COUNT(*) FROM sys_login_log " +
            "WHERE success = 0 AND login_time >= CURRENT_DATE() AND login_time < DATE_ADD(CURRENT_DATE(), INTERVAL 1 DAY)")
    long countTodayFailure();
}
