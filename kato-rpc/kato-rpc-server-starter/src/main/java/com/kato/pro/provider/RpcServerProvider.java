package com.kato.pro.provider;

import com.kato.pro.annotation.KatoService;
import com.kato.pro.config.KatoRpcProperties;
import com.kato.pro.constant.ServiceInfo;
import com.kato.pro.register.RegisterService;
import com.kato.pro.server.LocalServiceCache;
import com.kato.pro.server.RpcServer;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.CommandLineRunner;

import java.io.IOException;
import java.util.Objects;

@Slf4j
public class RpcServerProvider implements BeanPostProcessor, CommandLineRunner {

    private final RegisterService registerService;

    private final RpcServer rpcServer;

    private final KatoRpcProperties properties;

    RpcServerProvider(RegisterService registerService, RpcServer rpcServer, KatoRpcProperties properties) {
        this.registerService = registerService;
        this.rpcServer = rpcServer;
        this.properties = properties;
    }

    /**
     * 如果Bean带有 @KatoService注解，代笔该Bean需要注册到服务注册中心中提供使用
     * @param bean      bean 对象
     * @param beanName  beanName Bean的名字
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        KatoService katoService = bean.getClass().getAnnotation(KatoService.class);
        if (Objects.nonNull(katoService)) {
            Class<?> interfaceType = katoService.interfaceType();
            String serviceName = interfaceType.getName();
            String version = katoService.version();
            String serviceVersion = String.join("_", serviceName, version);
            LocalServiceCache.store(serviceVersion, bean);

            ServiceInfo serviceInfo = new ServiceInfo();
            serviceInfo.setServiceVersion(serviceVersion);
            serviceInfo.setPort(properties.getPort());
            serviceInfo.setApplicationName(properties.getAppName());
            serviceInfo.setAddress(properties.getServerAddress());
            registerService.register(serviceInfo);
        }
        return bean;
    }

    /**
     * 启动Netty
     */
    @Override
    public void run(String... args) throws Exception {
        // 启动NettyServer
        new Thread(() -> {
            rpcServer.init(properties.getPort());
            log.info("netty server turn up");
        }).start();
        // 项目关闭的钩子函数, 删除所有注册服务的缓存
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                registerService.destroy(LocalServiceCache.getAllKeys());
            } catch (IOException e) {
                log.error("destroy serviceDiscovery fail", e);
            }
        }));
    }
}
