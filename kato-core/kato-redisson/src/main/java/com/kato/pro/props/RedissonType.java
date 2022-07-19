package com.kato.pro.props;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.redisson.config.Config;

import java.util.Arrays;

public enum RedissonType {

    SINGLE{
        @Override
        public Config pack(RedissonProperties properties) {
            Config config = new Config();
            config.useSingleServer().setAddress(getAddress(properties)[0] );
            return config;
        }
    },
    CLUSTER {
        @Override
        public Config pack(RedissonProperties properties) {
            Config config = new Config();
            config.useClusterServers()
                    .setScanInterval(2000)
                    .addNodeAddress(getAddress(properties));
            return config;
        }
    },
    CLOUD {
        @Override
        public Config pack(RedissonProperties properties) {
            Config config = new Config();
            config.useReplicatedServers()
                    .setScanInterval(2000)
                    .addNodeAddress(getAddress(properties));
            return config;
        }
    },
    SENTINEL {
        @Override
        public Config pack(RedissonProperties properties) {
            Config config = new Config();
            config.useSentinelServers()
                    .setScanInterval(2000)
                    .addSentinelAddress(getAddress(properties));
            return config;
        }
    },
    MASTER_SLAVE {
        @Override
        public Config pack(RedissonProperties properties) {
            Config config = new Config();
            String[] addresses = getAddress(properties);
            String masterAddress = properties.getMasterAddress();
            if (StrUtil.isBlank(masterAddress)) {
                masterAddress = addresses[0];
                addresses = Arrays.stream(addresses).skip(1).toArray(String[]::new);
            }
            config.useMasterSlaveServers()
                    .setMasterAddress(masterAddress)
                    .addSlaveAddress(addresses);
            return config;
        }
    }
    ;

    public abstract Config pack(RedissonProperties properties);

    public String[] getAddress(RedissonProperties properties) {
        String addresses = properties.getAddress();
        if (StrUtil.isEmpty( addresses )) {
            throw new RuntimeException( "地址不存在" );
        }
        String[] split = addresses.split(",");
        String prefix = properties.getUseSSL() ? RedissonConstant.SSL_PREFIX : RedissonConstant.NOR_PREFIX;
        return Arrays.stream(split).map(address -> prefix + address).toArray(String[]::new);
    }

    public Integer getDb(Integer db) {
        return ObjectUtil.isNull( db ) ? 0 : db;
    }
}
