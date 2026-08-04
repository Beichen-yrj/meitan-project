package com.meitan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meitan.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    User findByUsername(@Param("username") String username);

    @Update("UPDATE sys_user SET last_login_time = #{loginTime}, last_login_ip = #{loginIp}, " +
            "login_count = COALESCE(login_count, 0) + 1 WHERE id = #{userId}")
    int recordSuccessfulLogin(@Param("userId") Long userId,
                              @Param("loginTime") LocalDateTime loginTime,
                              @Param("loginIp") String loginIp);
}
