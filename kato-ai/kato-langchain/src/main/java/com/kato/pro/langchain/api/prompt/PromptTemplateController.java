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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import jakarta.validation.Valid;

/**
 * Prompt 模板 REST API：
 *   GET    /api/v1/prompts?page=&size=      列表
 *   POST   /api/v1/prompts                  新增模板
 *   PUT    /api/v1/prompts/{id}             更新
 *   DELETE /api/v1/prompts/{id}             删除
 *   POST   /api/v1/prompts/render           渲染（key + vars → content）
 */
@Tag(name = "Prompt-Template", description = "Prompt 模板 CRUD + 渲染")
@RestController
@RequestMapping("/api/v1/prompts")
@RequiredArgsConstructor
public class PromptTemplateController {

    private final PromptTemplateService service;
    private final PromptTemplateRegistry registry;
    private final PromptRenderer renderer;

    @Operation(operationId = "ListPromptTemplates", summary = "模板列表",
            description = "分页查询 prompt 模板")
    @GetMapping
    public Result<PageResult<PromptTemplateVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        IPage<PromptTemplate> p = service.list(page, size);
        return Result.ok(PageResult.of(p, PromptTemplateVO::from));
    }

    @Operation(operationId = "CreatePromptTemplate", summary = "创建模板",
            description = "新建 prompt 模板")
    @PostMapping
    public Result<PromptTemplateVO> create(@Valid @RequestBody PromptTemplateVO vo) {
        if (vo == null) throw new BusinessException(ErrorCode.PARAM_INVALID, "body 不能为空");
        PromptTemplate t = service.create(vo.getTenantId(), vo.getTemplateKey(),
                vo.getContent(), vo.getDescription());
        return Result.ok(PromptTemplateVO.from(t));
    }

    @Operation(operationId = "UpdatePromptTemplate", summary = "更新模板",
            description = "按 id 修改模板内容")
    @PutMapping("/{id}")
    public Result<PromptTemplateVO> update(@PathVariable Long id, @RequestBody PromptTemplateVO vo) {
        if (vo == null) throw new BusinessException(ErrorCode.PARAM_INVALID, "body 不能为空");
        PromptTemplate t = service.update(id, vo.getContent(), vo.getDescription());
        return Result.ok(PromptTemplateVO.from(t));
    }

    @Operation(operationId = "DeletePromptTemplate", summary = "删除模板",
            description = "按 id 删除模板")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(service.delete(id));
    }

    @Operation(operationId = "RenderPrompt", summary = "渲染模板",
            description = "用变量渲染模板，输出最终 prompt")
    @PostMapping("/render")
    public Result<RenderResponse> render(@Valid @RequestBody RenderRequest req) {
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
                            .build());
                })
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "template not found: " + req.getKey()));
    }
}
