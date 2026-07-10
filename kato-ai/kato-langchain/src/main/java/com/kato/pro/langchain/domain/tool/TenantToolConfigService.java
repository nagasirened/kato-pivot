package com.kato.pro.langchain.domain.tool;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.infrastructure.persistence.TenantToolConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 租户级工具启用配置服务（R2）。
 *
 *   - 默认新租户对所有已注册工具默认启用（list 阶段由 caller 拼接"系统已注册 + 租户覆盖"）
 *   - 关闭（enabled=false）后 dispatcher 拒绝调用
 *   - DB 无记录 → 默认 true（开启）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantToolConfigService {

    private final TenantToolConfigMapper mapper;
    private final ToolRegistry registry;

    public boolean isEnabled(Long tenantId, String toolName) {
        Optional<TenantToolConfig> cfg = mapper.findByToolName(tenantId, toolName);
        return cfg.map(c -> Boolean.TRUE.equals(c.getEnabled())).orElse(true);
    }

    @Transactional
    public void setEnabled(Long tenantId, String toolName, boolean enabled) {
        if (!registry.names().contains(toolName)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "tool 不存在: " + toolName);
        }
        Optional<TenantToolConfig> existing = mapper.findByToolName(tenantId, toolName);
        if (existing.isPresent()) {
            TenantToolConfig c = existing.get();
            c.setEnabled(enabled);
            mapper.updateById(c);
        } else {
            TenantToolConfig c = new TenantToolConfig();
            c.setTenantId(tenantId);
            c.setToolName(toolName);
            c.setEnabled(enabled);
            mapper.insert(c);
        }
        log.info("TenantToolConfig updated: tenant={}, tool={}, enabled={}", tenantId, toolName, enabled);
    }

    public List<TenantToolConfig> listByTenant(Long tenantId) {
        return mapper.listByTenant(tenantId);
    }
}
