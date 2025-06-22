package com.xiaozhi.dao;

import java.util.List;

import com.xiaozhi.entity.SysUser;
import org.apache.ibatis.annotations.Param;

/**
 * 用户资料 数据层
 * 
 * @author Joey
 * 
 */
public interface UserMapper {
    SysUser selectUserByUserId(@Param("userId") Integer userId);

    SysUser selectUserByUsername(@Param("username") String username);

    SysUser selectUserByEmail(@Param("email") String email);

    SysUser query(@Param("username") String username, @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    int add(SysUser user);

    int update(SysUser user);

    List<SysUser> queryUsers(SysUser user);

    int generateCode(SysUser user);

    int queryCaptcha(@Param("code") String code, @Param("email") String email);

}
