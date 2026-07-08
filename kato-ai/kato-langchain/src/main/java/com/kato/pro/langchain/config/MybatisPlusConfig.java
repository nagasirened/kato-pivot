package com.kato.pro.langchain.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.kato.pro.langchain.infrastructure.persistence.TenantMybatisInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置：注册多租户拦截器、分页拦截器、字段自动填充。
 *
 * 拦截器顺序敏感（MyBatis-Plus 内部按 list 顺序执行）：
 *   1. TenantMybatisInterceptor   ← 必须最先，注入 tenant_id
 *   2. PaginationInnerInterceptor  ← 分页处理
 *
 * 沙箱说明：M1 未引入 mybatis-plus-spring-boot3-starter（其传递依赖
 * spring-boot-starter-jdbc 3.2.6 在本地 m2 缺失），所以本配置类手动注册
 * 拦截器。等环境有 spring-boot3 starter 时可启用自动装配。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantMybatisInterceptor());
        interceptor.addInnerInterceptor(paginationInnerInterceptor());
        return interceptor;
    }

    @Bean
    public PaginationInnerInterceptor paginationInnerInterceptor() {
        PaginationInnerInterceptor p = new PaginationInnerInterceptor(DbType.MYSQL);
        p.setMaxLimit(500L);  // 单页最大 500，防止深分页打爆 DB
        return p;
    }
}
