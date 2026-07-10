package com.kato.pro.langchain.api.prompt;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kato.pro.langchain.api.chat.dto.PageResult;
import com.kato.pro.langchain.api.prompt.dto.PromptTemplateVO;
import com.kato.pro.langchain.api.prompt.dto.RenderRequest;
import com.kato.pro.langchain.api.prompt.dto.RenderResponse;
import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.result.Result;
import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.domain.prompt.PromptRenderer;
import com.kato.pro.langchain.domain.prompt.PromptTemplate;
import com.kato.pro.langchain.domain.prompt.PromptTemplateRegistry;
import com.kato.pro.langchain.domain.prompt.PromptTemplateService;
import com.kato.pro.langchain.domain.prompt.PromptTemplateVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Prompt 模板 REST API：
 *   GET    /api/v1/prompts?page=&size=      列表
 *   POST   /api/v1/prompts                  新增模板
 *   PUT    /api/v1/prompts/{id}             更新
 *   DELETE /api/v1/prompts/{id}             删除
 *   POST   /api/v1/prompts/render           渲染（key + vars → content）
 */
@RestController
@RequestMapping("/api/v1/prompts")
@RequiredArgsConstructor
public class PromptTemplateController {

    private final PromptTemplateService service;
    private final PromptTemplateRegistry registry;
    private final PromptRenderer renderer;

    @GetMapping
    public Result<PageResult<PromptTemplateVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        IPage<PromptTemplate> p = service.list(page, size);
        return Result.ok(PageResult.of(p, PromptTemplateVO::from));
    }

    @PostMapping
    public Result<PromptTemplateVO> create(@RequestBody PromptTemplateVO vo) {
        if (vo == null) throw new BusinessException(ErrorCode.PARAM_INVALID, "body 不能为空");
        PromptTemplate t = service.create(vo.getTenantId(), vo.getTemplateKey(),
                vo.getContent(), vo.getDescription());
        return Result.ok(PromptTemplateVO.from(t));
    }

    @PutMapping("/{id}")
    public Result<PromptTemplateVO> update(@PathVariable Long id, @RequestBody PromptTemplateVO vo) {
        if (vo == null) throw new BusinessException(ErrorCode.PARAM_INVALID, "body 不能为空");
        PromptTemplate t = service.update(id, vo.getContent(), vo.getDescription());
        return Result.ok(PromptTemplateVO.from(t));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(service.delete(id));
    }

    @PostMapping("/render")
    public Result<RenderResponse> render(@RequestBody RenderRequest req) {
        if (req == null || req.getKey() == null || req.getKey().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "key 不能为空");
        }
        Long tenantId = TenantContext.currentOrNull() != null ? TenantContext.currentTenantId() : null;
        return registry.get(req.getKey(), tenantId)
                .map(t -> {
                    String content = renderer.substitute(t.content(),
                            req.getVars() == null ? Map.of() : req.getVars());
                    return Result.ok(RenderResponse.builder()
                            .key(req.getKey())
                            .content(content)
                            .source(t.source())
                            .version(t.version())
                            .build());
                })
                .orElseGet(() -> Result.ok(RenderResponse.builder()
                        .key(req.getKey())
                        .content("")
                        .source("MISS")
                        .build()));
    }
}
