package com.kato.pro.langchain.domain.tool;

import com.kato.pro.langchain.annotation.ToolDef;
import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tool 注册表。
 *
 *   - 启动时通过 ApplicationContextAware 扫描所有 @Tool @Component Bean
 *   - 内存 Map<name, Tool> 提供 O(1) 查询
 *   - checkEnabled(tenantId, toolName) 由 TenantToolConfigService 提供（task 7）
 *
 * 规则：name 全局唯一；@Tool.name 与 Tool.name() 必须一致（不一致启动报错）。
 */
@Slf4j
@Component
public class ToolRegistry implements ApplicationContextAware {

    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        Map<String, Object> beans = ctx.getBeansWithAnnotation(ToolDef.class);
        for (Object bean : beans.values()) {
            if (!(bean instanceof Tool t)) {
                throw new IllegalStateException("@Tool 标注的 Bean 必须实现 Tool SPI: " + bean.getClass());
            }
            ToolDef ann = bean.getClass().getAnnotation(ToolDef.class);
            if (!ann.name().equals(t.name())) {
                throw new IllegalStateException("@Tool.name 与 Tool.name() 不一致: " + ann.name());
            }
            Tool prev = tools.put(ann.name(), t);
            if (prev != null) {
                throw new IllegalStateException("Tool name 重复: " + ann.name());
            }
            log.info("Registered tool: name={}, type={}, desc={}", ann.name(), ann.type(), ann.description());
        }
    }

    public Optional<Tool> get(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(tools.get(name));
    }

    public Collection<Tool> listAll() {
        return Collections.unmodifiableCollection(tools.values());
    }

    public Set<String> names() {
        return Collections.unmodifiableSet(tools.keySet());
    }

    /** 强制要求存在的工具；找不到抛业务异常（被 dispatcher 用于前置校验） */
    public Tool require(String name) {
        return get(name).orElseThrow(() ->
                new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "tool 不存在: " + name));
    }
}
