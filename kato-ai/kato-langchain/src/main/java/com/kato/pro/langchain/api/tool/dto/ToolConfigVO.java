package com.kato.pro.langchain.api.tool.dto;

import com.kato.pro.langchain.domain.tool.TenantToolConfig;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ToolConfigVO {
    Long id;
    String toolName;
    Boolean enabled;

    public static ToolConfigVO from(TenantToolConfig c) {
        if (c == null) return null;
        return ToolConfigVO.builder()
                .id(c.getId())
                .toolName(c.getToolName())
                .enabled(c.getEnabled())
                .build();
    }
}
