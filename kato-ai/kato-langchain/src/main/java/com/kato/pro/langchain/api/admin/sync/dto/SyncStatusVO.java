package com.kato.pro.langchain.api.admin.sync.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;


/**
 * Adapter 状态概览（GET /api/v1/admin/sync/status）。
 *
 *   - adapterName : 唯一标识
 *   - description : 中文描述
 *   - sourceType  : KnowledgeDoc.SourceType
 *   - lastRun     : 最近一次运行记录（id/startTime/successCount/failCount/status/errorMsg）
 *                   无历史时为 null
 */
@Value
@Builder
@Schema(description = "同步状态视图")
public class SyncStatusVO {
    String adapterName;
    String description;
    String sourceType;
    Map<String, Object> lastRun;
}
