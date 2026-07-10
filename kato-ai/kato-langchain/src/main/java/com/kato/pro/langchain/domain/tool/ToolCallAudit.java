package com.kato.pro.langchain.domain.tool;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kato.pro.langchain.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工具调用审计（A3 + 写类审核）。
 *
 *   - argsJson   : LLM 结构化 tool_call 的 args JSON 原文
 *   - requesterId: 发起人 userId（从 ToolContext.userId）
 *   - status     : 见 AuditStatus
 *   - approverId : 审核通过/拒绝人（admin 操作）
 *   - resultJson : 实际执行结果（仅 EXECUTED 状态有值）
 *   - traceId    : 全链路串联
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tool_call_audit")
public class ToolCallAudit extends BaseEntity {

    private String toolName;
    private String toolType;
    private String argsJson;
    private Long requesterId;
    private String channel;
    private String status;
    private Long approverId;
    private LocalDateTime approveTime;
    private String resultJson;
    private String errorMsg;
    private String traceId;
}
