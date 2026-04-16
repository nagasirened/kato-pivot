package com.kato.pro.rec.entity.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RecommendConstant {

    public static final String THREAD_POOL_RECALL = "recall";

    /** 多路召回蛇形合并后保留的最大条数 N */
    public static final String PROP_SNAKE_MERGE_TOP_N = "kato.recommend.retrieve.snake-merge-top-n";

    /** 合并时每路非空召回至少占坑条数，再进入加权蛇形；0 表示不做保底 */
    public static final String PROP_SNAKE_MIN_PER_SOURCE = "kato.recommend.retrieve.snake-min-per-source";

    public static final int DEFAULT_SNAKE_MERGE_TOP_N = 100;

    public static final int DEFAULT_SNAKE_MIN_PER_SOURCE = 1;

    /** 重排序：label → 分数倍率 JSON，如 {"100":1.5,"200":0.8} */
    public static final String PROP_RERANK_LABEL_WEIGHTS = "kato.recommend.rerank.label-weights";

    public static final String DEFAULT_RERANK_LABEL_WEIGHTS = "{}";

    /** 重排序：强插规则 JSON 数组；position 从 0 起；labels 为集合，亦兼容单字段 label，如 [{"labels":[100,101],"position":0}] */
    public static final String PROP_RERANK_STRONG_INSERTS = "kato.recommend.rerank.strong-inserts";

    public static final String DEFAULT_RERANK_STRONG_INSERTS = "[]";

    /** 重排序：滑动窗口打散 JSON；labels 为集合，亦兼容单字段 label；空串表示关闭，如 {"windowSize":5,"labels":[100],"maxInWindow":1} */
    public static final String PROP_RERANK_SLIDING_WINDOW = "kato.recommend.rerank.sliding-window";

    public static final String DEFAULT_RERANK_SLIDING_WINDOW = "";

    // ===================== 精排（Rank）相关配置 =====================

    /** 精排：TensorFlow Serving gRPC 地址，如 "localhost:8500"；未配置则跳过精排 */
    public static final String PROP_RANK_TARGET_URL = "kato.recommend.rank.target-url";

    /** 精排：模型名称，默认 recommend_rank_model */
    public static final String PROP_RANK_MODEL_NAME = "kato.recommend.rank.model-name";

    /** 精排：模型签名，默认 serving_default */
    public static final String PROP_RANK_SIGNATURE_NAME = "kato.recommend.rank.signature-name";

}
