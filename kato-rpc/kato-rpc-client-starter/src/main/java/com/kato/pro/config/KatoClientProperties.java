package com.kato.pro.config;


import com.kato.pro.constant.ServiceType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "kato.rpc.client")
public class KatoClientProperties {

    /**
     * 服务地址
     */
    private String discoverAddress;

    /**
     * 序列化方式
     */
    private String serialize;

    /**
     * 服务调用的时间上限
     */
    private Integer timeout;

    /**
     * 选择器
     */
    private String balance;

    /**
     * 默认使用zookeeper作为注册中心
     */
    private ServiceType serviceType = ServiceType.ZOOKEEPER;
}
