package com.kato.pro.redisson.props;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.google.common.base.Splitter;
import org.redisson.config.Config;

import java.util.Arrays;
import java.util.List;

public enum RedissonType {

    SINGLE{
        @Override
        public Config pack(RedissonProperties properties) {
            Config config = new Config();
            config.useSingleServer().setDatabase(getDb(properties)).setAddress(getAddress(properties)[0] );
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
                    .setDatabase(getDb(properties))
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
                    .setDatabase(getDb(properties))
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
            if (CharSequenceUtil.isBlank(masterAddress)) {
                masterAddress = addresses[0];
                addresses = Arrays.stream(addresses).skip(1).toArray(String[]::new);
            }
            config.useMasterSlaveServers()
                    .setDatabase(getDb(properties))
                    .setMasterAddress(masterAddress)
                    .addSlaveAddress(addresses);
            return config;
        }
    }
    ;

    public abstract Config pack(RedissonProperties properties);

    public String[] getAddress(RedissonProperties properties) {
        String addresses = properties.getAddress();
        if (CharSequenceUtil.isEmpty(addresses)) {
            throw new IllegalArgumentException( "redisson address not exist" );
        }
        List<String> split = Splitter.on(",").trimResults().splitToList(addresses);
        String prefix = properties.getUseSSL() ? RedissonConstant.SSL_PREFIX : RedissonConstant.NOR_PREFIX;
        return split.stream().map(address -> prefix + address).toArray(String[]::new);
    }

    public Integer getDb(RedissonProperties redissonProperties) {
        Integer db = redissonProperties.getDb();
        return ObjectUtil.isNull( db ) ? 0 : db;
    }
}
