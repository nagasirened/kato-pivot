package com.kato.pro.client;

import com.kato.pro.annotation.KatoResource;
import com.kato.pro.config.KatoClientProperties;
import com.kato.pro.discovery.DiscoveryService;
import jodd.util.StringUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Proxy;
import java.util.Objects;

/**
 * 注入ClientBean
 */
public class KatoClientProcessor implements BeanFactoryPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private final DiscoveryService discoveryService;

    private final KatoClientProperties clientProperties;

    public KatoClientProcessor(DiscoveryService discoveryService, KatoClientProperties clientProperties) {
        this.discoveryService = discoveryService;
        this.clientProperties = clientProperties;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        for (String definitionName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(definitionName);
            String beanClassName = beanDefinition.getBeanClassName();
            if (StringUtil.isNotBlank(beanClassName)) {
                Class<?> clazz = ClassUtils.resolveClassName(beanClassName, this.getClass().getClassLoader());
                ReflectionUtils.doWithFields(clazz, field -> {
                    KatoResource katoResource = field.getAnnotation(KatoResource.class);
                    if (Objects.nonNull(katoResource)) {
                        Object bean = applicationContext.getBean(clazz);
                        field.setAccessible(true);
                        field.set(bean, Proxy.newProxyInstance(
                                          field.getType().getClassLoader(),
                                          new Class[]{field.getType()},
                                          new ClientStubInvocationHandler(discoveryService, field.getType(), katoResource.version(), clientProperties)
                                        )
                        );
                    }
                });
            }
        }
    }

}

