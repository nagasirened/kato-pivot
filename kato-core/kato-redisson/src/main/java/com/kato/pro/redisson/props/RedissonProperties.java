package com.kato.pro.redisson.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(RedissonProperties.PREFIX)
public class RedissonProperties {

    public static final String PREFIX = "kato.redisson";

    /** 连接地址 host:port */
    private String address;

    /** 仅主从模式需要填写master的地址 */
    private String masterAddress;

    /** 密码 */
    private String password;

    /** 数据库 */
    private Integer db;

    /** 连接类型：集群，云托管，单机，哨兵，主从模式 */
    private RedissonType type = RedissonType.SINGLE;

    private Boolean useSSL = Boolean.FALSE;

}
