package com.kato.pro.langchain.domain.prompt;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.infrastructure.persistence.PromptTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Prompt 模板 CRUD。
 *
 * 写操作（D5=C）：更新后通过 {@link PromptTemplateRegistry#reload()} 立即刷新缓存。
 * v1 简单实现：写完调 reload；高并发下可改成 Caffeine + 增量失效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptTemplateService {

    private final PromptTemplateMapper mapper;
    private final PromptTemplateRegistry registry;

    @Transactional
    public PromptTemplate create(Long tenantId, String key, String content, String description) {
        if (key == null || key.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "templateKey 不能为空");
        }
        if (content == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "content 不能为空");
        }
        Optional<PromptTemplate> existing = mapper.findTenantOverride(tenantId, key);
        if (existing.isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE,
                    "模板已存在 (tenantId=" + tenantId + ", key=" + key + ")");
        }
        PromptTemplate t = new PromptTemplate();
        t.setTenantId(tenantId);
        t.setTemplateKey(key);
        t.setContent(content);
        t.setVersion(1);
        t.setDescription(description);
        mapper.insert(t);
        registry.reload();
        log.info("Prompt template created: tenantId={} key={}", tenantId, key);
        return t;
    }

    @Transactional
    public PromptTemplate update(Long id, String content, String description) {
        PromptTemplate t = mapper.selectById(id);
        if (t == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "模板不存在: id=" + id);
        if (content != null) t.setContent(content);
        if (description != null) t.setDescription(description);
        t.setVersion(t.getVersion() == null ? 1 : t.getVersion() + 1);
        mapper.updateById(t);
        registry.reload();
        return t;
    }

    @Transactional
    public boolean delete(Long id) {
        int rows = mapper.deleteById(id);
        if (rows > 0) registry.reload();
        return rows > 0;
    }

    public IPage<PromptTemplate> list(int page, int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 20;
        return mapper.selectPage(new Page<>(page, size), null);
    }
}
