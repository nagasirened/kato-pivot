package com.kato.pro.langchain.api.tool.dto;

import com.kato.pro.langchain.domain.tool.ToolCallAudit;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;


@Value
@Builder
@Schema(description = "工具审计列表视图")
public class AuditListVO {
    Long id;
    String toolName;
    String toolType;
    String status;
    Long requesterId;
    Long approverId;
    LocalDateTime approveTime;
    String errorMsg;
    String traceId;
    LocalDateTime createTime;

    public static AuditListVO from(ToolCallAudit a) {
        if (a == null) return null;
        return AuditListVO.builder()
                .id(a.getId())
                .toolName(a.getToolName())
                .toolType(a.getToolType())
                .status(a.getStatus())
                .requesterId(a.getRequesterId())
                .approverId(a.getApproverId())
                .approveTime(a.getApproveTime())
                .errorMsg(a.getErrorMsg())
                .traceId(a.getTraceId())
                .createTime(a.getCreateTime())
                .build();
    }
}
