package com.kato.pro.langchain.domain.knowledge;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kato.pro.langchain.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库文档。
 *
 * 字段：
 *   - sourceType: 来源（UPLOAD / SYNC_*）
 *   - sourceId: 业务方 ID（同步用；UPLOAD 时为空）
 *   - title: 文档名
 *   - contentHash: 内容 SHA-256，幂等去重
 *   - status: 索引状态
 *   - errorMessage: 失败原因（status=FAILED 时有值）
 *   - chunkCount: 分段数（READY 后填）
 *   - charCount: 总字符数（READY 后填）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_doc")
public class KnowledgeDoc extends BaseEntity {

    private SourceType sourceType;

    /** 业务方 ID（同步用） */
    private String sourceId;

    /** 文档标题 */
    private String title;

    /** 文件名（UPLOAD 用） */
    @TableField("file_name")
    private String fileName;

    /** 内容 MIME 类型 */
    @TableField("content_type")
    private String contentType;

    /** 内容 SHA-256（用于幂等去重） */
    @TableField("content_hash")
    private String contentHash;

    /** 索引状态 */
    private KnowledgeDocStatus status;

    /** 索引失败原因 */
    @TableField("error_message")
    private String errorMessage;

    /** 分段数 */
    @TableField("chunk_count")
    private Integer chunkCount;

    /** 总字符数 */
    @TableField("char_count")
    private Integer charCount;
}
