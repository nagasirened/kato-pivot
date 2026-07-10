package com.kato.pro.langchain.domain.audit;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kato.pro.langchain.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作审计实体（spec §6 M11）。
 *
 *   - action   : CREATE/UPDATE/DELETE/TRIGGER/APPROVE/REJECT
 *   - resource : SYNC/KNOWLEDGE/TOOL/PROMPT/USER
 *   - resourceId : 业务ID（可空）
 *   - argsJson / resultJson : 请求/响应 JSON 摘要（用于回放）
 *   - traceId / durationMs : 全链路串联
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("op_audit")
public class OpAudit extends BaseEntity {
    private Long userId;
    private String username;
    private String action;
    private String resource;
    private String resourceId;
    private String requestUri;
    private String httpMethod;
    private Integer httpStatus;
    private String argsJson;
    private String resultJson;
    private String traceId;
    private Integer durationMs;
}
