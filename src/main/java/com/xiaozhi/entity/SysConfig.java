package com.xiaozhi.entity;

/**
 * LLM\STT\TTS配置
 * 
 * @author Joey
 * 
 */
public class SysConfig extends Base {
    private Integer configId;

    private Integer userId;

    private String deviceId;

    private Integer roleId;

    /**
     * 配置名称
     */
    private String configName;

    /**
     * 配置描述
     */
    private String configDesc;

    /**
     * 配置类型（model\stt\tts）
     */
    private String configType;

    /**
     * 服务提供商 (openai\quen\vosk\aliyun\tencent等)
     */
    private String provider;

    private String appId;

    private String apiKey;

    private String apiSecret;

    private String ak;

    private String sk;

    private String apiUrl;

    private String state;

    private String isDefault;

    public Integer getUserId() {
        return userId;
    }

    public SysConfig setUserId(Integer userId) {
        this.userId = userId;
        return this;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public SysConfig setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public SysConfig setRoleId(Integer roleId) {
        this.roleId = roleId;
        return this;
    }

    public Integer getConfigId() {
        return configId;
    }

    public SysConfig setConfigId(Integer configId) {
        this.configId = configId;
        return this;
    }

    public String getConfigName() {
        return configName;
    }

    public SysConfig setConfigName(String configName) {
        this.configName = configName;
        return this;
    }

    public String getConfigDesc() {
        return configDesc;
    }

    public SysConfig setConfigDesc(String configDesc) {
        this.configDesc = configDesc;
        return this;
    }

    public String getConfigType() {
        return configType;
    }

    public SysConfig setConfigType(String configType) {
        this.configType = configType;
        return this;
    }

    public String getProvider() {
        return provider;
    }

    public SysConfig setProvider(String provider) {
        this.provider = provider;
        return this;
    }

    public String getAppId() {
        return appId;
    }

    public SysConfig setAppId(String appId) {
        this.appId = appId;
        return this;
    }

    public String getApiKey() {
        return apiKey;
    }

    public SysConfig setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public SysConfig setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
        return this;
    }

    public String getAk() {
        return ak;
    }

    public SysConfig setAk(String ak) {
        this.ak = ak;
        return this;
    }

    public String getSk() {
        return sk;
    }

    public SysConfig setSk(String sk) {
        this.sk = sk;
        return this;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public SysConfig setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
        return this;
    }

    public String getState() {
        return state;
    }

    public SysConfig setState(String state) {
        this.state = state;
        return this;
    }

    public String getIsDefault() {
        return isDefault;
    }

    public SysConfig setIsDefault(String isDefault) {
        this.isDefault = isDefault;
        return this;
    }

    @Override
    public String toString() {
        return "SysConfig [configId=" + configId + ", configName=" + configName + ", configDesc=" + configDesc
                + ", configType=" + configType + ", provider=" + provider + ", appId=" + appId + ", apiKey=" + apiKey
                + ", apiSecret=" + apiSecret + ", ak=" + ak + ", sk=" + sk + ", apiUrl=" + apiUrl + ", state=" + state
                + ", isDefault=" + isDefault + "]";
    }
}
