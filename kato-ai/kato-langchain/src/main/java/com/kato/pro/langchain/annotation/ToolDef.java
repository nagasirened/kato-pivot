package com.kato.pro.langchain.annotation;

import com.kato.pro.langchain.domain.tool.ToolType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个 Spring Bean 为 Tool（被 ToolRegistry 自动扫描）。
 *
 * 规则：
 *   - 类必须同时是 @Component（或其衍生注解），确保 Spring 实例化
 *   - 必须实现 Tool SPI
 *   - name 全局唯一；type=WRITE 时强制走审核队列
 *
 * 注意：命名为 ToolDef 是为了避免与 com.kato.pro.langchain.domain.tool.Tool
 * （SPI 接口）同名导致的 javac 类型解析歧义。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ToolDef {

    /** 工具名（英文，snake_case，全局唯一） */
    String name();

    /** 工具类型：READ（读透传）/ WRITE（写审核） */
    ToolType type() default ToolType.READ;

    /** 人类可读描述（admin/调试） */
    String description() default "";

    /** JSON Schema 字符串（参数 schema，v1 简单版，先不校验） */
    String schema() default "{}";
}
