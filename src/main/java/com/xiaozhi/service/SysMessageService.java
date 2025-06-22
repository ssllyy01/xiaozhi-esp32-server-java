package com.xiaozhi.service;

import com.xiaozhi.common.web.PageFilter;
import com.xiaozhi.entity.SysMessage;

import java.util.List;

/**
 * 聊天记录查询/添加
 * 
 * @author Joey
 * 
 */
public interface SysMessageService {

  /**
   * 新增记录
   * 
   * @param message
   * @return
   */
  int add(SysMessage message);

  /**
   * 查询聊天记录
   * 指定分页信息
   * @param message
   * @return
   */
  List<SysMessage> query(SysMessage message, PageFilter pageFilter);

  /**
   * 删除记忆
   * 
   * @param message
   * @return
   */
  int delete(SysMessage message);

}