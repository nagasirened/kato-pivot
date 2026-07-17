package com.kato.pro.langchain.eval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 单条 Golden Case — RAG 评测样本。
 *
 * 字段语义：
 *   - id          全局唯一 case id（用于报告 & 日志）
 *   - query       真实用户 query（口语化、自然表述）
 *   - expectedChunkIds  期望命中的 chunkId 列表（>=1 条）
 *   - expectedMinScore  期望 top-1 score 下限（nullable=不限制）
 *   - category    业务分类（退货/退款/物流/订单/FAQ）
 *   - notes       维护备注（回归覆盖说明 / 已知限制）
 *
 * 用 @Data + @NoArgsConstructor + @AllArgsConstructor 以兼容 Jackson 反序列化。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoldenCase {

    private String id;
    private String query;
    private List<Long> expectedChunkIds;
    private Double expectedMinScore;
    private String category;
    private String notes;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String query;
        private List<Long> expectedChunkIds;
        private Double expectedMinScore;
        private String category;
        private String notes;

        public Builder id(String v) { this.id = v; return this; }
        public Builder query(String v) { this.query = v; return this; }
        public Builder expectedChunkIds(List<Long> v) { this.expectedChunkIds = v; return this; }
        public Builder expectedMinScore(Double v) { this.expectedMinScore = v; return this; }
        public Builder category(String v) { this.category = v; return this; }
        public Builder notes(String v) { this.notes = v; return this; }

        public GoldenCase build() {
            return new GoldenCase(id, query, expectedChunkIds, expectedMinScore, category, notes);
        }
    }
}
