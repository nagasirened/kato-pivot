package com.kato.pro.client;

import cn.hutool.core.util.StrUtil;
import com.kato.pro.annotation.KatoResource;
import jodd.util.StringUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.util.Objects;

/**
 * 注入ClientBean
 */
public class KatoClientProcessor implements BeanFactoryPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        for (String definitionName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(definitionName);
            String beanClassName = beanDefinition.getBeanClassName();
            if (StrUtil.isNotBlank(beanClassName)) {
                Class<?> clazz = ClassUtils.resolveClassName(beanClassName, this.getClass().getClassLoader());
                ReflectionUtils.doWithFields(clazz, field -> {
                    KatoResource katoResource = field.getAnnotation(KatoResource.class);
                    if (Objects.nonNull(katoResource)) {
                        Object bean = applicationContext.getBean(clazz);
                        field.setAccessible(true);


                    }
                });
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
