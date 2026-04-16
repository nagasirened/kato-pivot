package com.kato.pro.zookeeper.config.property;


import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@FieldNameConstants
@ConfigurationProperties(ZookeeperProperties.PREFIX)
public class ZookeeperProperties {

    public static final String PREFIX = "zookeeper.kato";

    private BaseZkProperties basic = new BaseZkProperties();

    private CuratorProperties curator = new CuratorProperties();

}
