package com.xiaozhi.service;

import com.xiaozhi.common.web.PageFilter;
import com.xiaozhi.entity.SysDevice;
import org.apache.ibatis.javassist.NotFoundException;

import java.util.List;

/**
 * 设备查询/更新
 * 
 * @author Joey
 * 
 */
public interface SysDeviceService {

  /**
   * 添加设备
   * 
   * @param device
   * @return
   */
  int add(SysDevice device) throws NotFoundException;

  /**
   * 查询设备信息
   * 
   * @param device
   * @return
   */
  List<SysDevice> query(SysDevice device, PageFilter pageFilter);

  /**
   * 查询设备信息，并join配置表联查，用来过滤不存在的configId
   * @param deviceId 设备id
   * @return
   */
  SysDevice selectDeviceById(String deviceId);

  /**
   * 查询验证码
   */
  SysDevice queryVerifyCode(SysDevice device);

  /**
   * 查询并生成验证码
   */
  SysDevice generateCode(SysDevice device);

  /**
   * 关系设备验证码语音路径
   */
  int updateCode(SysDevice device);

  /**
   * 更新设备信息
   * 
   * @param device
   * @return
   */
  int update(SysDevice device);

  /**
   * 删除设备
   * 
   * @param device
   * @return
   */
  int delete(SysDevice device);

  /**
   * 生成设备访问平台的token
   * @param deviceId
   * @return
   */
  String generateToken(String deviceId);
}