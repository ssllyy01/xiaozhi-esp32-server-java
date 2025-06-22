package com.xiaozhi.service;

import com.xiaozhi.common.web.PageFilter;
import com.xiaozhi.entity.SysRole;

import java.util.List;

/**
 * 角色查询/更新
 * 
 * @author Joey
 * 
 */
public interface SysRoleService {

  /**
   * 添加角色
   * 
   * @param role
   * @return
   */
  int add(SysRole role);

  /**
   * 查询角色信息
   * 指定分页信息
   * @param role
   * @param pageFilter
   * @return
   */
  List<SysRole> query(SysRole role, PageFilter pageFilter);

  /**
   * 更新角色信息
   * 
   * @param role
   * @return
   */
  int update(SysRole role);

  SysRole selectRoleById(Integer roleId);

}