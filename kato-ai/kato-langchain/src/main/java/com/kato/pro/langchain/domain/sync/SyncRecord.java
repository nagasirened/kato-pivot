package com.kato.pro.langchain.domain.sync;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

/**
 * 单条同步记录（adapter 拉取的业务数据）。
 *
 *   - externalId : 业务系统主键（用于幂等）
 *   - title      : 文档标题
 *   - content    : 文档正文（直接走分段/embedding）
 *   - tags       : 元数据（写入 KnowledgeDoc.metadata）
 *   - updatedAt  : 业务更新时间（增量判断）
 */
@Value
@Builder
public class SyncRecord {
    String externalId;
    String title;
    String content;
    Map<String, Object> tags;
    Instant updatedAt;
}
