package com.kato.pro.config;


import com.kato.pro.constant.ServiceType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "kato.rpc.server")
public class KatoRpcProperties {

    /** 注册中心类型 */
    private ServiceType serviceType = ServiceType.ZOOKEEPER;

    /** 注册中心地址 如 127.0.0.1:2181 */
    private String serverAddress;

    /** 服务名称，ServiceInfo的属性 */
    private String appName;

    /** 端口 */
    private Integer port;
}
