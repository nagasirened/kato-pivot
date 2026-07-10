package com.kato.pro.langchain.api.admin.sync.dto;

import lombok.Builder;
import lombok.Value;

/**
 * 手动触发同步响应（POST /api/v1/admin/sync/{adapterName}/trigger）。
 */
@Value
@Builder
public class TriggerResponse {
    String adapterName;
    Long runId;
    String status;
    Integer successCount;
    Integer failCount;
    String errorMsg;
}
