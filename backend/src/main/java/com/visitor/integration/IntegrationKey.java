package com.visitor.integration;

/**
 * 六类学校接口的配置键，与 vst_integration_config.integration_key 一一对应。
 */
public enum IntegrationKey {

    SSO("统一身份认证"),
    DATA_SYNC("数据中台"),
    GATE("门闸/停车场"),
    FACE("人脸平台"),
    PORTAL("融合门户/i阳光"),
    TODO_MSG("统一待办/消息"),
    ACCESS_SYNC("名单双向同步");

    private final String description;

    IntegrationKey(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static IntegrationKey of(String key) {
        for (IntegrationKey value : values()) {
            if (value.name().equalsIgnoreCase(key)) {
                return value;
            }
        }
        return null;
    }
}
