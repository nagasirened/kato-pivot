package com.kato.pro.langchain.api.tool;

import com.kato.pro.langchain.api.tool.dto.ToggleRequest;
import com.kato.pro.langchain.api.tool.dto.ToolConfigVO;
import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.result.Result;
import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.domain.tool.TenantToolConfig;
import com.kato.pro.langchain.domain.tool.TenantToolConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;



/**
 * 租户级工具启用配置 REST API（admin）。
 *
 *   GET /api/v1/admin/tool/config      租户已启用/全部工具
 *   PUT /api/v1/admin/tool/config/{toolName}  启用/停用
 */
@Tag(name = "Tool-Config", description = "租户级工具配置（开启/关闭/参数覆盖）")
@RestController
@RequestMapping("/api/v1/admin/tool/config")
@RequiredArgsConstructor
public class TenantToolConfigController {

    private final TenantToolConfigService service;

    @Operation(operationId = "ListToolConfig", summary = "工具配置列表", description = "查询租户级工具配置（启用状态/参数覆盖）")
    @GetMapping
    public Result<List<ToolConfigVO>> list() {
        Long tenantId = TenantContext.requireCurrent().tenantId();
        return Result.ok(service.listByTenant(tenantId).stream()
                .map(ToolConfigVO::from).toList());
    }
    @Operation(operationId = "UpsertToolConfig", summary = "更新工具配置", description = "按 toolName upsert 配置")

    @PutMapping("/{toolName}")
    public Result<Boolean> toggle(@PathVariable String toolName,
                                  @Valid @RequestBody ToggleRequest req) {
        if (req == null || req.getEnabled() == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "enabled 不能为空");
        }
        Long tenantId = TenantContext.requireCurrent().tenantId();
        service.setEnabled(tenantId, toolName, req.getEnabled());
        return Result.ok(true);
    }
}
