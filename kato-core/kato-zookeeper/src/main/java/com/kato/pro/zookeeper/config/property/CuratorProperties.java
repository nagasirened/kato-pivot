package com.kato.pro.zookeeper.config.property;

import lombok.Data;

@Data
public class CuratorProperties {

    private String address;
    private Integer sessionTimeout;
    private Integer connectionTimeout;
    private Integer maxRetries;
}
