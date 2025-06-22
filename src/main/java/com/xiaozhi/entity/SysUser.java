package com.xiaozhi.entity;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 用户表
 * 
 * @author Joey
 * 
 */
@JsonIgnoreProperties({ "password", "startTime", "endTime", "start" })
public class SysUser extends Base {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -3406166342385856305L;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 姓名
     */
    private String name;

    /**
     * 对话次数
     */
    private Integer totalMessage;

    /**
     * 参加人数
     */
    private Integer aliveNumber;

    /**
     * 总设备数
     */
    private Integer totalDevice;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 用户状态 0、被禁用，1、正常使用
     */
    private String state;

    /**
     * 用户类型 0、普通管理（拥有标准权限），1、超级管理（拥有所有权限）
     */
    private String isAdmin;

    /**
     * 手机号
     */
    private String tel;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 上次登录IP
     */
    private String loginIp;

    /**
     * 验证码
     */
    private String code;

    /**
     * 上次登录时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date loginTime;

    public String getUsername() {
        return username;
    }

    public SysUser setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public SysUser setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getName() {
        return name;
    }

    public SysUser setName(String name) {
        this.name = name;
        return this;
    }

    public String getAvatar() {
        return avatar;
    }

    public SysUser setAvatar(String avatar) {
        this.avatar = avatar;
        return this;
    }

    public String getState() {
        return state;
    }

    public SysUser setState(String state) {
        this.state = state;
        return this;
    }

    public String getIsAdmin() {
        return isAdmin;
    }

    public SysUser setIsAdmin(String isAdmin) {
        this.isAdmin = isAdmin;
        return this;
    }

    public Integer getTotalMessage() {
        return totalMessage;
    }

    public SysUser setTotalMessage(Integer totalMessage) {
        this.totalMessage = totalMessage;
        return this;
    }

    public Integer getAliveNumber() {
        return aliveNumber;
    }

    public SysUser setAliveNumber(Integer aliveNumber) {
        this.aliveNumber = aliveNumber;
        return this;
    }

    public Integer getTotalDevice() {
        return totalDevice;
    }

    public SysUser setTotalDevice(Integer totalDevice) {
        this.totalDevice = totalDevice;
        return this;
    }

    public String getTel() {
        return tel;
    }

    public SysUser setTel(String tel) {
        this.tel = tel;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public SysUser setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getLoginIp() {
        return loginIp;
    }

    public SysUser setLoginIp(String loginIp) {
        this.loginIp = loginIp;
        return this;
    }

    public Date getLoginTime() {
        return loginTime;
    }

    public SysUser setLoginTime(Date loginTime) {
        this.loginTime = loginTime;
        return this;
    }

    public String getCode() {
        return code;
    }

    public SysUser setCode(String code) {
        this.code = code;
        return this;
    }

    @Override
    public String toString() {
        return "SysUser [username=" + username + ", password=" + password + ", name=" + name + ", totalMessage="
                + totalMessage + ", aliveNumber=" + aliveNumber + ", totalDevice=" + totalDevice + ", avatar=" + avatar
                + ", state=" + state + ", isAdmin=" + isAdmin + ", tel=" + tel + ", email=" + email + ", loginIp="
                + loginIp + ", code=" + code + ", loginTime=" + loginTime + "]";
    }

}
