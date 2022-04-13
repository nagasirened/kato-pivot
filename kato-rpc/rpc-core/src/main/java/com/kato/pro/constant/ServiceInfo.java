package com.kato.pro.constant;

import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName ServiceInfo
 * @Author Zeng Guangfu
 * @Description 节点信息
 * @Date 2022/3/31 7:41 下午
 * @Version 1.0
 */

@Data
public class ServiceInfo implements Serializable {

    /**
     * 应用名称，集群中应用名称统一
     */
    private String applicationName;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 版本号
     */
    private String version;

    /**
     * ip地址
     */
    private String address;

    /**
     * 端口
     */
    private Integer port;

}
