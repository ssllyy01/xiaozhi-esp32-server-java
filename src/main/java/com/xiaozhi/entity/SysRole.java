package com.xiaozhi.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 角色配置
 * 
 * @author Joey
 * 
 */
@JsonIgnoreProperties({ "startTime", "endTime", "start", "limit", "userId", "code" })
public class SysRole extends Base {
    private Integer roleId;

    private String roleName;

    private String roleDesc;

    private String voiceName;

    private String state;

    private Integer ttsId;

    private Integer modelId;

    private String modelName;

    private Integer sttId;

    private Double temperature;

    private Double topP;

    private Float vadEnergyTh;
    private Float vadSpeechTh;
    private Float vadSilenceTh;
    private Integer vadSilenceMs;

    private String modelProvider;

    private String ttsProvider;

    private String isDefault;

    private Integer totalDevice;

    public Integer getTtsId() {
        return ttsId;
    }

    public SysRole setTtsId(Integer ttsId) {
        this.ttsId = ttsId;
        return this;
    }

    public Integer getModelId() {
        return modelId;
    }

    public SysRole setModelId(Integer modelId) {
        this.modelId = modelId;
        return this;
    }

    public String getModelName() {
        return modelName;
    }

    public SysRole setModelName(String modelName) {
        this.modelName = modelName;
        return this;
    }

    public Integer getSttId() {
        return sttId;
    }

    public SysRole setSttId(Integer sttId) {
        this.sttId = sttId;
        return this;
    }

    public Double getTemperature() {
        return temperature;
    }

    public SysRole setTemperature(Double temperature) {
        this.temperature = temperature;
        return this;
    }

    public Double getTopP() {
        return topP;
    }

    public SysRole setTopP(Double topP) {
        this.topP = topP;
        return this;
    }

    public Float getVadEnergyTh() {
        return vadEnergyTh;
    }

    public SysRole setVadEnergyTh(Float vadEnergyTh) {
        this.vadEnergyTh = vadEnergyTh;
        return this;
    }

    public Float getVadSpeechTh() {
        return vadSpeechTh;
    }

    public SysRole setVadSpeechTh(Float vadSpeechTh) {
        this.vadSpeechTh = vadSpeechTh;
        return this;
    }

    public Float getVadSilenceTh() {
        return vadSilenceTh;
    }

    public SysRole setVadSilenceTh(Float vadSilenceTh) {
        this.vadSilenceTh = vadSilenceTh;
        return this;
    }

    public Integer getVadSilenceMs() {
        return vadSilenceMs;
    }

    public SysRole setVadSilenceMs(Integer vadSilenceMs) {
        this.vadSilenceMs = vadSilenceMs;
        return this;
    }

    public String getTtsProvider() {
        return ttsProvider;
    }
    
    public SysRole setTtsProvider(String ttsProvider) {
        this.ttsProvider = ttsProvider;
        return this;
    }

    public String getModelProvider() {
        return modelProvider;
    }
    
    public SysRole setModelProvider(String modelProvider) {
        this.modelProvider = modelProvider;
        return this;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public SysRole setRoleId(Integer roleId) {
        this.roleId = roleId;
        return this;
    }

    public String getRoleName() {
        return roleName;
    }

    public SysRole setRoleName(String roleName) {
        this.roleName = roleName;
        return this;
    }

    public String getRoleDesc() {
        return roleDesc;
    }

    public SysRole setRoleDesc(String roleDesc) {
        this.roleDesc = roleDesc;
        return this;
    }

    public String getVoiceName() {
        return voiceName;
    }

    public SysRole setVoiceName(String voiceName) {
        this.voiceName = voiceName;
        return this;
    }

    public String getState() {
        return state;
    }

    public SysRole setState(String state) {
        this.state = state;
        return this;
    }

    public String getIsDefault() {
        return isDefault;
    }

    public SysRole setIsDefault(String isDefault) {
        this.isDefault = isDefault;
        return this;
    }

    public Integer getTotalDevice() {
        return totalDevice;
    }

    public SysRole setTotalDevice(Integer totalDevice) {
        this.totalDevice = totalDevice;
        return this;
    }
}
